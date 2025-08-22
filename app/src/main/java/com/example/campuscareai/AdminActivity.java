package com.example.campuscareai;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;

    private AdminComplaintAdapter complaintAdapter;
    private AdminEmergencyAdapter emergencyAdapter;

    private List<Ai_complaint_activity.Complaint> complaintList = new ArrayList<>();
    private List<EmergencySession> emergencyList = new ArrayList<>();

    private DatabaseReference complaintsRef, emergencyRef;

    private String selectedTab = "Minor"; // default tab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        tabLayout = findViewById(R.id.tabFilter);
        recyclerView = findViewById(R.id.allRecyclerComplaints);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        complaintsRef = FirebaseDatabase.getInstance().getReference("complaints");
        emergencyRef = FirebaseDatabase.getInstance().getReference("emergency_sessions");

        // ✅ Tabs
        tabLayout.addTab(tabLayout.newTab().setText("Minor"));
        tabLayout.addTab(tabLayout.newTab().setText("Major"));
        tabLayout.addTab(tabLayout.newTab().setText("Emergency"));

        // ✅ Default load complaints
        loadComplaints();

        // ✅ Handle tab selection
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getText().toString();
                if (selectedTab.equals("Emergency")) {
                    loadEmergencies();
                } else {
                    loadComplaints();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadComplaints() {
        complaintsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                complaintList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Ai_complaint_activity.Complaint complaint = snap.getValue(Ai_complaint_activity.Complaint.class);
                    if (complaint != null) {
                        // ✅ Filter based on seriousness
                        if (selectedTab.equals("Minor") && "Minor".equalsIgnoreCase(complaint.seriousness)) {
                            complaintList.add(complaint);
                        } else if (selectedTab.equals("Major") &&
                                ("Major".equalsIgnoreCase(complaint.seriousness) ||
                                        "Serious".equalsIgnoreCase(complaint.seriousness))) {
                            complaintList.add(complaint);
                        }
                    }
                }
                complaintAdapter = new AdminComplaintAdapter(complaintList, AdminActivity.this);
                recyclerView.setAdapter(complaintAdapter);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Failed to load complaints", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEmergencies() {
        emergencyRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                emergencyList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    EmergencySession session = snap.getValue(EmergencySession.class);
                    if (session != null) {
                        // ✅ Get last location if available
                        DataSnapshot locationsNode = snap.child("locations");
                        if (locationsNode.exists()) {
                            DataSnapshot lastLoc = null;
                            for (DataSnapshot loc : locationsNode.getChildren()) {
                                lastLoc = loc;
                            }
                            if (lastLoc != null) {
                                Double lat = lastLoc.child("latitude").getValue(Double.class);
                                Double lng = lastLoc.child("longitude").getValue(Double.class);
                                if (lat != null && lng != null) {
                                    session.latitude = lat;
                                    session.longitude = lng;
                                }
                            }
                        }
                        emergencyList.add(session);
                    }
                }
                emergencyAdapter = new AdminEmergencyAdapter(emergencyList, AdminActivity.this);
                recyclerView.setAdapter(emergencyAdapter);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Failed to load emergencies", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
