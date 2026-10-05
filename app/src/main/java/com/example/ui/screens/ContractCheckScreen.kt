package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClauseFinding
import com.example.data.model.RiskLevel
import com.example.ui.InputMode
import com.example.ui.LeaseViewModel
import com.example.ui.theme.*

@Composable
fun ContractCheckScreen(
    viewModel: LeaseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inputMode by viewModel.inputMode.collectAsState()
    val contractText by viewModel.contractText.collectAsState()
    val dormitoryName by viewModel.dormitoryName.collectAsState()
    val selectedPdfFileName by viewModel.selectedPdfFileName.collectAsState()
    val isProcessingPdf by viewModel.isProcessingPdf.collectAsState()
    val pdfStatusMessage by viewModel.pdfStatusMessage.collectAsState()
    val inspectionResult by viewModel.inspectionResult.collectAsState()
    val activeFilterLevel by viewModel.activeFilterLevel.collectAsState()
    val isSavedCurrent by viewModel.isSavedCurrent.collectAsState()

    // Document Picker launcher for PDF files
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.processPdfUri(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("contract_check_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            HeroHeaderCard()
        }

        // Dormitory Name & Input Mode Selector
        item {
            InputSelectorCard(
                inputMode = inputMode,
                dormitoryName = dormitoryName,
                onNameChange = { viewModel.updateDormitoryName(it) },
                onModeSelected = { viewModel.setInputMode(it) }
            )
        }

        // Input Content based on mode
        item {
            when (inputMode) {
                InputMode.PDF_FILE -> {
                    PdfUploadSection(
                        selectedFileName = selectedPdfFileName,
                        isProcessing = isProcessingPdf,
                        statusMessage = pdfStatusMessage,
                        onPickPdf = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        hasText = contractText.isNotBlank(),
                        extractedTextPreview = contractText
                    )
                }
                InputMode.PASTE_TEXT -> {
                    PasteTextSection(
                        text = contractText,
                        onTextChange = { viewModel.updateContractText(it) }
                    )
                }
                InputMode.SAMPLE_PRESETS -> {
                    SamplePresetsSection(
                        onSelectSample = { viewModel.loadSampleContract(it) }
                    )
                }
            }
        }

        // Analyze Action Button
        item {
            Button(
                onClick = { viewModel.runAnalysis() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("analyze_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !isProcessingPdf && contractText.isNotBlank()
            ) {
                Icon(Icons.Filled.SecurityUpdateGood, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ตรวจสอบสัญญาตามกฎหมาย สคบ. 2562",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Inspection Results Section
        if (inspectionResult != null) {
            val result = inspectionResult!!

            item {
                ResultScoreCard(
                    score = result.safetyScore,
                    verdict = result.verdict,
                    redCount = result.redCount,
                    yellowCount = result.yellowCount,
                    greenCount = result.greenCount,
                    isSaved = isSavedCurrent,
                    onSave = {
                        viewModel.saveCurrentInspection()
                        Toast.makeText(context, "บันทึกผลการตรวจสอบลงฐานข้อมูลเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        shareInspectionReport(context, dormitoryName, result)
                    }
                )
            }

            // Filter Tabs for 3 Colors
            item {
                FilterRiskRow(
                    selectedLevel = activeFilterLevel,
                    redCount = result.redCount,
                    yellowCount = result.yellowCount,
                    greenCount = result.greenCount,
                    onSelect = { viewModel.setFilterLevel(it) }
                )
            }

            // List of findings
            val filteredFindings = if (activeFilterLevel == null) {
                result.findings
            } else {
                result.findings.filter { it.level == activeFilterLevel }
            }

            if (filteredFindings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ไม่มีข้อสัญญาในหมวดนี้",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredFindings, key = { it.id }) { finding ->
                    ClauseFindingCard(
                        finding = finding,
                        onCopyAdvice = {
                            copyToClipboard(context, "ข้อความขอแก้ไขสัญญา", finding.suggestedAmendmentText)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HeroHeaderCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "LeaseCheck สคบ. 2562",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "ระบบตรวจสัญญาเช่าหอพัก 3 ระดับสี แดง เหลือง เขียว",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            // 3-Level Quick Explanation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LevelIndicatorPill("🔴 แดง", "ผิดกฎหมาย/โมฆะ", RiskRedContainer, RiskRedText)
                LevelIndicatorPill("🟡 เหลือง", "ข้อควรระวัง/เสียเปรียบ", RiskYellowContainer, RiskYellowText)
                LevelIndicatorPill("🟢 เขียว", "เป็นธรรม/ปลอดภัย", RiskGreenContainer, RiskGreenText)
            }
        }
    }
}

@Composable
fun LevelIndicatorPill(label: String, desc: String, bg: Color, fg: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = Modifier.padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = fg)
            Text(text = desc, fontSize = 9.sp, color = fg.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun InputSelectorCard(
    inputMode: InputMode,
    dormitoryName: String,
    onNameChange: (String) -> Unit,
    onModeSelected: (InputMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = dormitoryName,
                onValueChange = onNameChange,
                label = { Text("ชื่อหอพัก / อพาร์ตเมนต์") },
                placeholder = { Text("เช่น หอพักสุขใจ คอนโดธารา") },
                leadingIcon = { Icon(Icons.Filled.Apartment, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dormitory_name_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "เลือกวิธีนำเข้าสัญญาเช่า:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = inputMode == InputMode.PDF_FILE,
                    onClick = { onModeSelected(InputMode.PDF_FILE) },
                    label = { Text("📄 ไฟล์ PDF") },
                    leadingIcon = {
                        if (inputMode == InputMode.PDF_FILE) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("mode_pdf_chip")
                )
                FilterChip(
                    selected = inputMode == InputMode.PASTE_TEXT,
                    onClick = { onModeSelected(InputMode.PASTE_TEXT) },
                    label = { Text("✍️ พิมพ์/วาง") },
                    leadingIcon = {
                        if (inputMode == InputMode.PASTE_TEXT) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("mode_text_chip")
                )
                FilterChip(
                    selected = inputMode == InputMode.SAMPLE_PRESETS,
                    onClick = { onModeSelected(InputMode.SAMPLE_PRESETS) },
                    label = { Text("⚡ ตัวอย่าง") },
                    leadingIcon = {
                        if (inputMode == InputMode.SAMPLE_PRESETS) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("mode_sample_chip")
                )
            }
        }
    }
}

@Composable
fun PdfUploadSection(
    selectedFileName: String?,
    isProcessing: Boolean,
    statusMessage: String?,
    onPickPdf: () -> Unit,
    hasText: Boolean,
    extractedTextPreview: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "อัปโหลดสัญญาเช่าในรูปแบบ PDF",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ระบบจะสกัดข้อความภาษาไทยและตรวจจับข้อสัญญาไม่เป็นธรรมอัตโนมัติ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPickPdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_pdf_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (selectedFileName != null) "เปลี่ยนไฟล์ PDF ($selectedFileName)" else "เลือกไฟล์ PDF จากเครื่อง")
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(14.dp))
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "กำลังประมวลผลข้อความจาก PDF...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasText) RiskGreenText else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            if (hasText) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "ตัวอย่างข้อความที่สกัดได้ (${extractedTextPreview.length} ตัวอักษร):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = extractedTextPreview.take(240) + if (extractedTextPreview.length > 240) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PasteTextSection(
    text: String,
    onTextChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "วางข้อความสัญญาเช่าห้องพัก:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (text.isNotBlank()) {
                    TextButton(onClick = { onTextChange("") }) {
                        Text("ล้างข้อความ", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text("วางข้อความสัญญาเช่าที่นี่ เช่น ข้อ 1. ค่าเช่าเดือนละ 4,500 บาท ค่าไฟหน่วยละ 8 บาท ค่าน้ำหน่วยละ 20 บาท เงินมัดจำ 2 เดือน...")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("contract_text_area"),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "จำนวนตัวอักษร: ${text.length} ตัวอักษร",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SamplePresetsSection(
    onSelectSample: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ทดลองตรวจสอบด้วยสัญญาเช่าจำลอง 3 รูปแบบ:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Sample 1: Red
            PresetItemButton(
                title = "1. หอพักบุญอนันต์ (เสี่ยงสูง - ระดับสีแดง 🔴)",
                subtitle = "ค่าไฟ 8 บ., น้ำ 20 บ., ประกัน 2 เดือน, ล็อคห้องยึดของ, ริบเงินมัดจำ, ค่าต่อสัญญา",
                borderColor = RiskRedBorder,
                badgeColor = RiskRedContainer,
                badgeTextColor = RiskRedText,
                onClick = { onSelectSample(1) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sample 2: Yellow
            PresetItemButton(
                title = "2. หอพักสุขสราญ (มีข้อควรระวัง - ระดับสีเหลือง 🟡)",
                subtitle = "แจ้งล่วงหน้า 60 วัน, ผลักภาระค่าซ่อมทุกกรณี, คืนเงินประกัน 45 วัน, ค่าปรับล่าช้าสูง",
                borderColor = RiskYellowBorder,
                badgeColor = RiskYellowContainer,
                badgeTextColor = RiskYellowText,
                onClick = { onSelectSample(2) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sample 3: Green
            PresetItemButton(
                title = "3. หอพักกรีนลิฟวิ่ง (มาตรฐาน สคบ. - ระดับสีเขียว 🟢)",
                subtitle = "ค่าน้ำไฟตามบิลจริงของการไฟฟ้า/ประปา, ประกัน 1 เดือน, คืนเงินใน 7 วัน, สิทธิเลิกสัญญา 30 วัน",
                borderColor = RiskGreenBorder,
                badgeColor = RiskGreenContainer,
                badgeTextColor = RiskGreenText,
                onClick = { onSelectSample(3) }
            )
        }
    }
}

@Composable
fun PresetItemButton(
    title: String,
    subtitle: String,
    borderColor: Color,
    badgeColor: Color,
    badgeTextColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = badgeColor.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = badgeTextColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ResultScoreCard(
    score: Int,
    verdict: String,
    redCount: Int,
    yellowCount: Int,
    greenCount: Int,
    isSaved: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val scoreColor = when {
        score >= 80 -> RiskGreen
        score >= 50 -> RiskYellow
        else -> RiskRed
    }

    val scoreBg = when {
        score >= 80 -> RiskGreenContainer
        score >= 50 -> RiskYellowContainer
        else -> RiskRedContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("result_score_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ผลการวิเคราะห์สัญญาเช่า",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Score Badge
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = scoreBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "คะแนนความปลอดภัย: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$score/100",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Score Progress Bar
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = scoreColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Verdict Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = scoreBg.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (redCount > 0) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = scoreColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = verdict,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Badges Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountBadgeItem("🔴 ผิด สคบ.", "$redCount ข้อ", RiskRedContainer, RiskRedText, Modifier.weight(1f))
                CountBadgeItem("🟡 ควรระวัง", "$yellowCount ข้อ", RiskYellowContainer, RiskYellowText, Modifier.weight(1f))
                CountBadgeItem("🟢 ปลอดภัย", "$greenCount ข้อ", RiskGreenContainer, RiskGreenText, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Check else Icons.Filled.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSaved) "บันทึกแล้ว" else "บันทึกผล")
                }

                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("แชร์รายงาน")
                }
            }
        }
    }
}

@Composable
fun CountBadgeItem(title: String, count: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bg
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
            Text(text = count, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = fg)
        }
    }
}

@Composable
fun FilterRiskRow(
    selectedLevel: RiskLevel?,
    redCount: Int,
    yellowCount: Int,
    greenCount: Int,
    onSelect: (RiskLevel?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedLevel == null,
                onClick = { onSelect(null) },
                label = { Text("ทั้งหมด (${redCount + yellowCount + greenCount})") }
            )
        }
        item {
            FilterChip(
                selected = selectedLevel == RiskLevel.RED,
                onClick = { onSelect(RiskLevel.RED) },
                label = { Text("🔴 สีแดง ($redCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RiskRedContainer,
                    selectedLabelColor = RiskRedText
                )
            )
        }
        item {
            FilterChip(
                selected = selectedLevel == RiskLevel.YELLOW,
                onClick = { onSelect(RiskLevel.YELLOW) },
                label = { Text("🟡 สีเหลือง ($yellowCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RiskYellowContainer,
                    selectedLabelColor = RiskYellowText
                )
            )
        }
        item {
            FilterChip(
                selected = selectedLevel == RiskLevel.GREEN,
                onClick = { onSelect(RiskLevel.GREEN) },
                label = { Text("🟢 สีเขียว ($greenCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RiskGreenContainer,
                    selectedLabelColor = RiskGreenText
                )
            )
        }
    }
}

@Composable
fun ClauseFindingCard(
    finding: ClauseFinding,
    onCopyAdvice: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val (cardBg, borderColor, textColor, badgeText) = when (finding.level) {
        RiskLevel.RED -> Quad(RiskRedContainer.copy(alpha = 0.25f), RiskRedBorder, RiskRedText, "🔴 ผิดกฎหมาย สคบ. / ตกเป็นโมฆะ")
        RiskLevel.YELLOW -> Quad(RiskYellowContainer.copy(alpha = 0.25f), RiskYellowBorder, RiskYellowText, "🟡 ข้อควรระวัง / เสียเปรียบ")
        RiskLevel.GREEN -> Quad(RiskGreenContainer.copy(alpha = 0.25f), RiskGreenBorder, RiskGreenText, "🟢 ถูกต้องตามกฎหมาย สคบ.")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .animateContentSize()
            .testTag("finding_card_${finding.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (finding.level) {
                        RiskLevel.RED -> RiskRedContainer
                        RiskLevel.YELLOW -> RiskYellowContainer
                        RiskLevel.GREEN -> RiskGreenContainer
                    }
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = finding.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = finding.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Detected quote
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = finding.quoteSnippet,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Law Reference
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = finding.lawArticle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = finding.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Expandable details (Advice & Amendment template)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "💡 คำแนะนำสำหรับผู้เช่า:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = finding.tenantAdvice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "📝 ข้อความแนะนำสำหรับขอแก้สัญญา:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = finding.suggestedAmendmentText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = onCopyAdvice,
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("คัดลอกข้อความไปคุยกับหอพัก", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (expanded) "ย่อรายละเอียด ▲" else "ดูคำแนะนำวิธีเจรจาและข้อความขอแก้สัญญา ▼")
            }
        }
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "คัดลอกข้อความเรียบร้อยแล้ว นำไปส่งผู้ให้เช่าได้ทันที", Toast.LENGTH_SHORT).show()
}

private fun shareInspectionReport(context: Context, dormName: String, summary: com.example.data.analysis.OcpbRuleEngine.InspectionSummary) {
    val report = buildString {
        appendLine("🛡️ รายงานตรวจสอบสัญญาเช่าหอพัก: $dormName")
        appendLine("อ้างอิง: กฎหมาย สคบ. พ.ศ. 2562 (ควบคุมสัญญาเช่าอาคารเพื่ออยู่อาศัย)")
        appendLine("คะแนนความปลอดภัย: ${summary.safetyScore}/100")
        appendLine("สรุปผล: ${summary.verdict}")
        appendLine("----------------------------------------")
        appendLine("🔴 ข้อผิดกฎหมาย สคบ. (สีแดง): ${summary.redCount} ข้อ")
        appendLine("🟡 ข้อควรระวัง (สีเหลือง): ${summary.yellowCount} ข้อ")
        appendLine("🟢 ข้อที่เป็นธรรม (สีเขียว): ${summary.greenCount} ข้อ")
        appendLine("----------------------------------------")
        summary.findings.forEachIndexed { i, f ->
            appendLine("${i + 1}. [${f.level.name}] ${f.title}")
            appendLine("   กฎหมาย: ${f.lawArticle}")
            appendLine("   คำแนะนำ: ${f.tenantAdvice}")
        }
        appendLine("----------------------------------------")
        appendLine("ตรวจสอบผ่านแอป LeaseCheck")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "รายงานผลตรวจสอบสัญญาเช่า $dormName - LeaseCheck")
        putExtra(Intent.EXTRA_TEXT, report)
    }
    context.startActivity(Intent.createChooser(intent, "แชร์รายงานตรวจสอบสัญญา"))
}
