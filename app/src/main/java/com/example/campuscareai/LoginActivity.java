package com.example.campuscareai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    EditText loginUsername, loginPassword;
    Button loginButton;
    TextView signupRedirectText, guest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginUsername = findViewById(R.id.login_username);
        loginPassword = findViewById(R.id.login_password); // enrollment number
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirect);
        guest = findViewById(R.id.guest);

        // ✅ Guest Login
        guest.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        // ✅ Login Button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredName = loginUsername.getText().toString().trim();
                String enteredEnrollment = loginPassword.getText().toString().trim();

                if (enteredName.isEmpty() || enteredEnrollment.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");

                // ⚡ Search user by enrollment
                reference.child(enteredEnrollment).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String dbName = snapshot.child("name").getValue(String.class);
                            String dbEnrollment = snapshot.child("enrollment").getValue(String.class);

                            if (dbName != null && dbEnrollment != null &&
                                    dbName.equals(enteredName) && dbEnrollment.equals(enteredEnrollment)) {

                                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                                // ✅ Pass Name & Enrollment to HomeActivity
                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                intent.putExtra("name", dbName);
                                intent.putExtra("enrollment", dbEnrollment);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Invalid Name or Password", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // ✅ Redirect to Signup
        signupRedirectText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
        });
    }
}
