package com.example.friendmap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity_Debug";
    private TextView txtChatPartnerName;
    private RecyclerView rvMessages;
    private EditText edtMessageInput;
    private Button btnSendMessage;
    private ImageButton btnOpenEmojiPicker;
    private FrameLayout emojiPickerContainer;
    private GridView gvEmojiPicker;

    private FirebaseFirestore db;
    private String currentUserId;
    private String partnerId;
    private String partnerName;
    private String chatRoomId;

    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    private final String[] emojiList = {"🔥", "😂", "👻", "💩", "🎉", "🤡", "👍", "❤️"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. Lấy thông tin người bạn được truyền sang từ FriendsAdapter
        partnerId = getIntent().getStringExtra("PARTNER_ID");
        partnerName = getIntent().getStringExtra("PARTNER_NAME");

        // ĐỒNG BỘ CHÍ MẠNG: Chặn đứng phòng chat ma PARTNER_SAMPLE_456 nếu Intents bị truyền thiếu
        if (partnerId == null || partnerId.trim().isEmpty() || partnerId.equals("PARTNER_SAMPLE_456")) {
            Toast.makeText(this, "Lỗi dữ liệu: Không xác định được ID bạn bè!", Toast.LENGTH_SHORT).show();
            finish(); // Đóng ngay màn hình chat, ép phải chọn lại từ danh sách bạn bè để lấy ID thật
            return;
        }

        // 2. Ánh xạ các View từ file layout
        txtChatPartnerName = findViewById(R.id.txtChatPartnerName);
        rvMessages = findViewById(R.id.rvMessages);
        edtMessageInput = findViewById(R.id.edtMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnOpenEmojiPicker = findViewById(R.id.btnOpenEmojiPicker);
        emojiPickerContainer = findViewById(R.id.emojiPickerContainer);

        // ĐỒNG BỘ HIỂN THỊ TÊN CHÍ MẠNG: Đọc trực tiếp cả hoTen từ Firestore đề phòng danh sách bên kia truyền thiếu dữ liệu
        if (partnerName == null || partnerName.isEmpty() || partnerName.equals("Người Bạn")) {
            FirebaseFirestore.getInstance().collection("users").document(partnerId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String hoTen = doc.getString("hoTen");
                            String displayName = doc.getString("displayName");
                            String username = doc.getString("username");

                            String finalName = "Người Bạn";
                            if (hoTen != null && !hoTen.isEmpty()) finalName = hoTen;
                            else if (displayName != null && !displayName.isEmpty()) finalName = displayName;
                            else if (username != null && !username.isEmpty()) finalName = username;

                            txtChatPartnerName.setText(finalName);
                        } else {
                            txtChatPartnerName.setText("Bạn bè");
                        }
                    })
                    .addOnFailureListener(e -> txtChatPartnerName.setText("Bạn bè"));
        } else {
            txtChatPartnerName.setText(partnerName);
        }

        // TỐI ƯU THỨ TỰ CHÍ MẠNG: Khởi tạo Firebase & Lấy ID người dùng trước để có dữ liệu truyền cho Adapter
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentUserId = "USER_SAMPLE_123";
        }
        chatRoomId = generateChatRoomId(currentUserId, partnerId);

        // 4. Cấu hình RecyclerView để hiển thị danh sách tin nhắn (ĐÃ CẬP NHẬT TRUYỀN 2 THAM SỐ)
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId); // Sửa dứt điểm lỗi "cannot be applied to given types"
        rvMessages.setAdapter(messageAdapter);

        // 5. Bắt đầu lắng nghe tin nhắn thời gian thực
        listenForMessages();

        // 6. Xử lý gửi tin nhắn chữ thường
        if (btnSendMessage != null) {
            btnSendMessage.setOnClickListener(v -> {
                String text = edtMessageInput.getText().toString().trim();
                if (!text.isEmpty()) {
                    sendMessage(text, "");
                }
            });
        }

        // 7. Xử lý đóng/mở khay chọn nhanh Emoji trêu chọc
        if (btnOpenEmojiPicker != null) {
            btnOpenEmojiPicker.setOnClickListener(v -> {
                if (emojiPickerContainer != null) {
                    if (emojiPickerContainer.getVisibility() == View.GONE) {
                        setupEmojiGridView();
                        emojiPickerContainer.setVisibility(View.VISIBLE);
                    } else {
                        emojiPickerContainer.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private String generateChatRoomId(String id1, String id2) {
        List<String> ids = new ArrayList<>();
        ids.add(id1);
        ids.add(id2);
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }

    private void listenForMessages() {
        db.collection("messages")
                .whereEqualTo("chatRoomId", chatRoomId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe tin nhắn: " + error.getMessage());
                        return;
                    }
                    if (value == null) return;

                    messageList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            Message msg = doc.toObject(Message.class);
                            if (msg != null) {
                                messageList.add(msg);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi ép kiểu tin nhắn: " + e.getMessage());
                        }
                    }
                    if (messageAdapter != null) {
                        messageAdapter.notifyDataSetChanged();
                    }
                    if (!messageList.isEmpty() && rvMessages != null) {
                        rvMessages.smoothScrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage(String text, String emoji) {
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("chatRoomId", chatRoomId);
        msgMap.put("senderId", currentUserId);
        msgMap.put("text", text);
        msgMap.put("emojiTease", emoji);
        msgMap.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        if (edtMessageInput != null) edtMessageInput.setText("");
        if (emojiPickerContainer != null) emojiPickerContainer.setVisibility(View.GONE);

        db.collection("messages").add(msgMap)
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Gửi tin nhắn thất bại!", Toast.LENGTH_SHORT).show());
    }

    private void setupEmojiGridView() {
        gvEmojiPicker = findViewById(R.id.gvEmojiPicker);
        if (gvEmojiPicker == null) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, emojiList);
        gvEmojiPicker.setAdapter(adapter);

        gvEmojiPicker.setOnItemClickListener((parent, view, position, id) -> {
            String selectedEmoji = emojiList[position];
            sendMessage("", selectedEmoji);
        });
    }
}