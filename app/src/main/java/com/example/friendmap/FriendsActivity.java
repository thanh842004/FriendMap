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

        rvFriendRequests = findViewById(R.id.rvFriendRequests);
        rvFriendsList = findViewById(R.id.rvFriendsList);
        btnGoToAddFriend = findViewById(R.id.btnGoToAddFriend);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "USER_SAMPLE_123";

        if (rvFriendRequests != null) {
            rvFriendRequests.setLayoutManager(new LinearLayoutManager(this));
            requestList = new ArrayList<>();
            try {
                requestAdapter = new FriendRequestAdapter(requestList);
                rvFriendRequests.setAdapter(requestAdapter);
            } catch (NoSuchMethodError | Exception e) {
                Log.e(TAG, "Adapter lỗi: " + e.getMessage());
            }
        }

        if (rvFriendsList != null) {
            rvFriendsList.setLayoutManager(new LinearLayoutManager(this));
            friendsList = new ArrayList<>();
            try {
                friendsAdapter = new FriendsAdapter(friendsList);
                rvFriendsList.setAdapter(friendsAdapter);
            } catch (NoSuchMethodError | Exception e) {
                Log.e(TAG, "Adapter lỗi: " + e.getMessage());
            }
        }

        listenToFriendRequests();
        listenToFriendsList();

        if (btnGoToAddFriend != null) {
            btnGoToAddFriend.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(FriendsActivity.this, AddFriendActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Không thể mở AddFriendActivity: " + e.getMessage());
                    Toast.makeText(FriendsActivity.this, "Lỗi hệ thống!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh adapter khi quay lại từ ChatActivity → badge cập nhật lại
        if (friendsAdapter != null) {
            friendsAdapter.notifyDataSetChanged();
        }
    }

    private void listenToFriendRequests() {
        db.collection("friendRequests")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe lời mời: ", error);
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
                            Log.e(TAG, "Lỗi ép kiểu FriendRequest: ", e);
                        }
                    }
                    if (requestAdapter != null) {
                        requestAdapter.notifyDataSetChanged();
                    }
                });
    }

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
                                            if (user != null && !friendsList.contains(user)) {
                                                friendsList.add(user);
                                            }
                                            if (friendsAdapter != null) {
                                                friendsAdapter.notifyDataSetChanged();
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Lỗi load User: ", e);
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