package com.example.campuscareai;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class TrackingActivity extends AppCompatActivity {

    private RecyclerView recyclerComplaints;
    private ComplaintAdapter adapter;
    private List<Ai_complaint_activity.Complaint> complaintList;

    private DatabaseReference dbRef;
    private String userEnrollment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        recyclerComplaints = findViewById(R.id.recyclerComplaints);
        recyclerComplaints.setLayoutManager(new LinearLayoutManager(this));

        complaintList = new ArrayList<>();
        adapter = new ComplaintAdapter(complaintList, this);
        recyclerComplaints.setAdapter(adapter);

        userEnrollment = getIntent().getStringExtra("enrollment");
        if (userEnrollment == null) userEnrollment = "1234";

        dbRef = FirebaseDatabase.getInstance().getReference("complaints");

        fetchComplaints();
    }

    private void fetchComplaints() {
        dbRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                complaintList.clear();
                for (DataSnapshot ds : task.getResult().getChildren()) {
                    Ai_complaint_activity.Complaint complaint =
                            ds.getValue(Ai_complaint_activity.Complaint.class);
                    if (complaint != null) {
                        String key = ds.getKey();
                        if (key != null && key.contains(userEnrollment)) {
                            complaintList.add(complaint);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                if (complaintList.isEmpty()) {
                    Toast.makeText(this, "No complaints found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Failed to load complaints.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
