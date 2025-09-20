package model;

/**
 *User ไว้ mock ตัวlogin เฉยๆ)
 *เพื่อความง่ายขอเก็บ password เป็น plain-text
 */

public class User {
    private final String userId;      
    private final String username;    //สำหรับล็อกอิน
    private final String displayName; //ชื่อที่ใช้แสดงผล
    private final String password;    //mock รหัสผ่าน

    public User(String userId, String username, String displayName, String password) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.password = password;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getPassword() { return password; }

    @Override
    public String toString() {
        return displayName + " (" + username + ")";
    }
}
