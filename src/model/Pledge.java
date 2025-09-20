package model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 *Pledge 
 *เก็บรายการสนับสนุนของผู้ใช้ต่อโครงการ
 *
 * Fields:
 *pledgeId   : ไอดีรายการ, unique
 *userId     : ผู้สนับสนุน
 *projectId  : โครงการที่สนับสนุน (8 หลัก ตาม validation)
 *amount     : จำนวนเงินที่สนับสนุน
 *tierName   : ชื่อรางวัล (อาจเป็น null ได้ )
 *status     : SUCCESS หรือ REJECT
 *createdAt  : วันเวลาที่ทำ
 */

public class Pledge {
    private final String pledgeId;
    private final String userId;
    private final String projectId;
    private final double amount;
    private final String tierName; // nullable
    private final PledgeStatus status;
    private final LocalDateTime createdAt;

    public Pledge(String pledgeId, String userId, String projectId,
                  double amount, String tierName,
                  PledgeStatus status, LocalDateTime createdAt) {
        this.pledgeId = pledgeId;
        this.userId = userId;
        this.projectId = projectId;
        this.amount = amount;
        this.tierName = tierName;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters
    public String getPledgeId()    { return pledgeId; }
    public String getUserId()      { return userId; }
    public String getProjectId()   { return projectId; }
    public double getAmount()      { return amount; }
    public String getTierName()    { return tierName; }
    public PledgeStatus getStatus(){ return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return String.format("%s -> %s : %.2f (%s) [%s]",
                userId, projectId, amount,
                (tierName == null ? "-" : tierName),
                status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pledge)) return false;
        Pledge other = (Pledge) o;
        return Objects.equals(pledgeId, other.pledgeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pledgeId);
    }
}
