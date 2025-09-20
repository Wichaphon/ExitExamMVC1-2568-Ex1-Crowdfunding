package model;

/**
 *RewardTier 
 *ระดับของรางวัลที่ผูกกับโครงการ (Project)
 *Unique กับ Projectด้วย (projectId, tierName)
 *
 *Fields
 *projectId : รหัสโครงการ (8 หลัก ตัวแรกไม่ใช่ 0) — ตรวจใน Validation
 *tierName  : ชื่อ tier
 *minAmount : ยอดสนับสนุนขั้นต่ำที่ต้องถึงเพื่อรับ tier นี้
 *quota     : จำนวนสิทธิ์คงเหลือ
 */
public class RewardTier {

    private final String projectId;
    private final String tierName;
    private final double minAmount;
    private int quota;

    public RewardTier(String projectId, String tierName, double minAmount, int quota) {
        this.projectId = projectId;
        this.tierName = tierName;
        this.minAmount = minAmount;
        this.quota = quota;
    }

    //Getters
    public String getProjectId() { return projectId; }
    public String getTierName() { return tierName; }
    public double getMinAmount() { return minAmount; }
    public int getQuota() { return quota; }

    //ลด quota ลง 1 ถ้ามีการสนับสนุนใน tier นี้ 
    public void consumeOneQuota() {
        if (quota > 0) quota--;
    }

    //ใช้เช็คเร็วๆ เวลา validate ว่ามีสิทธิ์ให้รับไหม
    public boolean hasQuota() { return quota > 0; }

    //equals กับ /hashCode อิงตัว (projectId, tierName) ให้ไม่ซ้ำในโครงการเดียวกัน
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RewardTier)) return false;
        RewardTier other = (RewardTier) o;
        return projectId.equals(other.projectId) && tierName.equals(other.tierName);
    }

    @Override
    public int hashCode() {
        int result = projectId.hashCode();
        result = 31 * result + tierName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return tierName + " (min " + minAmount + ", quota " + quota + ")";
    }
}
