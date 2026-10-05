package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.analysis.OcpbRuleEngine
import com.example.data.model.LeaseInspectionEntity
import com.example.ui.LeaseViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: LeaseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedInspections by viewModel.savedInspections.collectAsState()
    val selectedItem by viewModel.selectedHistoryInspection.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("ล้างประวัติการตรวจสอบทั้งหมด?") },
            text = { Text("ข้อมูลการตรวจสัญญาเช่าทั้งหมดที่บันทึกไว้จะถูกลบออกจากเครื่อง") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("ยืนยันลบ", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    // Detail Dialog when an item is clicked
    if (selectedItem != null) {
        val item = selectedItem!!
        val findings = remember(item.findingsJson) {
            OcpbRuleEngine.deserializeFindings(item.findingsJson)
        }

        AlertDialog(
            onDismissRequest = { viewModel.clearSelectedHistoryDetail() },
            title = {
                Column {
                    Text(
                        text = item.dormitoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "คะแนนความปลอดภัย: ${item.safetyScore}/100",
                        fontSize = 12.sp,
                        color = if (item.safetyScore >= 80) RiskGreenText else if (item.safetyScore >= 50) RiskYellowText else RiskRedText
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = item.verdict,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🔴 ${item.redCount}", fontSize = 12.sp, color = RiskRedText, fontWeight = FontWeight.Bold)
                            Text("🟡 ${item.yellowCount}", fontSize = 12.sp, color = RiskYellowText, fontWeight = FontWeight.Bold)
                            Text("🟢 ${item.greenCount}", fontSize = 12.sp, color = RiskGreenText, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    items(findings) { finding ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "[${finding.level.name}] ${finding.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(text = finding.lawArticle, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = finding.explanation, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSelectedHistoryDetail() }) {
                    Text("ปิด")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHistoryItem(item.id)
                    }
                ) {
                    Text("ลบรายการนี้", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ประวัติการตรวจสอบสัญญา",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "บันทึกในฐานข้อมูลเครื่อง (${savedInspections.size} รายการ)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (savedInspections.isNotEmpty()) {
                    IconButton(onClick = { showClearConfirmDialog = true }) {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = "ล้างประวัติทั้งหมด",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (savedInspections.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HistoryToggleOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ยังไม่มีประวัติการตรวจสอบ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "เมื่อคุณตรวจสอบสัญญาเช่าในแท็บแรก สามารถกด 'บันทึกผล' เพื่อเก็บไว้เปรียบเทียบในหน้านี้ได้",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(savedInspections, key = { it.id }) { item ->
                HistoryInspectionCard(
                    item = item,
                    onClick = { viewModel.viewHistoryDetail(item) },
                    onDelete = { viewModel.deleteHistoryItem(item.id) }
                )
            }
        }
    }
}

@Composable
fun HistoryInspectionCard(
    item: LeaseInspectionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("th", "TH")) }
    val dateStr = remember(item.analyzedDate) { dateFormat.format(Date(item.analyzedDate)) }

    val scoreColor = when {
        item.safetyScore >= 80 -> RiskGreen
        item.safetyScore >= 50 -> RiskYellow
        else -> RiskRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.dormitoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scoreColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${item.safetyScore}/100",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "ลบรายการ",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.verdict,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔴 ${item.redCount}", fontSize = 11.sp, color = RiskRedText, fontWeight = FontWeight.Bold)
                    Text("🟡 ${item.yellowCount}", fontSize = 11.sp, color = RiskYellowText, fontWeight = FontWeight.Bold)
                    Text("🟢 ${item.greenCount}", fontSize = 11.sp, color = RiskGreenText, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
