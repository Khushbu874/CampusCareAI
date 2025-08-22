package com.example.campuscareai;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class ChatMessage {
    private String text;
    private boolean isUser;
    private List<Uri> mediaUris;
    private List<String> mediaTypes; // "image" or "video"
    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.mediaUris = new ArrayList<>();
        this.mediaTypes = new ArrayList<>();
    }
    public ChatMessage(String text, boolean isUser, List<Uri> mediaUris, List<String> mediaTypes) {
        this.text = text == null ? "" : text;
        this.isUser = isUser;
        this.mediaUris = mediaUris != null ? mediaUris : new ArrayList<>();
        this.mediaTypes = mediaTypes != null ? mediaTypes : new ArrayList<>();
    }

    public String getText() { return text; }
    public boolean isUser() { return isUser; }
    public List<Uri> getMediaUris() { return mediaUris; }
    public List<String> getMediaTypes() { return mediaTypes; }

    public boolean hasMedia() { return mediaUris != null && !mediaUris.isEmpty(); }
}