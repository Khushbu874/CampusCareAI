package com.example.campuscareai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class AdminComplaintAdapter extends RecyclerView.Adapter<AdminComplaintAdapter.AdminComplaintViewHolder> {
    private List<Ai_complaint_activity.Complaint> complaintList;
    private Context context;

    public AdminComplaintAdapter(List<Ai_complaint_activity.Complaint> complaintList, Context context) {
        this.complaintList = complaintList;
        this.context = context;
    }

    @NonNull
    @Override
    public AdminComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_complaint_item, parent, false);
        return new AdminComplaintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminComplaintViewHolder holder, int position) {
        Ai_complaint_activity.Complaint complaint = complaintList.get(position);
        holder.tvComplaintText.setText(complaint.complaint_text);

        holder.tvComplaintStatus.setText("Status: " + complaint.status);
        holder.tvComplaintKey.setText("Enrollment: " + complaint.user_password + " | " + complaint.seriousness);
        holder.proofContainer.removeAllViews();

        if (complaint.proofUris != null && !complaint.proofUris.isEmpty()) {
            for (String uriStr : complaint.proofUris) {

                if (uriStr == null) continue;
                Uri uri = Uri.parse(uriStr); // ✅ Get MIME type safely
                String mimeType = context.getContentResolver().getType(uri);

                if (mimeType != null && mimeType.startsWith("image")) {// ---- IMAGE ----
                    ImageView img = new ImageView(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300, 300);
                    params.setMargins(8, 8, 8, 8);
                    img.setLayoutParams(params);
                    img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(context) .load(uri) .into(img);

                    img.setOnClickListener(v -> {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(uri, "image/*");
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(i);
                    });
                    holder.proofContainer.addView(img);
                } else if (mimeType != null && mimeType.startsWith("video")) { // ---- VIDEO ----
                    VideoView vid = new VideoView(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(400, 300);
                    params.setMargins(8, 8, 8, 8);
                    vid.setLayoutParams(params); vid.setVideoURI(uri);
                    vid.setMediaController(new MediaController(context));
                    vid.setOnPreparedListener(mp -> vid.seekTo(1)); // preview frame
                    holder.proofContainer.addView(vid);

                    vid.setOnClickListener(v -> {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(uri, "video/*");
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(i);
                    });
                } else { // fallback
                    TextView unknown = new TextView(context);
                    unknown.setText("Unsupported file: " + uriStr);
                    holder.proofContainer.addView(unknown);
                }
            }
        } else {
            TextView noProof = new TextView(context);
            noProof.setText("No proof uploaded.");
            holder.proofContainer.addView(noProof);
        }
    }

    @Override
    public int getItemCount() {
        return complaintList.size();
    }

    public static class AdminComplaintViewHolder extends RecyclerView.ViewHolder {
        TextView tvComplaintKey, tvComplaintText, tvComplaintStatus;
        LinearLayout proofContainer;
        public AdminComplaintViewHolder(@NonNull View itemView) {
            super(itemView);
            tvComplaintKey = itemView.findViewById(R.id.tvComplaintKey);
            tvComplaintText = itemView.findViewById(R.id.tvComplaintText);
            tvComplaintStatus = itemView.findViewById(R.id.tvComplaintStatus);
            proofContainer = itemView.findViewById(R.id.proofContainer);
        }
    }
}