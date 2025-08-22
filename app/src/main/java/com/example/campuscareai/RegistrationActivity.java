package com.example.campuscareai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegistrationActivity extends AppCompatActivity {

    EditText signupName, signupEmail, signupEnrollment, signupPhone;
    Spinner signupDepartment, signupCourse, signupBranch, signupYear;
    Button signupButton;
    TextView loginRedirectText;

    FirebaseDatabase database;
    DatabaseReference reference;

    // Dropdown data
    private final String[] departments = {"Select Department", "Engineering", "Management", "Pharmacy", "Science"};
    private final String[] courses = {"Select Course", "B.Tech", "MBA", "B.Pharm", "B.Sc"};
    private final String[] branches = {"Select Branch", "CSE", "IT", "ECE", "EEE", "Mechanical", "Civil"};
    private final String[] years = {"Select Year", "1st Year", "2nd Year", "3rd Year", "4th Year"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Correct layout file name (your XML)
        setContentView(R.layout.activity_registration);

        // Bind XML views
        signupName = findViewById(R.id.signup_name);
        signupEmail = findViewById(R.id.signup_email);
        signupEnrollment = findViewById(R.id.signup_Enrollment);
        signupPhone = findViewById(R.id.signup_phone);

        signupDepartment = findViewById(R.id.signup_Department);
        signupCourse = findViewById(R.id.signup_Course);
        signupBranch = findViewById(R.id.signup_Branch);
        signupYear = findViewById(R.id.signup_Year);

        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        // Setup spinners
        setSpinnerAdapter(signupDepartment, departments);
        setSpinnerAdapter(signupCourse, courses);
        setSpinnerAdapter(signupBranch, branches);
        setSpinnerAdapter(signupYear, years);

        // Signup button click
        signupButton.setOnClickListener(v -> {
            String name = signupName.getText().toString().trim();
            String email = signupEmail.getText().toString().trim();
            String enrollment = signupEnrollment.getText().toString().trim();
            String phone = signupPhone.getText().toString().trim();

            String department = signupDepartment.getSelectedItem().toString();
            String course = signupCourse.getSelectedItem().toString();
            String branch = signupBranch.getSelectedItem().toString();
            String year = signupYear.getSelectedItem().toString();

            if (name.isEmpty() || email.isEmpty() || enrollment.isEmpty() || phone.isEmpty()
                    || department.equals("Select Department") || course.equals("Select Course")
                    || branch.equals("Select Branch") || year.equals("Select Year")) {
                Toast.makeText(RegistrationActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            database = FirebaseDatabase.getInstance();
            reference = database.getReference("users");

            HelperClass helperClass = new HelperClass(name, email, enrollment, phone, department, course, branch, year);
            reference.child(enrollment).setValue(helperClass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegistrationActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(RegistrationActivity.this, "Signup failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Redirect to login
        loginRedirectText.setOnClickListener(v -> {
            startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
        });
    }

    private void setSpinnerAdapter(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
}
