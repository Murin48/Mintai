package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RiskLevel(val labelTh: String, val descriptionTh: String) {
    RED("แดง (เสี่ยงสูง/ผิดกฎหมาย)", "ละเมิดประกาศ สคบ. 2562 ข้อสัญญานี้ตกเป็นโมฆะ"),
    YELLOW("เหลือง (ข้อควรระวัง)", "คลุมเครือ อาจทำให้ผู้เช่าเสียเปรียบ ควรขอแก้ไข"),
    GREEN("เขียว (ปลอดภัย/เป็นธรรม)", "สอดคล้องตามประกาศ สคบ. 2562 ถูกต้องตามกฎหมาย")
}

data class ClauseFinding(
    val id: String,
    val category: String,
    val title: String,
    val level: RiskLevel,
    val quoteSnippet: String,
    val lawArticle: String,
    val explanation: String,
    val tenantAdvice: String,
    val suggestedAmendmentText: String
)

@Entity(tableName = "lease_inspections")
data class LeaseInspectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dormitoryName: String,
    val analyzedDate: Long = System.currentTimeMillis(),
    val safetyScore: Int, // 0 to 100
    val verdict: String,
    val redCount: Int,
    val yellowCount: Int,
    val greenCount: Int,
    val contractExcerpt: String,
    val findingsJson: String
)

data class DormitoryOverchargeCalculation(
    val monthlyRent: Double,
    val securityDeposit: Double,
    val electricityRate: Double,
    val waterRate: Double,
    val monthlyUnitsElec: Double,
    val monthlyUnitsWater: Double
) {
    // Official PEA/MEA base rate estimate ~4.2 THB/unit
    // Official PWA/MWA base rate estimate ~12.0 THB/unit
    val officialElecRate: Double = 4.20
    val officialWaterRate: Double = 12.50

    val elecOverchargePerUnit: Double = (electricityRate - officialElecRate).coerceAtLeast(0.0)
    val waterOverchargePerUnit: Double = (waterRate - officialWaterRate).coerceAtLeast(0.0)

    val monthlyElecLoss: Double = elecOverchargePerUnit * monthlyUnitsElec
    val monthlyWaterLoss: Double = waterOverchargePerUnit * monthlyUnitsWater
    val annualOverchargeTotal: Double = (monthlyElecLoss + monthlyWaterLoss) * 12

    val isDepositOverLimit: Boolean = securityDeposit > monthlyRent
    val depositExcessAmount: Double = (securityDeposit - monthlyRent).coerceAtLeast(0.0)
}
