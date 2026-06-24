package com.example.friendmap;

import android.os.Bundle;
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

    // Danh sách 8 Emoji cố định dùng để trêu chọc bạn bè
    private final String[] emojiList = {"🔥", "😂", "👻", "💩", "🎉", "🤡", "👍", "❤️"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. Lấy thông tin người bạn được truyền sang từ danh sách bạn bè
        partnerId = getIntent().getStringExtra("PARTNER_ID");
        partnerName = getIntent().getStringExtra("PARTNER_NAME");

        // Cấu hình ID kiểm thử nếu dev độc lập
        if (partnerId == null) partnerId = "PARTNER_SAMPLE_456";
        if (partnerName == null) partnerName = "Người Bạn";

        // 2. Ánh xạ các View từ file layout
        txtChatPartnerName = findViewById(R.id.txtChatPartnerName);
        rvMessages = findViewById(R.id.rvMessages);
        edtMessageInput = findViewById(R.id.edtMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnOpenEmojiPicker = findViewById(R.id.btnOpenEmojiPicker);
        emojiPickerContainer = findViewById(R.id.emojiPickerContainer);

        txtChatPartnerName.setText(partnerName);

        // 3. Khởi tạo Firebase & Tạo mã phòng chat chung độc nhất giữa 2 người
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentUserId = "USER_SAMPLE_123";
        }
        chatRoomId = generateChatRoomId(currentUserId, partnerId);

        // 4. Cấu hình RecyclerView để hiển thị danh sách tin nhắn
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        rvMessages.setAdapter(messageAdapter);

        // 5. Bắt đầu lắng nghe tin nhắn thời gian thực
        listenForMessages();

        // 6. Xử lý gửi tin nhắn chữ thường
        btnSendMessage.setOnClickListener(v -> {
            String text = edtMessageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text, "");
            }
        });

        // 7. Xử lý đóng/mở khay chọn nhanh Emoji trêu chọc
        btnOpenEmojiPicker.setOnClickListener(v -> {
            if (emojiPickerContainer.getVisibility() == View.GONE) {
                setupEmojiGridView();
                emojiPickerContainer.setVisibility(View.VISIBLE);
            } else {
                emojiPickerContainer.setVisibility(View.GONE);
            }
        });
    }

    // Gộp và sắp xếp ID để đảm bảo 2 người luôn vào đúng 1 phòng chat chung vĩnh viễn
    private String generateChatRoomId(String id1, String id2) {
        List<String> ids = new ArrayList<>();
        ids.add(id1);
        ids.add(id2);
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }

    // Tải dữ liệu tin nhắn liên tục và cuộn mượt xuống cuối khi có tin mới
    private void listenForMessages() {
        db.collection("messages")
                .whereEqualTo("chatRoomId", chatRoomId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    messageList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null) {
                            messageList.add(msg);
                        }
                    }
                    messageAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        rvMessages.smoothScrollToPosition(messageList.size() - 1);
                    }
                });
    }

    // Đẩy cấu trúc dữ liệu tin nhắn hoặc emoji trêu chọc lên Firestore
    private void sendMessage(String text, String emoji) {
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("chatRoomId", chatRoomId);
        msgMap.put("senderId", currentUserId);
        msgMap.put("text", text);
        msgMap.put("emojiTease", emoji);
        msgMap.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        edtMessageInput.setText("");
        emojiPickerContainer.setVisibility(View.GONE);

        db.collection("messages").add(msgMap)
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Gửi tin nhắn thất bại!", Toast.LENGTH_SHORT).show());
    }

    // Gắn dữ liệu Emoji vào GridView và bắt sự kiện click gửi nhanh
    private void setupEmojiGridView() {
        gvEmojiPicker = findViewById(R.id.gvEmojiPicker);
        if (gvEmojiPicker == null) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, emojiList);
        gvEmojiPicker.setAdapter(adapter);

        gvEmojiPicker.setOnItemClickListener((parent, view, position, id) -> {
            String selectedEmoji = emojiList[position];
            sendMessage("", selectedEmoji); // Tin nhắn trêu chọc sẽ để trống trường text
        });
    }
}