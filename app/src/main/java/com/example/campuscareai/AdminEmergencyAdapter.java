package com.example.campuscareai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdminEmergencyAdapter extends RecyclerView.Adapter<AdminEmergencyAdapter.EmergencyViewHolder> {

    private List<EmergencySession> emergencyList;
    private Context context;

    public AdminEmergencyAdapter(List<EmergencySession> emergencyList, Context context) {
        this.emergencyList = emergencyList;
        this.context = context;
    }

    @NonNull
    @Override
    public EmergencyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_admin_emergency_item, parent, false);
        return new EmergencyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmergencyViewHolder holder, int position) {
        EmergencySession session = emergencyList.get(position);

        holder.tvEnrollment.setText("Enrollment: " + session.userId);
        holder.tvLocation.setText("Live Location: Lat=" + session.latitude + ", Lng=" + session.longitude);
        holder.tvStatus.setText("Status: " + session.status);
    }

    @Override
    public int getItemCount() {
        return emergencyList.size();
    }

    public static class EmergencyViewHolder extends RecyclerView.ViewHolder {
        TextView tvEnrollment, tvLocation, tvStatus;

        public EmergencyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEnrollment = itemView.findViewById(R.id.tvEmergencyEnrollment);
            tvLocation = itemView.findViewById(R.id.tvEmergencyLocation);
            tvStatus = itemView.findViewById(R.id.tvEmergencyStatus);
        }
    }
}
