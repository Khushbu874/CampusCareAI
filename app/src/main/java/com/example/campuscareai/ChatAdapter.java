package com.example.campuscareai;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.MediaController;
import android.media.MediaPlayer;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final ArrayList<ChatMessage> messages;
    private static final int TYPE_USER = 0;
    private static final int TYPE_BOT = 1;

    public ChatAdapter(ArrayList<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        return msg.isUser() ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if (viewType == TYPE_USER) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_user, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_bot, parent, false);
        }
        return new ChatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        Context ctx = holder.itemView.getContext();

        // Text
        if (msg.getText() != null && !msg.getText().isEmpty()) {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText(msg.getText());
        } else {
            holder.tvMessage.setVisibility(View.GONE);
        }

        // Clear old media views
        holder.mediaContainer.removeAllViews();

        // Add media views if any
        if (msg.hasMedia()) {
            for (int i = 0; i < msg.getMediaUris().size(); i++) {
                Uri uri = msg.getMediaUris().get(i);
                String type = msg.getMediaTypes().get(i);

                if ("image".equals(type)) {
                    ImageView iv = new ImageView(ctx);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            dpToPx(ctx, 160), dpToPx(ctx, 160));
                    lp.setMargins(dpToPx(ctx, 4), dpToPx(ctx, 4), dpToPx(ctx, 4), dpToPx(ctx, 4));
                    iv.setLayoutParams(lp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setImageURI(uri);
                    holder.mediaContainer.addView(iv);
                } else if ("video".equals(type)) {
                    VideoView vv = new VideoView(ctx);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            dpToPx(ctx, 200), dpToPx(ctx, 140));
                    lp.setMargins(dpToPx(ctx, 4), dpToPx(ctx, 4), dpToPx(ctx, 4), dpToPx(ctx, 4));
                    vv.setLayoutParams(lp);
                    vv.setVideoURI(uri);

                    MediaController mc = new MediaController(ctx);
                    mc.setAnchorView(vv);
                    vv.setMediaController(mc);

                    vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            vv.pause();
                            try { vv.seekTo(1); } catch (Exception ignored) {}
                        }
                    });

                    holder.mediaContainer.addView(vv);
                }
            }
            holder.mediaContainer.setVisibility(View.VISIBLE);
        } else {
            holder.mediaContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        LinearLayout mediaContainer;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(itemView.getContext().getResources().getIdentifier(
                    "txtUserMessage", "id", itemView.getContext().getPackageName()));
            if (tvMessage == null) {
                tvMessage = itemView.findViewById(itemView.getContext().getResources().getIdentifier(
                        "txtBotMessage", "id", itemView.getContext().getPackageName()));
            }
            mediaContainer = itemView.findViewById(R.id.mediaContainer);
            if (mediaContainer == null) {
                mediaContainer = new LinearLayout(itemView.getContext());
                mediaContainer.setOrientation(LinearLayout.VERTICAL);
            }
        }
    }

    private static int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
