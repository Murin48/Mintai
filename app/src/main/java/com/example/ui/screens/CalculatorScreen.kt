package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LeaseViewModel
import com.example.ui.theme.*
import java.text.DecimalFormat

@Composable
fun CalculatorScreen(
    viewModel: LeaseViewModel,
    modifier: Modifier = Modifier
) {
    val calcRent by viewModel.calcRent.collectAsState()
    val calcDeposit by viewModel.calcDeposit.collectAsState()
    val calcElecRate by viewModel.calcElecRate.collectAsState()
    val calcElecUnits by viewModel.calcElecUnits.collectAsState()
    val calcWaterRate by viewModel.calcWaterRate.collectAsState()
    val calcWaterUnits by viewModel.calcWaterUnits.collectAsState()

    val calculation = viewModel.getCalculation()
    val df = remember { DecimalFormat("#,##0.00") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("calculator_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "คำนวณส่วนต่างค่าน้ำ-ไฟ และเงินประกัน",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "เปรียบเทียบอัตราจริงตามกฎหมาย สคบ. 2562 เพื่อดูยอดเงินที่ถูกเก็บเกิน",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Section 1: Security Deposit Check
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. ตรวจสอบเงินประกันความเสียหาย",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = calcRent,
                            onValueChange = { viewModel.updateCalcRent(it) },
                            label = { Text("ค่าเช่า/เดือน (บาท)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = calcDeposit,
                            onValueChange = { viewModel.updateCalcDeposit(it) },
                            label = { Text("เงินประกันที่จ่าย (บาท)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (calculation.isDepositOverLimit) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RiskRedContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = RiskRedText)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "🔴 สคบ. ห้ามเก็บเงินประกันเกิน 1 เดือน!",
                                        fontWeight = FontWeight.Bold,
                                        color = RiskRedText
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "หอพักเรียกเก็บเกินกฎหมาย: ${df.format(calculation.depositExcessAmount)} บาท (ผู้เช่ามีสิทธิขอคืนหรือนำไปหักค่าเช่าได้)",
                                    fontSize = 13.sp,
                                    color = RiskRedText
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RiskGreenContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircleOutline, contentDescription = null, tint = RiskGreenText)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🟢 เงินประกันไม่เกิน 1 เดือน ถูกต้องตาม สคบ. 2562",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RiskGreenText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Utilities Check (Electricity & Water)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ElectricBolt, contentDescription = null, tint = AmberTertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. ตรวจสอบค่าน้ำประปาและค่าไฟฟ้า",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "⚡ ค่าไฟฟ้า (อัตราฐานการไฟฟ้าเฉลี่ย ~4.20 บาท/หน่วย)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = calcElecRate,
                            onValueChange = { viewModel.updateCalcElecRate(it) },
                            label = { Text("หอพักคิด (บ./หน่วย)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = calcElecUnits,
                            onValueChange = { viewModel.updateCalcElecUnits(it) },
                            label = { Text("หน่วยที่ใช้/เดือน") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "💧 ค่าน้ำประปา (อัตราฐานการประปาเฉลี่ย ~12.50 บาท/หน่วย)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = calcWaterRate,
                            onValueChange = { viewModel.updateCalcWaterRate(it) },
                            label = { Text("หอพักคิด (บ./หน่วย)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = calcWaterUnits,
                            onValueChange = { viewModel.updateCalcWaterUnits(it) },
                            label = { Text("หน่วยที่ใช้/เดือน") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Section 3: Summary of Financial Impact
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "📊 สรุปยอดเงินที่ถูกเรียกเก็บเกินจริง (Overcharge Summary)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OverchargeRow(
                        title = "ส่วนต่างค่าไฟฟ้าต่อเดือน:",
                        amount = calculation.monthlyElecLoss,
                        isExcess = calculation.elecOverchargePerUnit > 0
                    )
                    OverchargeRow(
                        title = "ส่วนต่างค่าน้ำประปาต่อเดือน:",
                        amount = calculation.monthlyWaterLoss,
                        isExcess = calculation.waterOverchargePerUnit > 0
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "รวมยอดเงินเสียเปรียบต่อปี:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "(สัญญาเช่า 1 ปี)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${df.format(calculation.annualOverchargeTotal)} บาท/ปี",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (calculation.annualOverchargeTotal > 0) RiskRedText else RiskGreenText
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "สิทธิทางกฎหมายของผู้เช่า:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ตามประมวลกฎหมายแพ่งและพาณิชย์ มาตรา 406 (ลาภมิควรได้) และประกาศ สคบ. 2562 ผู้เช่าที่ถูกเรียกเก็บค่าน้ำไฟเกินจริงมีสิทธิฟ้องเรียกเงินส่วนเกินคืนย้อนหลังได้ หรือร้องเรียนต่อคณะกรรมการคุ้มครองผู้บริโภค (สายด่วน 1166)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverchargeRow(title: String, amount: Double, isExcess: Boolean) {
    val df = remember { DecimalFormat("#,##0.00") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 14.sp)
        Text(
            text = if (isExcess) "+${df.format(amount)} บ./เดือน" else "ปกติ (0.00 บ.)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isExcess) RiskRedText else RiskGreenText
        )
    }
}
