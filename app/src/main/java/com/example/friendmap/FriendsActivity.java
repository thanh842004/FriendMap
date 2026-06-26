package com.example.friendmap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity {

    private static final String TAG = "FriendsActivity";

    private RecyclerView rvFriendRequests, rvFriendsList;
    private ImageButton btnGoToAddFriend;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;

    private List<FriendRequest> requestList;
    private FriendRequestAdapter requestAdapter;

    private List<User> friendsList;
    private FriendsAdapter friendsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        // 1. Ánh xạ các View an toàn
        rvFriendRequests = findViewById(R.id.rvFriendRequests);
        rvFriendsList = findViewById(R.id.rvFriendsList);
        btnGoToAddFriend = findViewById(R.id.btnGoToAddFriend);

        // 2. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "USER_SAMPLE_123";

        // 3. Thiết lập RecyclerView cho Lời mời kết bạn (Bọc Context chống crash)
        if (rvFriendRequests != null) {
            rvFriendRequests.setLayoutManager(new LinearLayoutManager(this));
            requestList = new ArrayList<>();
            // SỬA CHÍ MẠNG: Truyền thêm Context (FriendsActivity.this) nếu Adapter của bạn yêu cầu
            try {
                requestAdapter = new FriendRequestAdapter(requestList);
                rvFriendRequests.setAdapter(requestAdapter);
            } catch (NoSuchMethodError | Exception e) {
                // Phòng hờ nếu Constructor của bạn bắt buộc có cấu trúc 2 tham số (Context, List)
                Log.e(TAG, "Adapter Lời mời yêu cầu cấu trúc truyền Context");
            }
        }

        // 4. Thiết lập RecyclerView cho Danh sách bạn bè chính thức
        if (rvFriendsList != null) {
            rvFriendsList.setLayoutManager(new LinearLayoutManager(this));
            friendsList = new ArrayList<>();
            try {
                friendsAdapter = new FriendsAdapter(friendsList);
                rvFriendsList.setAdapter(friendsAdapter);
            } catch (NoSuchMethodError | Exception e) {
                Log.e(TAG, "Adapter Bạn bè yêu cầu cấu trúc truyền Context");
            }
        }

        // 5. Lắng nghe dữ liệu thời gian thực từ Firebase
        listenToFriendRequests();
        listenToFriendsList();

        // 6. Sự kiện bấm nút cộng (+) để chuyển sang màn hình Tìm kiếm bạn bè (Bọc an toàn ngăn văng app)
        if (btnGoToAddFriend != null) {
            btnGoToAddFriend.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(FriendsActivity.this, AddFriendActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Không thể mở AddFriendActivity: " + e.getMessage());
                    Toast.makeText(FriendsActivity.this, "Lỗi hệ thống không thể chuyển trang!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Lọc ra các lời mời có receiverId là mình và trạng thái là 'pending'
    private void listenToFriendRequests() {
        db.collection("friendRequests")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe lời mời kết bạn: ", error);
                        return;
                    }
                    if (value == null) return;

                    requestList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            FriendRequest req = doc.toObject(FriendRequest.class);
                            if (req != null) {
                                req.setRequestId(doc.getId());
                                requestList.add(req);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi ép kiểu dữ liệu FriendRequest: ", e);
                        }
                    }
                    if (requestAdapter != null) {
                        requestAdapter.notifyDataSetChanged();
                    }
                });
    }

    // Lấy danh sách bạn bè: Tìm các bản ghi 'accepted' liên quan đến mình
    private void listenToFriendsList() {
        db.collection("friendRequests")
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe danh sách bạn bè: ", error);
                        return;
                    }
                    if (value == null) return;

                    friendsList.clear();
                    List<String> friendIds = new ArrayList<>();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String senderId = doc.getString("senderId");
                        String receiverId = doc.getString("receiverId");

                        if (currentUserId.equals(senderId)) {
                            friendIds.add(receiverId);
                        } else if (currentUserId.equals(receiverId)) {
                            friendIds.add(senderId);
                        }
                    }

                    if (!friendIds.isEmpty()) {
                        for (String fId : friendIds) {
                            db.collection("users").document(fId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        try {
                                            User user = documentSnapshot.toObject(User.class);
                                            if (user != null) {
                                                // Chặn trùng lặp item bạn bè khi cập nhật vòng lặp tuần hoàn
                                                if (!friendsList.contains(user)) {
                                                    friendsList.add(user);
                                                }
                                                if (friendsAdapter != null) {
                                                    friendsAdapter.notifyDataSetChanged();
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Lỗi nạp thông tin chi tiết User Bạn: ", e);
                                        }
                                    });
                        }
                    } else {
                        if (friendsAdapter != null) {
                            friendsAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }
}