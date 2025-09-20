package model;

import java.time.LocalDate;

/**
 * รวมการตรวจสอบข้อมูล ผมใช้ซ้ำจาก Controller
 */
public class Validation {

    /**รหัสโครงการต้องเป็นเลข 8 หลักและตัวแรกไม่ใช่ 0 */
    public boolean validProjectId(String id) {
        return id != null && id.matches("^[1-9][0-9]{7}$");
    }

    /**เป้าหมายต้องมากกว่า 0 */
    public boolean positiveGoal(double goal) {
        return goal > 0.0d;
    }

    /**deadline อย่างน้อยต้องวันพรุ่งนี้ */
    public boolean isFutureDeadline(LocalDate d) {
        return d != null && d.isAfter(LocalDate.now());
    }

    /**จำนวนเงินสนับสนุนต้องมากกว่าหรือเท่ากับขั้นต่ำของ tier*/
    public boolean meetsMinAmount(double amount, RewardTier tier) {
        if (tier == null) return amount > 0.0d; // เคสไม่เลือกรางวัล ขอเป็นบวกเฉย ๆ
        return amount >= tier.getMinAmount();
    }

    /**เช็ค quota ว่ายังเหลือไหม */
    public boolean hasQuota(RewardTier tier) {
        return tier == null || tier.hasQuota();
    }
}
