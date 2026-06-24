package com.example.friendmap;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class AddFriendActivity extends AppCompatActivity {

    private EditText edtSearchQuery;
    private Button btnSearch, btnSendRequest;
    private TextView txtSearchStatus, txtResultName, txtResultPhone;
    private CardView cardSearchResult;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private User foundUser = null; // Lưu trữ thông tin người tìm thấy
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        // 1. Ánh xạ các thành phần UI
        edtSearchQuery = findViewById(R.id.edtSearchQuery);
        btnSearch = findViewById(R.id.btnSearch);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        txtSearchStatus = findViewById(R.id.txtSearchStatus);
        txtResultName = findViewById(R.id.txtResultName);
        txtResultPhone = findViewById(R.id.txtResultPhone);
        cardSearchResult = findViewById(R.id.cardSearchResult);

        // 2. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            // Chuỗi ID giả lập phục vụ quá trình phát triển nếu chưa chạy qua màn hình Login
            currentUserId = "USER_SAMPLE_123";
        }

        // 3. Sự kiện bấm nút Tìm kiếm
        btnSearch.setOnClickListener(v -> {
            String query = edtSearchQuery.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(AddFriendActivity.this, "Vui lòng nhập thông tin tìm kiếm!", Toast.LENGTH_SHORT).show();
                return;
            }
            searchUser(query);
        });

        // 4. Sự kiện bấm nút Gửi lời mời kết bạn
        btnSendRequest.setOnClickListener(v -> {
            if (foundUser != null) {
                sendFriendRequest(foundUser);
            }
        });
    }

    // Logic tìm kiếm người dùng qua Username hoặc Số điện thoại
    private void searchUser(String query) {
        txtSearchStatus.setText("Searching...");
        cardSearchResult.setVisibility(View.GONE);
        foundUser = null;

        // Tiến hành tìm kiếm theo trường 'username' trước
        db.collection("users")
                .whereEqualTo("username", query)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            // Không được tự tìm thấy chính mình
                            if (!user.getUid().equals(currentUserId)) {
                                foundUser = user;
                                displaySearchResult(user);
                                return;
                            }
                        }
                    }

                    // Nếu tìm theo username không thấy, tiếp tục tìm theo trường 'phone'
                    if (foundUser == null) {
                        searchByPhone(query);
                    }
                });
    }

    private void searchByPhone(String query) {
        db.collection("users")
                .whereEqualTo("phone", query)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            if (!user.getUid().equals(currentUserId)) {
                                foundUser = user;
                                displaySearchResult(user);
                                return;
                            }
                        }
                    }
                    // Nếu cả 2 trường đều không có kết quả
                    txtSearchStatus.setText("Không tìm thấy người dùng nào phù hợp.");
                    cardSearchResult.setVisibility(View.GONE);
                });
    }

    // Hiển thị kết quả tìm kiếm lên CardView
    private void displaySearchResult(User user) {
        txtSearchStatus.setText("Đã tìm thấy kết quả:");
        txtResultName.setText(user.getDisplayName());
        txtResultPhone.setText(user.getPhone());
        cardSearchResult.setVisibility(View.VISIBLE);
        btnSendRequest.setText("Kết bạn");
        btnSendRequest.setEnabled(true);
    }

    // Xử lý logic đẩy dữ liệu yêu cầu kết bạn lên Firestore
    private void sendFriendRequest(User targetUser) {
        btnSendRequest.setEnabled(false);
        btnSendRequest.setText("Sending...");

        // Kiểm tra xem lời mời kết bạn đã tồn tại trước đó hay chưa để tránh trùng lặp
        db.collection("friendRequests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", targetUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Toast.makeText(AddFriendActivity.this, "Bạn đã gửi lời mời tới người này rồi!", Toast.LENGTH_SHORT).show();
                        btnSendRequest.setText("Đã gửi");
                        return;
                    }

                    // Tiến hành tạo bản ghi yêu cầu mới
                    Map<String, Object> request = new HashMap<>();
                    request.put("senderId", currentUserId);
                    request.put("senderName", auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : "Một người bạn");
                    request.put("receiverId", targetUser.getUid());
                    request.put("status", "pending");

                    db.collection("friendRequests")
                            .add(request)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(AddFriendActivity.this, "Gửi lời mời kết bạn thành công!", Toast.LENGTH_SHORT).show();
                                btnSendRequest.setText("Đã gửi");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AddFriendActivity.this, "Lỗi hệ thống, thử lại sau!", Toast.LENGTH_SHORT).show();
                                btnSendRequest.setEnabled(true);
                                btnSendRequest.setText("Kết bạn");
                            });
                });
    }
}