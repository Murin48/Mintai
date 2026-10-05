package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.analysis.OcpbRuleEngine
import com.example.data.analysis.PdfTextExtractor
import com.example.data.local.LeaseDatabase
import com.example.data.model.ClauseFinding
import com.example.data.model.DormitoryOverchargeCalculation
import com.example.data.model.LeaseInspectionEntity
import com.example.data.model.RiskLevel
import com.example.data.repository.LeaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LeaseAppTab {
    CHECK,
    CALCULATOR,
    RULES,
    HISTORY
}

enum class InputMode {
    PDF_FILE,
    PASTE_TEXT,
    SAMPLE_PRESETS
}

class LeaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LeaseRepository

    init {
        val db = LeaseDatabase.getInstance(application)
        repository = LeaseRepository(db.leaseDao())
    }

    val savedInspections: StateFlow<List<LeaseInspectionEntity>> = repository.allInspections
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentTab = MutableStateFlow(LeaseAppTab.CHECK)
    val currentTab: StateFlow<LeaseAppTab> = _currentTab.asStateFlow()

    private val _inputMode = MutableStateFlow(InputMode.PDF_FILE)
    val inputMode: StateFlow<InputMode> = _inputMode.asStateFlow()

    private val _contractText = MutableStateFlow("")
    val contractText: StateFlow<String> = _contractText.asStateFlow()

    private val _dormitoryName = MutableStateFlow("หอพักของฉัน")
    val dormitoryName: StateFlow<String> = _dormitoryName.asStateFlow()

    private val _selectedPdfFileName = MutableStateFlow<String?>(null)
    val selectedPdfFileName: StateFlow<String?> = _selectedPdfFileName.asStateFlow()

    private val _isProcessingPdf = MutableStateFlow(false)
    val isProcessingPdf: StateFlow<Boolean> = _isProcessingPdf.asStateFlow()

    private val _pdfStatusMessage = MutableStateFlow<String?>(null)
    val pdfStatusMessage: StateFlow<String?> = _pdfStatusMessage.asStateFlow()

    private val _inspectionResult = MutableStateFlow<OcpbRuleEngine.InspectionSummary?>(null)
    val inspectionResult: StateFlow<OcpbRuleEngine.InspectionSummary?> = _inspectionResult.asStateFlow()

    private val _activeFilterLevel = MutableStateFlow<RiskLevel?>(null)
    val activeFilterLevel: StateFlow<RiskLevel?> = _activeFilterLevel.asStateFlow()

    private val _isSavedCurrent = MutableStateFlow(false)
    val isSavedCurrent: StateFlow<Boolean> = _isSavedCurrent.asStateFlow()

    private val _selectedHistoryInspection = MutableStateFlow<LeaseInspectionEntity?>(null)
    val selectedHistoryInspection: StateFlow<LeaseInspectionEntity?> = _selectedHistoryInspection.asStateFlow()

    // Calculator state
    private val _calcRent = MutableStateFlow("4500")
    val calcRent: StateFlow<String> = _calcRent.asStateFlow()

    private val _calcDeposit = MutableStateFlow("9000")
    val calcDeposit: StateFlow<String> = _calcDeposit.asStateFlow()

    private val _calcElecRate = MutableStateFlow("8.0")
    val calcElecRate: StateFlow<String> = _calcElecRate.asStateFlow()

    private val _calcElecUnits = MutableStateFlow("150")
    val calcElecUnits: StateFlow<String> = _calcElecUnits.asStateFlow()

    private val _calcWaterRate = MutableStateFlow("20.0")
    val calcWaterRate: StateFlow<String> = _calcWaterRate.asStateFlow()

    private val _calcWaterUnits = MutableStateFlow("6")
    val calcWaterUnits: StateFlow<String> = _calcWaterUnits.asStateFlow()

    fun setTab(tab: LeaseAppTab) {
        _currentTab.value = tab
    }

    fun setInputMode(mode: InputMode) {
        _inputMode.value = mode
    }

    fun updateContractText(text: String) {
        _contractText.value = text
        _isSavedCurrent.value = false
    }

    fun updateDormitoryName(name: String) {
        _dormitoryName.value = name
    }

    fun setFilterLevel(level: RiskLevel?) {
        _activeFilterLevel.value = level
    }

    fun loadSampleContract(sampleType: Int) {
        when (sampleType) {
            1 -> {
                _dormitoryName.value = "หอพักบุญอนันต์ (เสี่ยงสูง)"
                _contractText.value = OcpbRuleEngine.SAMPLE_CONTRACT_RED
            }
            2 -> {
                _dormitoryName.value = "หอพักสุขสราญ (มีข้อควรระวัง)"
                _contractText.value = OcpbRuleEngine.SAMPLE_CONTRACT_YELLOW
            }
            3 -> {
                _dormitoryName.value = "หอพักกรีนลิฟวิ่ง (มาตรฐาน สคบ.)"
                _contractText.value = OcpbRuleEngine.SAMPLE_CONTRACT_GREEN
            }
        }
        _selectedPdfFileName.value = null
        _pdfStatusMessage.value = "โหลดตัวอย่างสัญญาเรียบร้อยแล้ว"
        runAnalysis()
    }

    fun processPdfUri(uri: Uri) {
        viewModelScope.launch {
            _isProcessingPdf.value = true
            _selectedPdfFileName.value = uri.lastPathSegment ?: "contract.pdf"
            _pdfStatusMessage.value = "กำลังอ่านและแปลงข้อความจากไฟล์ PDF..."

            val result = withContext(Dispatchers.IO) {
                PdfTextExtractor.extractTextFromUri(getApplication(), uri)
            }

            _isProcessingPdf.value = false
            if (result.isSuccessful && result.text.isNotBlank()) {
                _contractText.value = result.text
                _pdfStatusMessage.value = "อ่านไฟล์ PDF สำเร็จ (${result.charCount} ตัวอักษร) พร้อมวิเคราะห์"
                runAnalysis()
            } else {
                _pdfStatusMessage.value = result.message
            }
        }
    }

    fun runAnalysis() {
        val text = _contractText.value.trim()
        if (text.isBlank()) {
            _pdfStatusMessage.value = "กรุณาใส่ข้อความสัญญาเช่าหรือนำเข้าไฟล์ PDF ก่อนกดวิเคราะห์"
            return
        }

        viewModelScope.launch {
            val summary = withContext(Dispatchers.Default) {
                OcpbRuleEngine.evaluateContract(text)
            }
            _inspectionResult.value = summary
            _isSavedCurrent.value = false
        }
    }

    fun saveCurrentInspection() {
        val summary = _inspectionResult.value ?: return
        viewModelScope.launch {
            val jsonFindings = OcpbRuleEngine.serializeFindings(summary.findings)
            val excerpt = _contractText.value.take(400)
            val entity = LeaseInspectionEntity(
                dormitoryName = _dormitoryName.value.ifBlank { "หอพักไม่ระบุชื่อ" },
                safetyScore = summary.safetyScore,
                verdict = summary.verdict,
                redCount = summary.redCount,
                yellowCount = summary.yellowCount,
                greenCount = summary.greenCount,
                contractExcerpt = excerpt,
                findingsJson = jsonFindings
            )
            repository.saveInspection(entity)
            _isSavedCurrent.value = true
        }
    }

    fun viewHistoryDetail(item: LeaseInspectionEntity) {
        _selectedHistoryInspection.value = item
    }

    fun clearSelectedHistoryDetail() {
        _selectedHistoryInspection.value = null
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteInspection(id)
            if (_selectedHistoryInspection.value?.id == id) {
                _selectedHistoryInspection.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _selectedHistoryInspection.value = null
        }
    }

    // Calculator updates
    fun updateCalcRent(v: String) { _calcRent.value = v }
    fun updateCalcDeposit(v: String) { _calcDeposit.value = v }
    fun updateCalcElecRate(v: String) { _calcElecRate.value = v }
    fun updateCalcElecUnits(v: String) { _calcElecUnits.value = v }
    fun updateCalcWaterRate(v: String) { _calcWaterRate.value = v }
    fun updateCalcWaterUnits(v: String) { _calcWaterUnits.value = v }

    fun getCalculation(): DormitoryOverchargeCalculation {
        val rent = _calcRent.value.toDoubleOrNull() ?: 0.0
        val deposit = _calcDeposit.value.toDoubleOrNull() ?: 0.0
        val elecRate = _calcElecRate.value.toDoubleOrNull() ?: 0.0
        val elecUnits = _calcElecUnits.value.toDoubleOrNull() ?: 0.0
        val waterRate = _calcWaterRate.value.toDoubleOrNull() ?: 0.0
        val waterUnits = _calcWaterUnits.value.toDoubleOrNull() ?: 0.0

        return DormitoryOverchargeCalculation(
            monthlyRent = rent,
            securityDeposit = deposit,
            electricityRate = elecRate,
            waterRate = waterRate,
            monthlyUnitsElec = elecUnits,
            monthlyUnitsWater = waterUnits
        )
    }
}
