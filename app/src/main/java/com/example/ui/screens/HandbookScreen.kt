package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RiskLevel
import com.example.ui.theme.*

@Composable
fun HandbookScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("handbook_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "คู่มือสิทธิผู้เช่า & กฎหมาย สคบ. 2562",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "ประกาศควบคุมสัญญาเช่าอาคารเพื่ออยู่อาศัย",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "กฎหมายฉบับนี้มีผลบังคับใช้กับผู้ประกอบธุรกิจให้เช่าตั้งแต่ 5 ห้องหรือ 5 หน่วยขึ้นไป มีบทลงโทษจำคุกไม่เกิน 1 ปี หรือปรับไม่เกิน 100,000 บาท หรือทั้งจำทั้งปรับ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Contact OCPB Hotlines
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📞 ช่องทางร้องเรียนและปรึกษา สคบ.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1166"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RiskRed)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("สายด่วน 1166")
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://complaint.ocpb.go.th"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ร้องเรียนออนไลน์")
                        }
                    }
                }
            }
        }

        // 10 Prohibitions
        item {
            Text(
                text = "🚫 10 ข้อห้ามเด็ดขาดของผู้ให้เช่าหอพักตาม สคบ. 2562",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val rulesList = listOf(
            RuleItemData("1. ห้ามคิดค่าน้ำ-ค่าไฟเกินอัตราจริง", "ต้องคิดตามอัตราจริงที่การไฟฟ้าและการประปาเรียกเก็บ ห้ามบวกกำไรส่วนเกิน", RiskLevel.RED),
            RuleItemData("2. ห้ามเรียกเก็บเงินประกันเกิน 1 เดือน", "เงินประกันความเสียหายต้องไม่เกิน 1 เดือนของอัตราค่าเช่า", RiskLevel.RED),
            RuleItemData("3. ห้ามเรียกเก็บค่าเช่าล่วงหน้าเกิน 1 เดือน", "รวมเงินประกันและค่าเช่าล่วงหน้าต้องไม่เกิน 2 เดือน ณ วันทำสัญญา", RiskLevel.RED),
            RuleItemData("4. ห้ามล็อคห้อง ยึดทรัพย์สิน หรือตัดน้ำไฟ", "การล็อคห้องหรือยึดทรัพย์สินเป็นความผิดทางอาญาฐานบุกรุกและทำให้เสียทรัพย์ ผู้ให้เช่าไม่มีสิทธิทำได้", RiskLevel.RED),
            RuleItemData("5. ห้ามเข้าตรวจห้องพักโดยไม่แจ้งล่วงหน้า", "ต้องแจ้งผู้เช่าล่วงหน้าเป็นหนังสือหรือข้อความในระยะเวลาอันสมควร", RiskLevel.RED),
            RuleItemData("6. ห้ามเรียกเก็บค่าต่อสัญญาเช่า", "การต่อสัญญาเช่าห้ามมีค่าธรรมเนียมเพิ่มเติมใดๆ", RiskLevel.RED),
            RuleItemData("7. ห้ามปรับขึ้นราคาในระหว่างสัญญา", "ห้ามเปลี่ยนแปลงอัตราค่าเช่า ค่าน้ำ ค่าไฟ หรือค่าบริการก่อนสัญญาจะสิ้นสุด", RiskLevel.RED),
            RuleItemData("8. ต้องคืนเงินประกันภายใน 7 วัน", "เมื่อสัญญาเช่าสิ้นสุดและผู้เช่าย้ายออก ต้องคืนเงินประกันทันทีหรือไม่เกิน 7 วัน", RiskLevel.GREEN),
            RuleItemData("9. ผู้เช่ามีสิทธิบอกเลิกสัญญาแจ้งล่วงหน้า 30 วัน", "หากมีเหตุจำเป็น สามารถบอกเลิกสัญญาก่อนกำหนดได้โดยแจ้งเป็นหนังสือล่วงหน้า 30 วัน", RiskLevel.GREEN),
            RuleItemData("10. ความเสื่อมสภาพตามปกติเป็นหน้าที่ผู้ให้เช่า", "การชำรุดจากการใช้งานปกติ (Wear and Tear) ผู้ให้เช่าต้องเป็นผู้ซ่อมแซม", RiskLevel.YELLOW)
        )

        items(rulesList.size) { index ->
            val rule = rulesList[index]
            RuleDetailCard(rule)
        }

        // Formal Letter Template for Landlord
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📝 แบบฟอร์มหนังสือขอความร่วมมือแก้ไขสัญญาเช่า",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ใช้สำหรับส่งให้เจ้าของหอพักทางแชทหรือยื่นเป็นเอกสารเพื่อขอปรับแก้สัญญาให้ถูกต้องตามกฎหมาย",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val templateText = """
เรียน ผู้ให้เช่า / ผู้ดูแลหอพัก

เนื่องด้วยกระผม/ดิฉัน ผู้เช่าห้องพัก ได้ตรวจสอบข้อความในสัญญาเช่าห้องพัก พบว่ามีบางข้อที่ยังไม่สอดคล้องกับ "ประกาศคณะกรรมการว่าด้วยสัญญา เรื่อง ให้ธุรกิจการให้เช่าอาคารเพื่ออยู่อาศัยเป็นธุรกิจที่ควบคุมสัญญา พ.ศ. 2562" ของสำนักงานคณะกรรมการคุ้มครองผู้บริโภค (สคบ.) อาทิ:
1. การคิดค่าน้ำ-ค่าไฟเกินกว่าอัตราจริงที่หน่วยงานรัฐเรียกเก็บ
2. การเรียกเก็บเงินประกันเกินกว่าอัตราค่าเช่า 1 เดือน
3. ข้อกำหนดเรื่องการริบเงินประกันและการบอกเลิกสัญญา

จึงใคร่ขอความอนุเคราะห์จากทางหอพักในการพิจารณาปรับปรุงข้อสัญญาดังกล่าวให้สอดคล้องตามประกาศ สคบ. 2562 เพื่อความเป็นธรรมและถูกต้องตามกฎหมายร่วมกัน

ขอแสดงความนับถือ
ผู้เช่าห้องพัก
                    """.trimIndent()

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = templateText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("แบบฟอร์มขอแก้สัญญา", templateText))
                            Toast.makeText(context, "คัดลอกแบบฟอร์มหนังสือเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("คัดลอกแบบฟอร์มข้อความ")
                    }
                }
            }
        }
    }
}

data class RuleItemData(val title: String, val desc: String, val level: RiskLevel)

@Composable
fun RuleDetailCard(item: RuleItemData) {
    val (badgeBg, borderC, textC) = when (item.level) {
        RiskLevel.RED -> Triple(RiskRedContainer, RiskRedBorder, RiskRedText)
        RiskLevel.YELLOW -> Triple(RiskYellowContainer, RiskYellowBorder, RiskYellowText)
        RiskLevel.GREEN -> Triple(RiskGreenContainer, RiskGreenBorder, RiskGreenText)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderC.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeBg,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = when (item.level) {
                        RiskLevel.RED -> "ข้อห้าม"
                        RiskLevel.YELLOW -> "ข้อควรระวัง"
                        RiskLevel.GREEN -> "สิทธิผู้เช่า"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textC
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
