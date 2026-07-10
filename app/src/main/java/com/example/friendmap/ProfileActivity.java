package com.example.friendmap;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    TextView tvName, tvUsername, tvPhone, tvEmail;
    ImageButton btnBack, btnEdit, btnChangeAvatar;
    ImageView ivAvatar;
    Button btnLogout;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String currentUid;
    String currentName = "", currentPhone = "";

    ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) uploadAvatarBase64(uri);
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUid = mAuth.getCurrentUser().getUid();

        tvName = findViewById(R.id.tvName);
        tvUsername = findViewById(R.id.tvUsername);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnLogout = findViewById(R.id.btnLogout);
        ivAvatar = findViewById(R.id.ivAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);

        loadProfile();

        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> showEditDialog());

        btnChangeAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Đăng xuất")
                    .setMessage("Bạn có chắc muốn đăng xuất không?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        mAuth.signOut();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void loadProfile() {
        db.collection("users").document(currentUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        currentName = document.getString("displayName") != null
                                ? document.getString("displayName") : "";
                        currentPhone = document.getString("phone") != null
                                ? document.getString("phone") : "";

                        tvName.setText(currentName);
                        tvUsername.setText("@" + document.getString("username"));
                        tvPhone.setText(currentPhone);
                        tvEmail.setText(document.getString("email"));

                        // Load ảnh Base64 nếu có
                        String avatarBase64 = document.getString("avatarBase64");
                        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                            loadImageFromBase64(avatarBase64);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show());
    }

    private void showEditDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        EditText etName = dialogView.findViewById(R.id.etEditName);
        EditText etPhone = dialogView.findViewById(R.id.etEditPhone);

        etName.setText(currentName);
        etPhone.setText(currentPhone);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Tên không được để trống!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateProfile(newName, newPhone);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateProfile(String newName, String newPhone) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", newName);
        updates.put("hoTen", newName);
        updates.put("phone", newPhone);

        db.collection("users").document(currentUid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    currentName = newName;
                    currentPhone = newPhone;
                    tvName.setText(newName);
                    tvPhone.setText(newPhone);
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void uploadAvatarBase64(Uri uri) {
        Toast.makeText(this, "Đang xử lý ảnh...", Toast.LENGTH_SHORT).show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize ảnh xuống 200x200 để tránh quá nặng
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true);

            // Convert sang Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // Lưu lên Firestore
            db.collection("users").document(currentUid)
                    .update("avatarBase64", base64)
                    .addOnSuccessListener(unused -> {
                        loadImageFromBase64(base64);
                        Toast.makeText(this, "Đổi ảnh thành công!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadImageFromBase64(String base64) {
        try {
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            ivAvatar.setImageBitmap(bitmap);
            ivAvatar.clearColorFilter(); // Xóa tint xanh mặc định
            ivAvatar.setPadding(0, 0, 0, 0); // Xóa padding để ảnh hiện full
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi hiển thị ảnh", Toast.LENGTH_SHORT).show();
        }
    }
}