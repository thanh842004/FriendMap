package com.example.friendmap;

import android.os.Bundle;
import android.util.Log; // Thêm import này để dùng Log.d

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Thêm import thư viện Firebase
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Đoạn xử lý EdgeToEdge an toàn: Nếu không tìm thấy R.id.main thì sẽ áp dụng thẳng lên cửa sổ chính
        if (findViewById(R.id.main) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // ============================================================
        // 🔐 BƯỚC 9: KHỞI TẠO VÀ KIỂM TRA KẾT NỐI FIREBASE
        // ============================================================
        try {
            FirebaseApp.initializeApp(this);
            Log.d("FirebaseCheck", "Firebase connected: " + FirebaseApp.getInstance().getName());
        } catch (Exception e) {
            Log.e("FirebaseCheck", "Lỗi kết nối Firebase: " + e.getMessage());
        }
        // ============================================================
    }
}