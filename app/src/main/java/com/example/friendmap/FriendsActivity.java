package com.example.friendmap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity {

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

        // 1. Ánh xạ các View
        rvFriendRequests = findViewById(R.id.rvFriendRequests);
        rvFriendsList = findViewById(R.id.rvFriendsList);
        btnGoToAddFriend = findViewById(R.id.btnGoToAddFriend);

        // 2. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "USER_SAMPLE_123";

        // 3. Thiết lập RecyclerView cho Lời mời kết bạn
        rvFriendRequests.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();
        requestAdapter = new FriendRequestAdapter(requestList);
        rvFriendRequests.setAdapter(requestAdapter);

        // 4. Thiết lập RecyclerView cho Danh sách bạn bè chính thức
        rvFriendsList.setLayoutManager(new LinearLayoutManager(this));
        friendsList = new ArrayList<>();
        friendsAdapter = new FriendsAdapter(friendsList);
        rvFriendsList.setAdapter(friendsAdapter);

        // 5. Lắng nghe dữ liệu thời gian thực
        listenToFriendRequests();
        listenToFriendsList();

        // 6. Sự kiện bấm nút cộng (+) để chuyển sang màn hình Tìm kiếm bạn bè
        btnGoToAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(FriendsActivity.this, AddFriendActivity.class);
            startActivity(intent);
        });
    }

    // Lọc ra các lời mời có receiverId là mình và trạng thái là 'pending'
    private void listenToFriendRequests() {
        db.collection("friendRequests")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    requestList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        FriendRequest req = doc.toObject(FriendRequest.class);
                        if (req != null) {
                            req.setRequestId(doc.getId()); // Lưu lại document ID để cập nhật khi bấm nút
                            requestList.add(req);
                        }
                    }
                    requestAdapter.notifyDataSetChanged();
                });
    }

    // Lấy danh sách bạn bè: Tìm các bản ghi 'accepted' liên quan đến mình
    private void listenToFriendsList() {
        db.collection("friendRequests")
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    friendsList.clear();
                    List<String> friendIds = new ArrayList<>();

                    // Duyệt xem bản ghi nào chứa ID của mình để lấy ID của người bạn kia
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String senderId = doc.getString("senderId");
                        String receiverId = doc.getString("receiverId");

                        if (currentUserId.equals(senderId)) {
                            friendIds.add(receiverId);
                        } else if (currentUserId.equals(receiverId)) {
                            friendIds.add(senderId);
                        }
                    }

                    // Từ danh sách ID thu được, truy vấn bảng 'users' để lấy thông tin chi tiết hiển thị
                    if (!friendIds.isEmpty()) {
                        for (String fId : friendIds) {
                            db.collection("users").document(fId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        User user = documentSnapshot.toObject(User.class);
                                        if (user != null) {
                                            friendsList.add(user);
                                            friendsAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    } else {
                        friendsAdapter.notifyDataSetChanged();
                    }
                });
    }
}