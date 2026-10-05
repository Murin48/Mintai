package com.example.data.analysis

import com.example.data.model.ClauseFinding
import com.example.data.model.RiskLevel
import org.json.JSONArray
import org.json.JSONObject

object OcpbRuleEngine {

    data class InspectionSummary(
        val safetyScore: Int,
        val verdict: String,
        val redCount: Int,
        val yellowCount: Int,
        val greenCount: Int,
        val findings: List<ClauseFinding>
    )

    fun evaluateContract(contractText: String): InspectionSummary {
        val findings = mutableListOf<ClauseFinding>()
        val normalizedText = contractText.lowercase()

        // 1. ตรวจสอบค่าไฟฟ้า (Electricity Charge)
        checkElectricityClause(normalizedText, contractText, findings)

        // 2. ตรวจสอบค่าน้ำประปา (Water Charge)
        checkWaterClause(normalizedText, contractText, findings)

        // 3. ตรวจสอบเงินประกัน (Security Deposit)
        checkSecurityDepositClause(normalizedText, contractText, findings)

        // 4. ตรวจสอบค่าเช่าล่วงหน้า (Advance Rent)
        checkAdvanceRentClause(normalizedText, contractText, findings)

        // 5. ตรวจสอบการล็อคห้อง/ยึดทรัพย์สิน/ตัดน้ำไฟ (Lockout & Property Seizure)
        checkLockoutAndSeizureClause(normalizedText, contractText, findings)

        // 6. ตรวจสอบการเข้าตรวจห้องพักโดยพลการ (Entry Without Notice)
        checkInspectionEntryClause(normalizedText, contractText, findings)

        // 7. ตรวจสอบการบอกเลิกสัญญาก่อนกำหนด (Early Termination)
        checkEarlyTerminationClause(normalizedText, contractText, findings)

        // 8. ตรวจสอบระยะเวลาคืนเงินประกัน (Deposit Return Timeline)
        checkDepositRefundTimelineClause(normalizedText, contractText, findings)

        // 9. ตรวจสอบการคิดค่าต่อสัญญาเช่า (Contract Renewal Fee)
        checkRenewalFeeClause(normalizedText, contractText, findings)

        // 10. ตรวจสอบค่าซ่อมแซมและการเสื่อมสภาพตามปกติ (Maintenance & Wear & Tear)
        checkMaintenanceClause(normalizedText, contractText, findings)

        // 11. ตรวจสอบการปรับขึ้นราคาค่าเช่าระหว่างสัญญา (Unilateral Fee Alteration)
        checkUnilateralFeeChangeClause(normalizedText, contractText, findings)

        // 12. ตรวจสอบค่าปรับล่าช้า (Late Payment Fee)
        checkLatePaymentFeeClause(normalizedText, contractText, findings)

        val redCount = findings.count { it.level == RiskLevel.RED }
        val yellowCount = findings.count { it.level == RiskLevel.YELLOW }
        val greenCount = findings.count { it.level == RiskLevel.GREEN }

        // Compute Safety Score (100 is fully compliant)
        val score = (100 - (redCount * 25) - (yellowCount * 10)).coerceIn(0, 100)

        val verdict = when {
            redCount > 0 -> "พบข้อสัญญาผิดกฎหมาย สคบ. 2562 (ระดับสีแดง $redCount ข้อ) — มีข้อสัญญาตกเป็นโมฆะและอาจถูกเอาเปรียบ"
            yellowCount > 0 -> "ผ่านเกณฑ์พื้นฐาน แต่มีข้อควรระวัง (ระดับสีเหลือง $yellowCount ข้อ) — ควรเจรจาขอแก้ข้อความให้รัดกุม"
            else -> "สัญญาถูกต้อง เป็นธรรม สอดคล้องตามประกาศ สคบ. 2562 (ระดับสีเขียว) — ปลอดภัยสำหรับผู้เช่า"
        }

        return InspectionSummary(
            safetyScore = score,
            verdict = verdict,
            redCount = redCount,
            yellowCount = yellowCount,
            greenCount = greenCount,
            findings = findings
        )
    }

    private fun checkElectricityClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        val elecOverchargeRegex = Regex("(?:ค่าไฟ|ไฟฟ้า)[^\\n]{0,40}?(?:หน่วยละ|ยูนิตละ|อัตรา)\\s*([6-9]|1[0-9])\\s*(?:บาท)?")
        val elecMatch = elecOverchargeRegex.find(text)

        if (elecMatch != null || text.contains("ค่าไฟหน่วยละ 7") || text.contains("ค่าไฟหน่วยละ 8") ||
            text.contains("ค่าไฟหน่วยละ 9") || text.contains("ค่าไฟหน่วยละ 10")
        ) {
            val snippet = elecMatch?.value ?: "ระบุค่าไฟหน่วยละเกินอัตราจริง"
            list.add(
                ClauseFinding(
                    id = "elec_red",
                    category = "ค่าน้ำ-ค่าไฟ",
                    title = "เรียกเก็บค่าไฟเกินอัตราการไฟฟ้า",
                    level = RiskLevel.RED,
                    quoteSnippet = snippet,
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 5 (ห้ามเก็บค่าน้ำ-ค่าไฟเกินกว่าอัตราที่การไฟฟ้าเรียกเก็บ)",
                    explanation = "กฎหมาย สคบ. กำหนดให้ผู้ให้เช่าต้องคิดค่าไฟฟ้าตามใบเสร็จจริงที่การไฟฟ้านครหลวงหรือส่วนภูมิภาคเรียกเก็บ ห้ามบวกกำไรส่วนเกิน",
                    tenantAdvice = "ขอให้ผู้ให้เช่าคิดตามใบเสร็จการไฟฟ้าจริง หรือคิดไม่เกินอัตราฐานราชการ (~4.20-4.50 บ./หน่วย)",
                    suggestedAmendmentText = "ขอแก้ไขเป็น: 'ค่าไฟฟ้าให้คิดตามอัตราจริงที่การไฟฟ้านครหลวง/ส่วนภูมิภาคเรียกเก็บจากผู้ประกอบธุรกิจตามประกาศ สคบ.'"
                )
            )
        } else if (text.contains("ตามอัตราการไฟฟ้า") || text.contains("ตามใบเสร็จการไฟฟ้า") || text.contains("ตามบิลการไฟฟ้า")) {
            list.add(
                ClauseFinding(
                    id = "elec_green",
                    category = "ค่าน้ำ-ค่าไฟ",
                    title = "ค่าไฟฟ้าคิดตามอัตราจริงของการไฟฟ้า",
                    level = RiskLevel.GREEN,
                    quoteSnippet = "ค่าไฟฟ้าคิดตามอัตราจริงของรัฐ",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 3 (2)",
                    explanation = "ถูกต้องตามข้อกำหนด สคบ. ไม่มีการบวกกำไรค่าสาธารณูปโภคเกินจริง",
                    tenantAdvice = "ขอตรวจสอบใบเสร็จบิลการไฟฟ้ารายเดือนควบคู่กับเลขมิเตอร์หน้าห้อง",
                    suggestedAmendmentText = "ไม่ต้องแก้ไข ข้อความนี้ถูกต้องตามกฎหมายแล้ว"
                )
            )
        }
    }

    private fun checkWaterClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        val waterOverchargeRegex = Regex("(?:ค่าน้ำ|ประปา)[^\\n]{0,40}?(?:หน่วยละ|ยูนิตละ|อัตรา)\\s*(1[8-9]|2[0-9]|3[0-9])\\s*(?:บาท)?")
        val waterMatch = waterOverchargeRegex.find(text)

        if (waterMatch != null || text.contains("ค่าน้ำหน่วยละ 18") || text.contains("ค่าน้ำหน่วยละ 20") ||
            text.contains("ค่าน้ำหน่วยละ 22") || text.contains("ค่าน้ำหน่วยละ 25") || text.contains("เหมาค่าน้ำเดือนละ")
        ) {
            val snippet = waterMatch?.value ?: "ระบุค่าน้ำเกินอัตราการประปา หรือเก็บแบบเหมาจ่ายสูงเกินควร"
            list.add(
                ClauseFinding(
                    id = "water_red",
                    category = "ค่าน้ำ-ค่าไฟ",
                    title = "เรียกเก็บค่าน้ำประปาเกินจริง / เหมาจ่ายเกินอัตรา",
                    level = RiskLevel.RED,
                    quoteSnippet = snippet,
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 5 (ห้ามเรียกเก็บค่าน้ำประปาเกินกว่าอัตราจริง)",
                    explanation = "หอพักต้องคิดค่าน้ำตามอัตราจริงของการประปานครหลวง/ภูมิภาค (โดยเฉลี่ยประมาณ 10-14 บาท/หน่วย)",
                    tenantAdvice = "ทักท้วงผู้ให้เช่าว่า สคบ. 2562 สั่งห้ามคิดค่าน้ำเกินจริง โทษจำคุกไม่เกิน 1 ปี หรือปรับไม่เกิน 100,000 บาท",
                    suggestedAmendmentText = "ขอแก้ไขเป็น: 'ค่าน้ำประปาให้คิดตามอัตราที่การประปานครหลวง/ส่วนภูมิภาคเรียกเก็บตามจริง'"
                )
            )
        } else if (text.contains("ตามอัตราการประปา") || text.contains("ตามใบเสร็จการประปา")) {
            list.add(
                ClauseFinding(
                    id = "water_green",
                    category = "ค่าน้ำ-ค่าไฟ",
                    title = "ค่าน้ำประปาคิดตามอัตราจริงของการประปา",
                    level = RiskLevel.GREEN,
                    quoteSnippet = "ค่าน้ำประปาคิดตามอัตราจริงของการประปา",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 3 (2)",
                    explanation = "ถูกต้องตามเกณฑ์ สคบ. 2562",
                    tenantAdvice = "ตรวจสอบเลขมิเตอร์น้ำเมื่อเข้าพักและก่อนย้ายออก",
                    suggestedAmendmentText = "ข้อความถูกต้องแล้ว"
                )
            )
        }
    }

    private fun checkSecurityDepositClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        if (text.contains("เงินประกัน 2 เดือน") || text.contains("เงินประกัน 3 เดือน") ||
            text.contains("มัดจำ 2 เดือน") || text.contains("มัดจำ 3 เดือน") ||
            text.contains("ประกันความเสียหาย 2 เดือน")
        ) {
            list.add(
                ClauseFinding(
                    id = "deposit_red",
                    category = "เงินประกันและค่าเช่าล่วงหน้า",
                    title = "เรียกเก็บเงินประกันเกินกว่า 1 เดือน",
                    level = RiskLevel.RED,
                    quoteSnippet = "เรียกเก็บเงินประกัน 2-3 เดือน",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 5 (ห้ามเรียกเก็บเงินประกันเกินกว่า 1 เดือนของอัตราค่าเช่า)",
                    explanation = "กฎหมาย สคบ. ควบคุมสัญญาเช่า ห้ามเรียกเก็บเงินประกันความเสียหายเกินกว่าอัตราค่าเช่า 1 เดือนเด็ดขาด",
                    tenantAdvice = "ผู้เช่ามีสิทธิจ่ายเงินประกันเพียง 1 เดือนเท่านั้น หากจ่ายเกินไปสามารถทวงคืนได้",
                    suggestedAmendmentText = "ขอแก้ไขเป็น: 'ผู้เช่าตกลงวางเงินประกันความเสียหายเท่ากับอัตราค่าเช่า 1 เดือน เป็นจำนวนเงิน...บาท'"
                )
            )
        } else if (text.contains("เงินประกัน 1 เดือน") || text.contains("มัดจำ 1 เดือน") || text.contains("ประกันความเสียหาย 1 เดือน")) {
            list.add(
                ClauseFinding(
                    id = "deposit_green",
                    category = "เงินประกันและค่าเช่าล่วงหน้า",
                    title = "เงินประกันไม่เกิน 1 เดือน ถูกต้องตามกฎหมาย",
                    level = RiskLevel.GREEN,
                    quoteSnippet = "เงินประกันความเสียหาย 1 เดือน",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 4 (1)",
                    explanation = "เงินประกัน 1 เดือน ถูกต้องตามเพดานสูงสุดที่กฎหมายกำหนด",
                    tenantAdvice = "ขอใบเสร็จรับเงินประกันและถ่ายภาพสภาพห้องพักไว้เป็นหลักฐาน",
                    suggestedAmendmentText = "ข้อความถูกต้องแล้ว"
                )
            )
        }
    }

    private fun checkAdvanceRentClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        if (text.contains("ค่าเช่าล่วงหน้า 2 เดือน") || text.contains("ค่าเช่าล่วงหน้า 3 เดือน") ||
            text.contains("ชำระล่วงหน้า 2 เดือน")
        ) {
            list.add(
                ClauseFinding(
                    id = "advance_red",
                    category = "เงินประกันและค่าเช่าล่วงหน้า",
                    title = "เรียกเก็บค่าเช่าล่วงหน้าเกิน 1 เดือน",
                    level = RiskLevel.RED,
                    quoteSnippet = "เรียกเก็บค่าเช่าล่วงหน้า 2 เดือนขึ้นไป",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 5 (ห้ามเรียกเก็บค่าเช่าล่วงหน้าเกิน 1 เดือน)",
                    explanation = "สคบ. กำหนดให้เรียกเก็บค่าเช่าล่วงหน้าได้ไม่เกิน 1 เดือน รวมกับเงินประกันแล้วต้องไม่เกิน 2 เดือน ณ วันทำสัญญา",
                    tenantAdvice = "เจรจาจ่ายค่าเช่าล่วงหน้าเพียง 1 เดือนตามสิทธิ",
                    suggestedAmendmentText = "ขอแก้ไขเป็น: 'ผู้เช่าตกลงชำระค่าเช่าล่วงหน้า 1 เดือน ณ วันทำสัญญา'"
                )
            )
        } else if (text.contains("ค่าเช่าล่วงหน้า 1 เดือน") || text.contains("ชำระค่าเช่าล่วงหน้า 1 เดือน")) {
            list.add(
                ClauseFinding(
                    id = "advance_green",
                    category = "เงินประกันและค่าเช่าล่วงหน้า",
                    title = "ค่าเช่าล่วงหน้าไม่เกิน 1 เดือน",
                    level = RiskLevel.GREEN,
                    quoteSnippet = "ค่าเช่าล่วงหน้า 1 เดือน",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 4 (2)",
                    explanation = "สอดคล้องตามระเบียบ สคบ.",
                    tenantAdvice = "เก็บสลิปการโอนและใบเสร็จไว้เสมอ",
                    suggestedAmendmentText = "ข้อความถูกต้องแล้ว"
                )
            )
        }
    }

    private fun checkLockoutAndSeizureClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        if (text.contains("ล็อคห้อง") || text.contains("ล็อกห้อง") || text.contains("คล้องกุญแจ") ||
            text.contains("ยึดทรัพย์สิน") || text.contains("ริบทรัพย์สิน") || text.contains("ขนย้ายทรัพย์สิน") ||
            text.contains("ตัดน้ำตัดไฟ") || text.contains("งดจ่ายน้ำไฟ")
        ) {
            list.add(
                ClauseFinding(
                    id = "lockout_red",
                    category = "การล็อคห้องและยึดทรัพย์สิน",
                    title = "ล็อคห้อง ตัดน้ำไฟ หรือยึดทรัพย์สินโดยพลการ (ผิดกฎหมายร้ายแรง)",
                    level = RiskLevel.RED,
                    quoteSnippet = "ระบุสิทธิผู้ให้เช่าในการล็อคกุญแจห้อง ตัดน้ำตัดไฟ หรือยึดทรัพย์สินหากผิดนัด",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 5 (ห้ามมิให้ผู้ให้เช่าปิดกั้นมิให้ผู้เช่าเข้าใช้ประโยชน์ในอาคาร หรือเข้ายึดครอบครองทรัพย์สิน)",
                    explanation = "การล็อคห้องหรือตัดน้ำตัดไฟถือเป็นความผิดอาญาฐานบุกรุกและลักทรัพย์/ทำให้เสียทรัพย์ ผู้ให้เช่าไม่มีอำนาจทำได้ตามอำเภอใจ ต้องฟ้องร้องขับไล่ทางศาลเท่านั้น ข้อสัญญานี้ตกเป็นโมฆะ",
                    tenantAdvice = "แจ้งผู้ให้เช่าว่าข้อสัญญานี้ผิดกฎหมาย สคบ. และมีโทษทางอาญา หากผู้ให้เช่าล็อคห้อง ผู้เช่าสามารถแจ้งความดำเนินคดีได้ทันที",
                    suggestedAmendmentText = "ขอตัดข้อความการล็อคห้อง ยึดทรัพย์สิน และตัดน้ำตัดไฟออกทั้งหมด โดยใช้กระบวนการบอกกล่าวตามกฎหมายแทน"
                )
            )
        }
    }

    private fun checkInspectionEntryClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        if (text.contains("เข้าตรวจได้ตลอดเวลาโดยไม่ต้องแจ้ง") || text.contains("เข้าห้องพักโดยไม่ต้องบอกกล่าว") ||
            text.contains("โดยมิต้องบอกล่วงหน้า") || text.contains("โดยไม่ต้องได้รับอนุญาต")
        ) {
            list.add(
                ClauseFinding(
                    id = "entry_red",
                    category = "การเข้าตรวจห้องพัก",
                    title = "ผู้ให้เช่าสงวนสิทธิเข้าห้องพักโดยไม่แจ้งล่วงหน้า",
                    level = RiskLevel.RED,
                    quoteSnippet = "เข้าตรวจห้องพักได้โดยมิต้องบอกกล่าวล่วงหน้า",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 5 (ห้ามผู้ให้เช่าเข้าตรวจห้องพักโดยไม่แจ้งให้ผู้เช่าทราบล่วงหน้า)",
                    explanation = "ผู้ให้เช่าไม่มีสิทธิบุกรุกห้องพักของผู้เช่าตามอำเภอใจ ต้องแจ้งล่วงหน้าในระยะเวลาอันสมควร เว้นแต่เกิดเหตุฉุกเฉินภัยพิบัติ",
                    tenantAdvice = "ขอแก้ไขให้ต้องแจ้งล่วงหน้าอย่างน้อย 24-48 ชั่วโมง และต้องมีผู้เช่าอยู่ร่วมด้วย",
                    suggestedAmendmentText = "ขอแก้ไขเป็น: 'ผู้ให้เช่ามีสิทธิเข้าตรวจห้องพักได้โดยต้องแจ้งให้ผู้เช่าทราบล่วงหน้าไม่น้อยกว่า 24 ชั่วโมง และได้รับความยินยอมจากผู้เช่า เว้นแต่กรณีเกิดเหตุฉุกเฉินร้ายแรง'"
                )
            )
        } else if (text.contains("แจ้งล่วงหน้าก่อนเข้าตรวจ") || text.contains("แจ้งให้ผู้เช่าทราบล่วงหน้า")) {
            list.add(
                ClauseFinding(
                    id = "entry_green",
                    category = "การเข้าตรวจห้องพัก",
                    title = "กำหนดให้แจ้งล่วงหน้าก่อนเข้าตรวจห้องพัก",
                    level = RiskLevel.GREEN,
                    quoteSnippet = "แจ้งล่วงหน้าก่อนเข้าตรวจห้องพัก",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 4 (5)",
                    explanation = "คุ้มครองสิทธิความเป็นส่วนตัวของผู้เช่าตามเกณฑ์ สคบ.",
                    tenantAdvice = "นัดหมายเวลาที่สะดวกก่อนให้ช่างหรือผู้ให้เช่าเข้าห้อง",
                    suggestedAmendmentText = "ข้อความถูกต้องแล้ว"
                )
            )
        }
    }

    private fun checkEarlyTerminationClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        if (text.contains("ห้ามบอกเลิกสัญญา") || text.contains("ไม่อนุญาตให้ย้ายออกก่อนครบ") ||
            text.contains("หากอยู่ไม่ครบกำหนดริบเงินประกันทั้งหมด") || text.contains("ริบเงินประกันทันทีหากอยู่ไม่ครบ")
        ) {
            list.add(
                ClauseFinding(
                    id = "term_red",
                    category = "การบอกเลิกสัญญาเช่า",
                    title = "ตัดสิทธิผู้เช่าบอกเลิกสัญญาก่อนกำหนด / ริบเงินประกันทั้งหมด",
                    level = RiskLevel.RED,
                    quoteSnippet = "ห้ามบอกเลิกสัญญาเช่าก่อนครบกำหนด หรือริบเงินประกันทั้งหมดทันที",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 4 (6) และ ข้อ 5",
                    explanation = "ผู้เช่ามีสิทธิตามกฎหมายที่จะบอกเลิกสัญญาก่อนครบกำหนดได้ หากมีเหตุจำเป็น โดยต้องบอกกล่าวล่วงหน้าไม่น้อยกว่า 30 วัน และไม่ค้างชำระค่าเช่า",
                    tenantAdvice = "ทักท้วงขอเพิ่มสิทธิบอกเลิกสัญญาเช่าตามเกณฑ์ สคบ. 2562 ล่วงหน้า 30 วัน",
                    suggestedAmendmentText = "ขอแก้ไขเป็น: 'ผู้เช่ามีสิทธิบอกเลิกสัญญาก่อนสิ้นสุดระยะเวลาการเช่าได้ โดยต้องมีหนังสือบอกกล่าวล่วงหน้าไม่น้อยกว่า 30 วัน และไม่ค้างชำระค่าเช่า'"
                )
            )
        } else if (text.contains("แจ้งย้ายออกล่วงหน้า 60 วัน") || text.contains("บอกกล่าวก่อน 60 วัน") || text.contains("แจ้งล่วงหน้า 2 เดือน")) {
            list.add(
                ClauseFinding(
                    id = "term_yellow",
                    category = "การบอกเลิกสัญญาเช่า",
                    title = "กำหนดระยะเวลาแจ้งย้ายออกล่วงหน้านานเกินควร (60 วัน)",
                    level = RiskLevel.YELLOW,
                    quoteSnippet = "ต้องแจ้งย้ายออกล่วงหน้า 60 วัน หรือ 2 เดือน",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 4 (6) (เกณฑ์ปกติ 30 วัน)",
                    explanation = "สคบ. กำหนดเกณฑ์แจ้งล่วงหน้าไว้ที่ 30 วัน การขยายเป็น 60 วันอาจสร้างภาระให้ผู้เช่าที่จำเป็นต้องย้ายกะทันหัน",
                    tenantAdvice = "ขอเจรจาปรับลดระยะเวลาแจ้งล่วงหน้าเหลือ 30 วันตามมาตรฐาน",
                    suggestedAmendmentText = "ขอปรับลดระยะเวลาแจ้งล่วงหน้าจาก 60 วัน เป็น 30 วัน"
                )
            )
        } else if (text.contains("บอกกล่าวล่วงหน้าไม่น้อยกว่า 30 วัน") || text.contains("แจ้งล่วงหน้า 30 วัน")) {
            list.add(
                ClauseFinding(
                    id = "term_green",
                    category = "การบอกเลิกสัญญาเช่า",
                    title = "สิทธิบอกเลิกสัญญาเช่าล่วงหน้า 30 วัน สอดคล้องตามกฎหมาย",
                    level = RiskLevel.GREEN,
                    quoteSnippet = "บอกกล่าวล่วงหน้าไม่น้อยกว่า 30 วัน",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 4 (6)",
                    explanation = "เป็นไปตามมาตรฐานการคุ้มครองผู้บริโภคของ สคบ. 2562",
                    tenantAdvice = "เมื่อจะย้ายออก ให้ทำหนังสือแจ้งเป็นลายลักษณ์อักษรหรือส่งข้อความพร้อมหลักฐาน",
                    suggestedAmendmentText = "ข้อความถูกต้องแล้ว"
                )
            )
        }
    }

    private fun checkDepositRefundTimelineClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        if (text.contains("ภายใน 45 วัน") || text.contains("ภายใน 60 วัน") || text.contains("ภายใน 90 วัน")) {
            list.add(
                ClauseFinding(
                    id = "refund_yellow",
                    category = "การคืนเงินประกัน",
                    title = "ระยะเวลาคืนเงินประกันยาวนานเกินเกณฑ์ สคบ. (เกิน 7 วัน)",
                    level = RiskLevel.YELLOW,
                    quoteSnippet = "คืนเงินประกันภายใน 45-60 วันหลังย้ายออก",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 4 (7) (ต้องคืนทันทีหรือไม่เกิน 7 วัน)",
                    explanation = "สคบ. ระบุว่าผู้ให้เช่าต้องคืนเงินประกันทันที หรืออย่างช้าไม่เกิน 7 วัน นับแต่วันที่สัญญาเช่าสิ้นสุดและผู้เช่าย้ายออก",
                    tenantAdvice = "ขอแก้ระยะเวลาคืนเงินประกันให้เป็นภายใน 7 วันหลังตรวจสภาพห้อง",
                    suggestedAmendmentText = "ขอแก้ไขเป็น: 'ผู้ให้เช่าต้องคืนเงินประกันความเสียหายให้แก่ผู้เช่าทันที หรือภายใน 7 วันนับแต่วันที่ผู้เช่าย้ายออกและตรวจสอบสภาพห้องเรียบร้อยแล้ว'"
                )
            )
        } else if (text.contains("คืนเงินประกันภายใน 7 วัน") || text.contains("คืนเงินมัดจำภายใน 7 วัน")) {
            list.add(
                ClauseFinding(
                    id = "refund_green",
                    category = "การคืนเงินประกัน",
                    title = "กำหนดคืนเงินประกันภายใน 7 วัน ถูกต้องตาม สคบ.",
                    level = RiskLevel.GREEN,
                    quoteSnippet = "คืนเงินประกันภายใน 7 วัน",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 4 (7)",
                    explanation = "ตรงตามข้อกำหนด สคบ. 2562 ข้อ 4 (7)",
                    tenantAdvice = "เตือนผู้ให้เช่าตรวจห้องพร้อมกันในวันส่งมอบกุญแจ",
                    suggestedAmendmentText = "ข้อความถูกต้องแล้ว"
                )
            )
        }
    }

    private fun checkRenewalFeeClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        if (text.contains("ค่าต่อสัญญา") || text.contains("ค่าธรรมเนียมต่อสัญญา") || text.contains("ค่าทำสัญญาใหม่")) {
            list.add(
                ClauseFinding(
                    id = "renewal_red",
                    category = "ค่าต่อสัญญาและค่าบริการ",
                    title = "เรียกเก็บค่าต่อสัญญาเช่า / ค่าธรรมเนียมทำสัญญา",
                    level = RiskLevel.RED,
                    quoteSnippet = "มีข้อความเรียกเก็บค่าต่อสัญญาหรือค่าทำสัญญาใหม่",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 5 (ห้ามเรียกเก็บค่าต่อสัญญาเช่า)",
                    explanation = "กฎหมาย สคบ. ห้ามผู้ให้เช่าเรียกเก็บค่าต่อสัญญาเช่าใดๆ ทั้งสิ้น ข้อสัญญานี้ถือว่าผิดกฎหมาย",
                    tenantAdvice = "ปฏิเสธการจ่ายค่าต่อสัญญา โดยอ้างอิงประกาศ สคบ. 2562 ข้อ 5",
                    suggestedAmendmentText = "ขอตัดข้อความเรียกเก็บค่าต่อสัญญาออกทั้งหมด"
                )
            )
        }
    }

    private fun checkMaintenanceClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        if (text.contains("ผู้เช่าต้องรับผิดชอบค่าซ่อมแซมทุกกรณี") || text.contains("ซ่อมแซมเองทุกกรณี") ||
            text.contains("รวมถึงความเสื่อมสภาพตามปกติ")
        ) {
            list.add(
                ClauseFinding(
                    id = "maint_yellow",
                    category = "การบำรุงรักษาและการซ่อมแซม",
                    title = "ผลักภาระค่าซ่อมแซมทุกกรณีรวมความเสื่อมสภาพตามปกติให้ผู้เช่า",
                    level = RiskLevel.YELLOW,
                    quoteSnippet = "ผู้เช่าต้องรับผิดชอบค่าซ่อมแซมทุกกรณีแม้เกิดจากการใช้งานปกติ",
                    lawArticle = "ประมวลกฎหมายแพ่งและพาณิชย์ และ พ.ร.บ. ว่าด้วยข้อสัญญาที่ไม่เป็นธรรม พ.ศ. 2540",
                    explanation = "ความเสื่อมสภาพจากการใช้งานตามปกติ (Wear and tear) เช่น หลอดไฟหมดอายุ แอร์เก่า หรือท่อน้ำเสื่อมตามเวลา ถือเป็นหน้าที่ของผู้ให้เช่าที่จะต้องบำรุงรักษา",
                    tenantAdvice = "ขอระบุขอบเขตให้ชัดเจนว่าผู้เช่ารับผิดชอบเฉพาะความเสียหายที่เกิดจากความประมาทเลินเล่อของผู้เช่าเท่านั้น",
                    suggestedAmendmentText = "ขอแก้ไขเป็น: 'ผู้เช่ารับผิดชอบค่าเสียหายเฉพาะกรณีที่เกิดจากความประมาทเลินเล่อหรือจงใจของผู้เช่า ส่วนการเสื่อมสภาพตามปกติเป็นหน้าที่ของผู้ให้เช่า'"
                )
            )
        }
    }

    private fun checkUnilateralFeeChangeClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        if (text.contains("สงวนสิทธิ์ในการปรับขึ้นค่าเช่า") || text.contains("ปรับขึ้นค่าเช่าระหว่างสัญญา") ||
            text.contains("เปลี่ยนแปลงค่าเช่าโดยไม่ต้องแจ้ง")
        ) {
            list.add(
                ClauseFinding(
                    id = "fee_change_red",
                    category = "การปรับเปลี่ยนราคา",
                    title = "ผู้ให้เช่าสงวนสิทธิ์ปรับขึ้นค่าเช่าหรือค่าบริการระหว่างสัญญา",
                    level = RiskLevel.RED,
                    quoteSnippet = "สงวนสิทธิ์ในการปรับขึ้นค่าเช่าระหว่างสัญญา",
                    lawArticle = "ประกาศ สคบ. 2562 ข้อ 5 (ห้ามเปลี่ยนแปลงอัตราค่าเช่าหรือค่าบริการก่อนสัญญาจะสิ้นสุด)",
                    explanation = "ตลอดอายุสัญญาเช่า ผู้ให้เช่าไม่มีสิทธิ์ขึ้นราคาค่าเช่า ค่าน้ำ ค่าไฟ หรือค่าบริการใดๆ ตามอำเภอใจ",
                    tenantAdvice = "ขอตัดเงื่อนไขการปรับราคาในระหว่างอายุสัญญาออก",
                    suggestedAmendmentText = "ขอตัดข้อความการปรับขึ้นค่าเช่าระหว่างอายุสัญญาออกทั้งหมด"
                )
            )
        }
    }

    private fun checkLatePaymentFeeClause(text: String, originalText: String, list: MutableList<ClauseFinding>) {
        val lateRegex = Regex("(?:ค่าปรับ|เบี้ยปรับ|จ่ายช้า)[^\\n]{0,30}?(?:วันละ)\\s*([1-9][0-9]{2,})\\s*(?:บาท)?")
        val match = lateRegex.find(text)
        if (match != null || text.contains("วันละ 200") || text.contains("วันละ 300") || text.contains("วันละ 500")) {
            val snippet = match?.value ?: "คิดค่าปรับล่าช้าวันละ 200 บาทขึ้นไป"
            list.add(
                ClauseFinding(
                    id = "late_yellow",
                    category = "ค่าปรับล่าช้า",
                    title = "ค่าปรับผิดนัดชำระล่าช้ารายวันสูงเกินสมควร",
                    level = RiskLevel.YELLOW,
                    quoteSnippet = snippet,
                    lawArticle = "พ.ร.บ. ว่าด้วยข้อสัญญาที่ไม่เป็นธรรม พ.ศ. 2540 และประมวลกฎหมายแพ่งฯ",
                    explanation = "ค่าปรับล่าช้ารายวันในอัตราที่สูงเกินไป (เช่น วันละ 200-500 บาท) ถือเป็นเบี้ยปรับที่สูงเกินส่วนและศาลมีอำนาจปรับลดลงได้",
                    tenantAdvice = "ขอเจรจาคิดค่าปรับเป็นอัตราคงที่สมเหตุสมผล เช่น ไม่เกินวันละ 50 บาท หรือตามดอกเบี้ยผิดนัด",
                    suggestedAmendmentText = "ขอปรับลดค่าปรับล่าช้าเป็นไม่เกินวันละ 50 บาท และให้มีระยะเวลาผ่อนผัน (Grace period) 3-5 วัน"
                )
            )
        }
    }

    // Helper functions to serialize / deserialize JSON for Room database
    fun serializeFindings(findings: List<ClauseFinding>): String {
        val array = JSONArray()
        for (f in findings) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("category", f.category)
            obj.put("title", f.title)
            obj.put("level", f.level.name)
            obj.put("quoteSnippet", f.quoteSnippet)
            obj.put("lawArticle", f.lawArticle)
            obj.put("explanation", f.explanation)
            obj.put("tenantAdvice", f.tenantAdvice)
            obj.put("suggestedAmendmentText", f.suggestedAmendmentText)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeFindings(jsonStr: String): List<ClauseFinding> {
        val list = mutableListOf<ClauseFinding>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val level = try {
                    RiskLevel.valueOf(obj.getString("level"))
                } catch (_: Exception) {
                    RiskLevel.YELLOW
                }
                list.add(
                    ClauseFinding(
                        id = obj.getString("id"),
                        category = obj.getString("category"),
                        title = obj.getString("title"),
                        level = level,
                        quoteSnippet = obj.getString("quoteSnippet"),
                        lawArticle = obj.getString("lawArticle"),
                        explanation = obj.getString("explanation"),
                        tenantAdvice = obj.getString("tenantAdvice"),
                        suggestedAmendmentText = obj.getString("suggestedAmendmentText")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    // Sample Contract Templates for Instant Testing
    val SAMPLE_CONTRACT_RED = """
สัญญาเช่าห้องพัก หอพักบุญอนันต์
ข้อ 1. ผู้เช่าตกลงเช่าห้องพักเลขที่ 402 อัตราค่าเช่าเดือนละ 4,500 บาท
ข้อ 2. ในวันทำสัญญานี้ ผู้เช่าตกลงวางเงินประกันความเสียหาย 2 เดือน เป็นเงิน 9,000 บาท และค่าเช่าล่วงหน้า 1 เดือน รวมเป็น 13,500 บาท
ข้อ 3. ผู้เช่าตกลงชำระค่าไฟฟ้าในอัตราหน่วยละ 8 บาท และค่าน้ำประปาในอัตราหน่วยละ 20 บาท โดยคิดคำนวณจากมิเตอร์ห้องพัก
ข้อ 4. ผู้ให้เช่ามีสิทธิเข้าไปตรวจสอบห้องพักของผู้เช่าได้ตลอดเวลาโดยไม่ต้องแจ้งให้ผู้เช่าทราบล่วงหน้า เพื่อความปลอดภัยของอาคาร
ข้อ 5. หากผู้เช่าค้างชำระค่าเช่าเกินกว่า 3 วัน ผู้ให้เช่ามีสิทธิล็อคห้อง ตัดน้ำตัดไฟ และยึดทรัพย์สินของผู้เช่าทั้งหมดภายในห้องพักไว้ทันที โดยผู้เช่าไม่มีสิทธิเรียกร้องค่าเสียหาย
ข้อ 6. สัญญาเช่านี้มีกำหนดระยะเวลา 1 ปี หากผู้เช่าย้ายออกก่อนครบกำหนด 1 ปี ไม่ว่าด้วยเหตุผลใดก็ตาม ผู้ให้เช่าจะริบเงินประกันทั้งหมดทันที
ข้อ 7. เมื่อสิ้นสุดสัญญาเช่า 1 ปี หากผู้เช่าประสงค์จะเช่าต่อ ผู้เช่าต้องชำระค่าต่อสัญญาเป็นจำนวนเงิน 1,500 บาท
ข้อ 8. ผู้ให้เช่าขอสงวนสิทธิ์ในการปรับขึ้นค่าเช่าหรือค่าน้ำค่าไฟในระหว่างสัญญาตามภาวะเศรษฐกิจ
    """.trimIndent()

    val SAMPLE_CONTRACT_YELLOW = """
สัญญาเช่าห้องพัก หอพักสุขสราญ
ข้อ 1. อัตราค่าเช่าห้องพักเดือนละ 5,000 บาท
ข้อ 2. ผู้เช่าวางเงินประกันความเสียหาย 1 เดือน (5,000 บาท) และค่าเช่าล่วงหน้า 1 เดือน ณ วันทำสัญญา
ข้อ 3. ค่าน้ำประปาและค่าไฟฟ้าคิดตามอัตราการประปาและการไฟฟ้านครหลวง
ข้อ 4. หากผู้เช่าประสงค์จะย้ายออกก่อนครบกำหนดสัญญา ผู้เช่าต้องทำหนังสือแจ้งย้ายออกล่วงหน้า 60 วัน หากแจ้งไม่ถึง 60 วัน ผู้ให้เช่าขอหักเงินประกัน
ข้อ 5. การคืนเงินประกันความเสียหาย ผู้ให้เช่าจะทำการโอนคืนให้แก่ผู้เช่าภายใน 45 วันหลังจากผู้เช่าย้ายออกและชำระบิลค้างเสร็จสิ้น
ข้อ 6. ผู้เช่าต้องรับผิดชอบค่าซ่อมแซมทุกกรณีในห้องพัก รวมถึงการเสื่อมสภาพตามปกติของอุปกรณ์และเฟอร์นิเจอร์
ข้อ 7. หากผู้เช่าชำระค่าเช่าเกินวันที่ 5 ของเดือน ผู้เช่ายินยอมเสียค่าปรับผิดนัดชำระวันละ 200 บาท
    """.trimIndent()

    val SAMPLE_CONTRACT_GREEN = """
สัญญาเช่าอาคารเพื่ออยู่อาศัย (หอพักกรีนลิฟวิ่ง)
ตามประกาศคณะกรรมการว่าด้วยสัญญา เรื่อง ให้ธุรกิจการให้เช่าอาคารเพื่ออยู่อาศัยเป็นธุรกิจที่ควบคุมสัญญา พ.ศ. 2562
ข้อ 1. ผู้ให้เช่าตกลงให้เช่า และผู้เช่าตกลงเช่าห้องพักเลขที่ 305 อัตราค่าเช่าเดือนละ 5,500 บาท
ข้อ 2. ในวันทำสัญญานี้ ผู้เช่าวางเงินประกันความเสียหาย 1 เดือน เป็นจำนวน 5,500 บาท และชำระค่าเช่าล่วงหน้า 1 เดือน
ข้อ 3. ค่าน้ำประปาคิดตามอัตราจริงของการประปานครหลวง และค่าไฟฟ้าคิดตามอัตราจริงของการไฟฟ้านครหลวง โดยแนบสำเนาใบแจ้งหนี้ให้ผู้เช่าตรวจสอบทุกรอบบิล
ข้อ 4. ผู้ให้เช่าจะเข้าตรวจสอบสภาพห้องพักได้ต่อเมื่อได้แจ้งให้ผู้เช่าทราบล่วงหน้าเป็นลายลักษณ์อักษรไม่น้อยกว่า 24 ชั่วโมง และต้องได้รับความยินยอมจากผู้เช่า เว้นแต่กรณีเหตุฉุกเฉิน
ข้อ 5. ผู้เช่ามีสิทธิบอกเลิกสัญญาก่อนครบกำหนดสัญญาเช่าได้ โดยทำหนังสือบอกกล่าวล่วงหน้าไม่น้อยกว่า 30 วัน ทั้งนี้ผู้เช่าต้องไม่ค้างชำระค่าเช่า
ข้อ 6. เมื่อสัญญาเช่าสิ้นสุดและผู้เช่าส่งมอบห้องพัก ผู้ให้เช่าต้องคืนเงินประกันความเสียหายให้แก่ผู้เช่าทันที หรือคืนเงินประกันภายใน 7 วัน นับแต่วันที่ส่งมอบห้อง โดยหักเฉพาะค่าเสียหายที่เกิดขึ้นจริงตามรายงานตรวจรับห้อง
ข้อ 7. การชำรุดบกพร่องอันเกิดจากการใช้งานตามปกติและการเสื่อมสภาพตามอายุการใช้งานเป็นหน้าที่ของผู้ให้เช่าที่จะต้องทำการซ่อมแซมให้อยู่ในสภาพใช้งานได้ดี
    """.trimIndent()
}
