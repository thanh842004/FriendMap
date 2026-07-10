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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

    // ====== THÊM MỚI ======
    private String myAvatar = "";
    private String partnerAvatar = "";
    // ======================

    private List<Message> messageList;
    private MessageAdapter messageAdapter;

    private final String[] emojiList = {
            "🔥","😂","👻","💩","🎉","🤡","👍","❤️"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        partnerId = getIntent().getStringExtra("PARTNER_ID");
        partnerName = getIntent().getStringExtra("PARTNER_NAME");

        if (partnerId == null || partnerId.trim().isEmpty()) {
            Toast.makeText(this,
                    "Không xác định được bạn bè",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtChatPartnerName = findViewById(R.id.txtChatPartnerName);
        rvMessages = findViewById(R.id.rvMessages);
        edtMessageInput = findViewById(R.id.edtMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnOpenEmojiPicker = findViewById(R.id.btnOpenEmojiPicker);
        emojiPickerContainer = findViewById(R.id.emojiPickerContainer);

        db = FirebaseFirestore.getInstance();

        currentUserId = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        chatRoomId = generateChatRoomId(currentUserId, partnerId);

        txtChatPartnerName.setText(partnerName);

        messageList = new ArrayList<>();

        rvMessages.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // ==========================
        // LẤY AVATAR CỦA MÌNH
        // ==========================

        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        String avatar =
                                documentSnapshot.getString("avatarBase64");

                        if (avatar != null)
                            myAvatar = avatar;
                    }

                    loadPartnerAvatar();

                });

        btnSendMessage.setOnClickListener(v -> {

            String text =
                    edtMessageInput.getText()
                            .toString()
                            .trim();

            if (!text.isEmpty()) {

                sendMessage(text, "");

            }

        });

        btnOpenEmojiPicker.setOnClickListener(v -> {

            if (emojiPickerContainer.getVisibility()
                    == View.GONE) {

                setupEmojiGridView();

                emojiPickerContainer.setVisibility(View.VISIBLE);

            } else {

                emojiPickerContainer.setVisibility(View.GONE);

            }

        });

    }

    //=============================
    // LOAD AVATAR NGƯỜI BÊN KIA
    //=============================

    private void loadPartnerAvatar() {

        db.collection("users")
                .document(partnerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        String avatar =
                                documentSnapshot.getString("avatarBase64");

                        if (avatar != null)
                            partnerAvatar = avatar;
                    }

                    // TẠO ADAPTER SAU KHI ĐÃ CÓ 2 AVATAR

                    messageAdapter =
                            new MessageAdapter(
                                    messageList,
                                    currentUserId,
                                    myAvatar,
                                    partnerAvatar
                            );

                    rvMessages.setAdapter(messageAdapter);

                    listenForMessages();

                });

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
                .addSnapshotListener(
                        MetadataChanges.INCLUDE,
                        (value, error) -> {

                            if (error != null) {
                                Log.e(TAG, error.getMessage());
                                return;
                            }

                            if (value == null) return;

                            messageList.clear();

                            for (DocumentSnapshot doc : value.getDocuments()) {

                                Message message = doc.toObject(Message.class);

                                if (message != null) {
                                    messageList.add(message);
                                }

                            }

                            messageAdapter.notifyDataSetChanged();

                            if (!messageList.isEmpty()) {

                                rvMessages.scrollToPosition(
                                        messageList.size() - 1
                                );

                            }

                        });

    }

    private void sendMessage(String text, String emoji) {

        HashMap<String, Object> map = new HashMap<>();

        map.put("chatRoomId", chatRoomId);
        map.put("senderId", currentUserId);
        map.put("text", text);
        map.put("emojiTease", emoji);
        map.put("timestamp", FieldValue.serverTimestamp());

        db.collection("messages")
                .add(map)
                .addOnSuccessListener(unused -> {

                    edtMessageInput.setText("");

                    emojiPickerContainer.setVisibility(View.GONE);

                })
                .addOnFailureListener(e ->

                        Toast.makeText(
                                ChatActivity.this,
                                "Không gửi được tin nhắn",
                                Toast.LENGTH_SHORT
                        ).show()

                );

    }

    private void setupEmojiGridView() {

        gvEmojiPicker = findViewById(R.id.gvEmojiPicker);

        if (gvEmojiPicker == null) return;

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        emojiList
                );

        gvEmojiPicker.setAdapter(adapter);

        gvEmojiPicker.setOnItemClickListener((parent, view, position, id) -> {

            sendMessage(
                    "",
                    emojiList[position]
            );

        });

    }

}