package com.example.friendmap;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    // ĐÃ THÊM: Bổ sung chatRoomId để khớp hoàn toàn với Firestore, sửa lỗi ẩn tin nhắn
    private String chatRoomId;
    private String senderId;
    private String text;
    private String emojiTease;
    private boolean isRead;

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    @ServerTimestamp
    private Date timestamp;

    // Hàm khởi tạo không đối số bắt buộc cho Firebase
    public Message() {}

    public Message(String chatRoomId, String senderId, String text, String emojiTease) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.text = text;
        this.emojiTease = emojiTease;
    }

    // Getter và Setter đầy đủ
    public String getChatRoomId() { return chatRoomId; }
    public void setChatRoomId(String chatRoomId) { this.chatRoomId = chatRoomId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getEmojiTease() { return emojiTease; }
    public void setEmojiTease(String emojiTease) { this.emojiTease = emojiTease; }
}