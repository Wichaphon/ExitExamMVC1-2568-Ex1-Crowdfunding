package model;

import java.time.LocalDate;

/**
 * เก็บข้อมูลโครงการที่เปิดให้สนับสนุน*/

public class Project {
    private String id;          //รหัสโครงการ 8 หลัก ตัวแรกไม่ใช่ 0
    private String name;        //ชื่อโครงการ
    private double goal;        //เป้าหมายการระดมทุน
    private double raised;      //ยอดที่ระดมได้ปัจจุบัน
    private LocalDate deadline; //วันสิ้นสุดโครงการ
    private String category;    //หมวดหมู่ 

    public Project(String id, String name, double goal, LocalDate deadline, String category) {
        this.id = id;
        this.name = name;
        this.goal = goal;
        this.raised = 0; //เริ่มต้นที่ 0
        this.deadline = deadline;
        this.category = category;
    }

    //Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getGoal() { return goal; }
    public double getRaised() { return raised; }
    public LocalDate getDeadline() { return deadline; }
    public String getCategory() { return category; }

    //Setters
    public void addRaised(double amount) { this.raised += amount; }

    @Override
    public String toString() {
        return String.format("%s (%s) - %.2f/%.2f THB, deadline: %s",
                name, category, raised, goal, deadline.toString());
    }
}
