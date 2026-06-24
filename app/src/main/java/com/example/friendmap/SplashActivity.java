package com.example.friendmap;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Đã đăng nhập → vào màn hình chính
                startActivity(new Intent(this, MainActivity.class));
            } else {
                // Chưa đăng nhập → vào màn hình đăng nhập
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        }, 2000); // Hiện splash 2 giây
    }
}