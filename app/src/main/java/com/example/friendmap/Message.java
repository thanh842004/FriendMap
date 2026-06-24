package com.example.friendmap;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    private String senderId;
    private String text;
    @ServerTimestamp
    private Date timestamp;
    private String emojiTease;

    // Hàm khởi tạo không đối số bắt buộc
    public Message() {}

    public Message(String senderId, String text, String emojiTease) {
        this.senderId = senderId;
        this.text = text;
        this.emojiTease = emojiTease;
    }

    // Getter và Setter
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getEmojiTease() { return emojiTease; }
    public void setEmojiTease(String emojiTease) { this.emojiTease = emojiTease; }
}