package com.example.campuscareai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private Button btnAi, btnAlert, btnTrack, btnAdmin;
    private ImageButton btnProfile;

    String passedName, passedEnrollment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Get extras from LoginActivity
        passedName = getIntent().getStringExtra("name");
        passedEnrollment = getIntent().getStringExtra("enrollment");

        // Initialize UI components
        btnAi = findViewById(R.id.btn_ai_chatbot);
        btnAlert = findViewById(R.id.btn_emergency);
        btnTrack = findViewById(R.id.btn_track_status);
        btnProfile = findViewById(R.id.btn_profile);
        btnAdmin = findViewById(R.id.admin_btn);

        // Profile button click
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfilePageActivity.class);
            // Forward enrollment to ProfilePageActivity
            intent.putExtra("enrollment", passedEnrollment);
            startActivity(intent);
        });

        // AI Chatbot button click
        btnAi.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(passedName) && !TextUtils.isEmpty(passedEnrollment)) {
                Intent intent = new Intent(HomeActivity.this, Ai_complaint_activity.class);
                // Pass login name and enrollment/password to AI Chatbot
                intent.putExtra("name", passedName);
                intent.putExtra("enrollment", passedEnrollment);
                startActivity(intent);
            } else {
                Toast.makeText(HomeActivity.this, "Please login to access this page.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Emergency button click
        btnAlert.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(passedName) && !TextUtils.isEmpty(passedEnrollment)) {
                Intent intent = new Intent(HomeActivity.this, EmergencyActivity.class);
                // Pass login name and enrollment/password to AI Chatbot
                intent.putExtra("name", passedName);
                intent.putExtra("enrollment", passedEnrollment);
                startActivity(intent);
            } else {
                Toast.makeText(HomeActivity.this, "Please login to access this page.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Track Status button click
        btnTrack.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TrackingActivity.class);
            intent.putExtra("name", passedName);
            intent.putExtra("enrollment", passedEnrollment);
            startActivity(intent);
        });

        // Admin button click
        btnAdmin.setOnClickListener(v -> {
            if ("admin".equalsIgnoreCase(passedName) && "SRUAC12345".equals(passedEnrollment)) {
                Intent intent = new Intent(HomeActivity.this, AdminActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(HomeActivity.this, "Access Denied! Only Admin can access this page.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
