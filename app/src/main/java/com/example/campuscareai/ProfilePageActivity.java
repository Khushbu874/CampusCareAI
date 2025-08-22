package com.example.campuscareai;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

public class ProfilePageActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    ImageView profileImage;
    Button changePhotoButton;
    TextView name, email, enrollment, department, course, branch, year, phone;
    Uri imageUri;  // To hold selected image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        profileImage = findViewById(R.id.profile_image);
        name = findViewById(R.id.profile_name);
        email = findViewById(R.id.profile_email);
        enrollment = findViewById(R.id.profile_Enrollment);
        department = findViewById(R.id.profile_Department);
        course = findViewById(R.id.profile_course);
        branch = findViewById(R.id.profile_Branch);
        year = findViewById(R.id.profile_year);
        phone = findViewById(R.id.profile_phone);

        // ✅ Get enrollment from intent
        String passedEnrollment = getIntent().getStringExtra("enrollment");
        if (passedEnrollment == null || passedEnrollment.isEmpty()) {
            Toast.makeText(this, "Enrollment not received", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Fetch user data from Firebase
        DatabaseReference reference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(passedEnrollment);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    name.setText("Name: " + snapshot.child("name").getValue(String.class));
                    email.setText("Email: " + snapshot.child("email").getValue(String.class));
                    enrollment.setText("Enrollment No.: " + snapshot.child("enrollment").getValue(String.class));
                    department.setText("Department: " + snapshot.child("department").getValue(String.class));
                    course.setText("Course: " + snapshot.child("course").getValue(String.class));
                    branch.setText("Branch: " + snapshot.child("branch").getValue(String.class));
                    year.setText("Year: " + snapshot.child("year").getValue(String.class));
                    phone.setText("Phone: " + snapshot.child("phone").getValue(String.class));
                } else {
                    Toast.makeText(ProfilePageActivity.this, "Data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfilePageActivity.this, "Firebase Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ Handle image select
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }
}
