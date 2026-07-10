package com.example.friendmap;

public class User {
    private String uid;
    private String username;
    private String displayName;
    private String phone;
    private String email;
    private double latitude;
    private double longitude;
    private String avatarBase64;

    // BỔ SUNG CHÍ MẠNG: Đồng bộ chính xác với Firestore và trạng thái Online/Offline
    private String hoTen;
    private boolean isOnline;

    // Hàm khởi tạo không đối số bắt buộc phải có để Firestore tự động ép kiểu (Mapping)
    public User() {}

    public User(String uid, String username, String displayName,
                String phone, String email,
                double latitude, double longitude,
                String hoTen,
                boolean isOnline,
                String avatarBase64) {

        this.uid = uid;
        this.username = username;
        this.displayName = displayName;
        this.phone = phone;
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hoTen = hoTen;
        this.isOnline = isOnline;
        this.avatarBase64 = avatarBase64;
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

    // BỔ SUNG GETTER / SETTER CHO CÁC BIẾN MỚI
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    public String getAvatarBase64() {
        return avatarBase64;
    }
    public void setAvatarBase64(String avatarBase64) {
        this.avatarBase64 = avatarBase64;
    }

    // Hàm so sánh trùng lặp bọc an toàn cho RecyclerView ngăn trùng item bạn bè khi cập nhật vòng lặp
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return uid != null ? uid.equals(user.uid) : user.uid == null;
    }

    @Override
    public int hashCode() {
        return uid != null ? uid.hashCode() : 0;
    }
}