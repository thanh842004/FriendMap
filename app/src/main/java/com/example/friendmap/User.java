package com.example.friendmap;

public class User {
    private String uid;
    private String username;
    private String displayName;
    private String phone;
    private String email;
    private double latitude;
    private double longitude;

    // Hàm khởi tạo không đối số bắt buộc phải có để Firestore tự động ép kiểu (Mapping)
    public User() {}

    public User(String uid, String username, String displayName, String phone, String email, double latitude, double longitude) {
        this.uid = uid;
        this.username = username;
        this.displayName = displayName;
        this.phone = phone;
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getter và Setter
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}