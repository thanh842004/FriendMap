package com.example.friendmap;

public class FriendRequest {
    private String requestId; // ID của document trên Firestore để sau này update trạng thái
    private String senderId;
    private String senderName;
    private String receiverId;
    private String status;

    public FriendRequest() {}

    public FriendRequest(String requestId, String senderId, String senderName, String receiverId, String status) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.status = status;
    }

    // Getter và Setter
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}