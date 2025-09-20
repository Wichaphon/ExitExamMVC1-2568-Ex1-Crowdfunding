package model;

//สถานะการสนับสนุนตามโจทย์ 
public enum PledgeStatus {
    SUCCESS,  //ผ่านvalidation ทั้งหมด คือยอดถูกบวก + quotaลด
    REJECT    //ไม่ผ่านกฎ (เช่น ต่ำกว่า minAmount, หมดquota, เลยdeadline)
}
