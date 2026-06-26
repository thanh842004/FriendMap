package com.example.friendmap;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

    private static final String TAG = "AddFriendActivity";

    private EditText edtSearchQuery;
    private Button btnSearch, btnSendRequest;
    private TextView txtSearchStatus, txtResultName, txtResultPhone;
    private CardView cardSearchResult;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private User foundUser = null;
    private String currentUserId = "USER_SAMPLE_123"; // Giá trị mặc định an toàn phòng hờ
    private String currentUserName = "Một người bạn";

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

        // FIX CHÍ MẠNG: Bọc kiểm tra FirebaseAuth an toàn, tránh lỗi NullPointerException khi lấy DisplayName
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            try {
                if (auth.getCurrentUser().getDisplayName() != null && !auth.getCurrentUser().getDisplayName().trim().isEmpty()) {
                    currentUserName = auth.getCurrentUser().getDisplayName();
                } else if (auth.getCurrentUser().getEmail() != null) {
                    currentUserName = auth.getCurrentUser().getEmail().split("@")[0];
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi phân tích dữ liệu User Auth: " + e.getMessage());
            }
        }

        // 3. Sự kiện bấm nút Tìm kiếm (Thêm kiểm tra null cho View)
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                if (edtSearchQuery == null) return;
                String query = edtSearchQuery.getText().toString().trim();
                if (query.isEmpty()) {
                    Toast.makeText(AddFriendActivity.this, "Vui lòng nhập thông tin tìm kiếm!", Toast.LENGTH_SHORT).show();
                    return;
                }
                searchUser(query);
            });
        }

        // 4. Sự kiện bấm nút Gửi lời mời kết bạn (Thêm kiểm tra null cho View)
        if (btnSendRequest != null) {
            btnSendRequest.setOnClickListener(v -> {
                if (foundUser != null) {
                    sendFriendRequest(foundUser);
                } else {
                    Toast.makeText(this, "Không có thông tin người dùng để kết bạn!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Logic tìm kiếm người dùng qua Username
    private void searchUser(String query) {
        if (txtSearchStatus != null) txtSearchStatus.setText("Đang tìm kiếm...");
        if (cardSearchResult != null) cardSearchResult.setVisibility(View.GONE);
        foundUser = null;

        db.collection("users")
                .whereEqualTo("username", query)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                User user = document.toObject(User.class);
                                if (user.getUid() == null) user.setUid(document.getId());

                                if (!user.getUid().equals(currentUserId)) {
                                    foundUser = user;
                                    displaySearchResult(user);
                                    return;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi ép kiểu dữ liệu User từ Firestore: ", e);
                            }
                        }
                    }
                    // Nếu không ra kết quả theo username, chuyển sang tìm số điện thoại
                    searchByPhone(query);
                });
    }

    // Logic tìm kiếm phụ qua Số điện thoại
    private void searchByPhone(String query) {
        db.collection("users")
                .whereEqualTo("phone", query)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                User user = document.toObject(User.class);
                                if (user.getUid() == null) user.setUid(document.getId());

                                if (!user.getUid().equals(currentUserId)) {
                                    foundUser = user;
                                    displaySearchResult(user);
                                    return;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi ép kiểu dữ liệu Phone: ", e);
                            }
                        }
                    }
                    // Nếu không có kết quả ở cả 2 trường
                    if (txtSearchStatus != null) txtSearchStatus.setText("Không tìm thấy người dùng nào phù hợp.");
                    if (cardSearchResult != null) cardSearchResult.setVisibility(View.GONE);
                });
    }

    // Hiển thị kết quả lên CardView
    private void displaySearchResult(User user) {
        if (txtSearchStatus != null) txtSearchStatus.setText("Đã tìm thấy kết quả:");
        if (txtResultName != null) txtResultName.setText(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
        if (txtResultPhone != null) txtResultPhone.setText(user.getPhone() != null ? user.getPhone() : "Chưa cập nhật SĐT");
        if (cardSearchResult != null) cardSearchResult.setVisibility(View.VISIBLE);
        if (btnSendRequest != null) {
            btnSendRequest.setText("Kết bạn");
            btnSendRequest.setEnabled(true);
        }
    }

    // Xử lý logic đẩy dữ liệu yêu cầu kết bạn lên Firestore
    private void sendFriendRequest(User targetUser) {
        if (targetUser.getUid() == null) {
            Toast.makeText(this, "Lỗi dữ liệu: Đối tượng không có UID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnSendRequest != null) {
            btnSendRequest.setEnabled(false);
            btnSendRequest.setText("Sending...");
        }

        db.collection("friendRequests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", targetUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        Toast.makeText(AddFriendActivity.this, "Bạn đã gửi lời mời tới người này rồi!", Toast.LENGTH_SHORT).show();
                        if (btnSendRequest != null) btnSendRequest.setText("Đã gửi");
                        return;
                    }

                    Map<String, Object> request = new HashMap<>();
                    request.put("senderId", currentUserId);
                    request.put("senderName", currentUserName);
                    request.put("receiverId", targetUser.getUid());
                    request.put("status", "pending");
                    request.put("timestamp", com.google.firebase.Timestamp.now());

                    db.collection("friendRequests")
                            .add(request)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(AddFriendActivity.this, "Gửi lời mời kết bạn thành công!", Toast.LENGTH_SHORT).show();
                                if (btnSendRequest != null) btnSendRequest.setText("Đã gửi");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi push dữ liệu lên Firestore: ", e);
                                Toast.makeText(AddFriendActivity.this, "Lỗi hệ thống, thử lại sau!", Toast.LENGTH_SHORT).show();
                                if (btnSendRequest != null) {
                                    btnSendRequest.setEnabled(true);
                                    btnSendRequest.setText("Kết bạn");
                                }
                            });
                });
    }
}