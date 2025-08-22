package com.example.campuscareai;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import android.provider.MediaStore;
import android.graphics.ImageDecoder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Ai_complaint_activity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private static final int REQ_CODE_UPLOAD_MEDIA = 200;

    private EditText etAskAnything;
    private ImageButton btnSend, btnMic, btnUpload;

    private RecyclerView recyclerChat;
    private ChatAdapter adapter;
    private ArrayList<ChatMessage> messages;

    private HorizontalScrollView previewScroll;
    private LinearLayout previewContainer;

    private final List<Uri> pendingMediaUris = new ArrayList<>();
    private final List<String> pendingMediaTypes = new ArrayList<>();

    // Firebase
    private DatabaseReference dbRef;

    // Track chatbot state
    private enum ChatState { STEP1, COMPLAINT_WAIT_CONFIRM, COMPLAINT_WAIT_UPLOAD }
    private ChatState chatState = ChatState.STEP1;

    private String lastComplaintText = "";
    private String lastComplaintSeriousness = "minor";
    private String userName, userEnrollment;
    private int seriousSkipCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_complaint);

        userName = getIntent().getStringExtra("name");
        userEnrollment = getIntent().getStringExtra("enrollment");

        if (userName == null) userName = "unknown";
        if (userEnrollment == null) userEnrollment = "1234";

        etAskAnything   = findViewById(R.id.etAskAnything);
        btnSend         = findViewById(R.id.btnSend);
        btnMic          = findViewById(R.id.btnMic);
        btnUpload       = findViewById(R.id.btnUpload);
        recyclerChat    = findViewById(R.id.recyclerChat);

        previewScroll   = findViewById(R.id.previewScroll);
        previewContainer= findViewById(R.id.previewContainer);

        drawerLayout    = findViewById(R.id.drawerLayout);
        navigationView  = findViewById(R.id.navigationView);
        btnMenu         = findViewById(R.id.btnMenu);

        dbRef = FirebaseDatabase.getInstance().getReference("complaints");

        // Drawer menu
        btnMenu.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.openDrawer(GravityCompat.START);
            else drawerLayout.closeDrawer(GravityCompat.START);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent =new Intent(this, HomeActivity.class);
                intent.putExtra("name", userName);
                intent.putExtra("enrollment", userEnrollment);
                startActivity(intent);
            } else if (id == R.id.nav_chat) {
            } else if (id == R.id.nav_emergency) {
                Intent intent =new Intent(this, EmergencyActivity.class);
                intent.putExtra("name", userName);
                intent.putExtra("enrollment", userEnrollment);
                startActivity(intent);
            } else if (id == R.id.nav_status) {
                Intent intent =new Intent(this, TrackingActivity.class);
                intent.putExtra("name", userName);
                intent.putExtra("enrollment", userEnrollment);
                startActivity(intent);
            } else if (id == R.id.nav_admin) {
                if ("admin".equalsIgnoreCase(userName) && "SRUAC12345".equals(userEnrollment)) {
                    Intent intent = new Intent(this, AdminActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Access Denied! Only Admin can access this page.", Toast.LENGTH_SHORT).show();
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // RecyclerView
        messages = new ArrayList<>();
        adapter  = new ChatAdapter(messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerChat.setLayoutManager(lm);
        recyclerChat.setAdapter(adapter);

        // First AI message
        addMessage("Do you want to ask something about campus regarding querries(fee, library, hostel, etc.) or do you want to drop a complaint?", false);

        // Enable/disable send button
        updateSendEnabled();
        etAskAnything.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { updateSendEnabled(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> handleUserMessage());
        btnMic.setOnClickListener(v -> startVoiceInput());
        btnUpload.setOnClickListener(v -> openGallery());
    }

    private void handleUserMessage() {
        String userText = etAskAnything.getText().toString().trim();
        if (userText.isEmpty() && pendingMediaUris.isEmpty()) return;

        ChatMessage userMsg = new ChatMessage(userText.isEmpty() ? "" : userText, true,
                new ArrayList<>(pendingMediaUris), new ArrayList<>(pendingMediaTypes));

        addMessage(userMsg);
        etAskAnything.setText("");

        switch (chatState) {
            case STEP1:
                handleStep1(userText);
                break;

            case COMPLAINT_WAIT_CONFIRM:
                handleComplaintConfirmation(userText);
                break;

            case COMPLAINT_WAIT_UPLOAD:
                handleComplaintUpload(userText);
                break;
        }

        clearPreview();
    }

    // Step1: decide whether query / complaint / idle
    private void handleStep1(String input) {
        input = input.toLowerCase();

        List<String> queryWords = Arrays.asList("ask", "query", "question");
        List<String> complaintWords = Arrays.asList("drop complaint", "complaint", "issue", "problem", "emergency", "submit complaint");

        boolean isQuery = queryWords.stream().anyMatch(input::contains);
        boolean isComplaint = complaintWords.stream().anyMatch(input::contains);

        if (isQuery) {
            addMessage("Okay, please drop/ask your query.", false);
        } else if (isComplaint) {
            addMessage("Please write your complaint.", false);
            chatState = ChatState.COMPLAINT_WAIT_CONFIRM;
        } else {
            // Check for known query topics
            String reply = getBotReply(input);
            addMessage(reply, false);
            // Back to step1
            addMessage("Do you want to ask something about campus regarding querries(fee, library, hostel, etc.) or do you want to drop a complaint?", false);
        }
    }

    private void handleComplaintConfirmation(String input) {
        input = input.toLowerCase();
        lastComplaintText = input;
        lastComplaintSeriousness = isSeriousComplaint(input) ? "serious" : "minor";

        addMessage("Would you agree to submit your complaint to the admin? Your complaint is: " + input, false);
        chatState = ChatState.COMPLAINT_WAIT_UPLOAD;
    }

    private void handleComplaintUpload(String input) {
        input = input.toLowerCase();

        if (lastComplaintSeriousness.equals("serious")) {
            // For serious complaints, wait for proof
            if (!pendingMediaUris.isEmpty() || input.contains("skip")) {
                // Either proof uploaded or user decided to skip
                if (input.contains("skip")) seriousSkipCount++;

                saveComplaintToFirebase(); // submit complaint (with uploaded proof if exists)
                addMessage("Your serious complaint has been submitted to admin.", false);
                clearPreview();
                seriousSkipCount = 0; // reset skip counter
                resetChat();
            } else {
                // Ask user for proof, max 2 times
                if (seriousSkipCount < 2) {
                    addMessage("Please upload proof (image/video) for your serious complaint, or type 'skip' to submit without proof.", false);
                } else {
                    // User ignored twice, submit without proof
                    saveComplaintToFirebase();
                    addMessage("Your serious complaint has been submitted to admin without proof.", false);
                    clearPreview();
                    seriousSkipCount = 0; // reset counter
                    resetChat();
                }
            }
        } else {
            // Minor complaint directly
            saveComplaintToFirebase();
            addMessage("Your complaint has been submitted to admin.", false);
            resetChat();
        }
    }



    private void resetChat() {
        chatState = ChatState.STEP1;
        lastComplaintText = "";
        lastComplaintSeriousness = "minor";
        addMessage("Do you want to ask something or you want to drop?", false);
    }

    // --- Message helpers ---
    private void addMessage(String text, boolean isUser) {
        addMessage(new ChatMessage(text, isUser));
    }

    private void addMessage(ChatMessage message) {
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerChat.scrollToPosition(messages.size() - 1);
    }

    private boolean isSeriousComplaint(String input) {
        input = input.toLowerCase();
        return input.contains("emergency") || input.contains("harassment")
                || input.contains("danger") || input.contains("urgent") || input.contains("bullied") || input.contains("violence") || input.contains("abuse");
    }

    private boolean isYesReply(String input) {
        input = input.toLowerCase();
        return input.contains("yes") || input.contains("ok") || input.contains("okay") || input.contains("agree") || input.contains("yaah") || input.contains("ofcause");
    }

    private void saveComplaintToFirebase() {
        String key = dbRef.push().getKey();
        if (key != null) {
            // Append enrollment + seriousness to key
            String finalKey = key + "-" + userEnrollment + "-" + lastComplaintSeriousness;

            Complaint complaint = new Complaint(
                    userName,
                    userEnrollment,
                    lastComplaintText,
                    lastComplaintSeriousness,
                    "pending"
            );

            // Attach proof URIs if any
            if (!pendingMediaUris.isEmpty()) {
                List<String> proofUrls = new ArrayList<>();
                for (Uri uri : pendingMediaUris) proofUrls.add(uri.toString());
                complaint.proofUris = proofUrls;
            }

            // Save with the updated key
            dbRef.child(finalKey).setValue(complaint);
        }
    }



    private String getBotReply(String input) {
        if (input == null) return "Sorry, I didn't get that.";
        input = input.toLowerCase();

        if (input.contains("library") || input.contains("book")) return "Library is open from 9 AM to 7 PM. Contact librarian for late returns.";
        else if (input.contains("wifi") || input.contains("internet")) return "Wi-Fi issues: reconnect or report to IT (room 204).";
        else if (input.contains("exam") || input.contains("result") || input.contains("timetable")) return "Exams start on 15th December. Check portal for timetable.";
        else if (input.contains("fees") || input.contains("fee")) return "Semester fee is ₹25,000. Last date without fine: 30th Sep.";
        else if (input.contains("canteen") || input.contains("food")) return "Canteen complaints forwarded to committee.";
        else if (input.contains("hostel") || input.contains("room") || input.contains("warden")) return "Contact warden at ext 112 for hostel issues.";
        else if (input.contains("admission") || input.contains("apply")) return "Admission starts in June. Check website for dates.";
        else if (input.contains("holiday")) return "Next college holiday: 2nd October (Gandhi Jayanti).";
        else if (input.contains("media")) return "Received attachments. Say 'forward to admin' to send with complaint.";
        else return "Please ask about complaints-related queries or drop complaints to admin.";
    }

    // === Voice Input ===
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try { startActivityForResult(intent, REQ_CODE_SPEECH_INPUT); }
        catch (Exception e) { Toast.makeText(this, "Mic not supported", Toast.LENGTH_SHORT).show(); }
    }

    // === Gallery Upload ===
    private void openGallery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)  != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}, 1);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        startActivityForResult(intent, REQ_CODE_UPLOAD_MEDIA);
    }

    private void updateSendEnabled() {
        boolean hasText = etAskAnything.getText().toString().trim().length() > 0;
        boolean hasMedia = !pendingMediaUris.isEmpty();
        btnSend.setEnabled(hasText || hasMedia);
    }

    private void showPreview(List<Uri> uris, List<String> types) {
        pendingMediaUris.clear();
        pendingMediaTypes.clear();
        pendingMediaUris.addAll(uris);
        pendingMediaTypes.addAll(types);
        previewContainer.removeAllViews();
        previewScroll.setVisibility(View.VISIBLE);

        for (int i = 0; i < uris.size(); i++) {
            Uri uri = uris.get(i);
            String type = types.get(i);

            FrameLayout wrapper = new FrameLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(250, 250);
            lp.setMargins(10, 10, 10, 10);
            wrapper.setLayoutParams(lp);

            if ("image".equals(type)) {
                ImageView img = new ImageView(this);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) img.setImageBitmap(ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri)));
                    else img.setImageBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), uri));
                } catch (Exception e) { e.printStackTrace(); }
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                wrapper.addView(img);
            } else if ("video".equals(type)) {
                VideoView vid = new VideoView(this);
                vid.setVideoURI(uri);
                FrameLayout.LayoutParams vLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                vid.setLayoutParams(vLp);
                vid.setMediaController(new MediaController(this));
                wrapper.addView(vid);
                vid.setOnPreparedListener(mp -> { vid.pause(); try { vid.seekTo(1); } catch (Exception ignored) {} });
            }

            ImageButton btnRemove = new ImageButton(this);
            FrameLayout.LayoutParams rmLp = new FrameLayout.LayoutParams(80, 80);
            rmLp.rightMargin = 4; rmLp.topMargin = 4;
            btnRemove.setLayoutParams(rmLp);
            btnRemove.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnRemove.setBackgroundColor(0x00000000);
            final int index = i;
            btnRemove.setOnClickListener(v -> {
                if (index >= 0 && index < pendingMediaUris.size()) {
                    pendingMediaUris.remove(index);
                    pendingMediaTypes.remove(index);
                    showPreview(new ArrayList<>(pendingMediaUris), new ArrayList<>(pendingMediaTypes));
                }
            });
            wrapper.addView(btnRemove);
            previewContainer.addView(wrapper);
        }

        updateSendEnabled();
    }

    private void clearPreview() {
        previewScroll.setVisibility(View.GONE);
        previewContainer.removeAllViews();
        pendingMediaUris.clear();
        pendingMediaTypes.clear();
        updateSendEnabled();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQ_CODE_SPEECH_INPUT) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) etAskAnything.setText(result.get(0));

            } else if (requestCode == REQ_CODE_UPLOAD_MEDIA) {
                List<Uri> uris = new ArrayList<>();
                List<String> types = new ArrayList<>();

                if (data.getClipData() != null) {
                    ClipData clipData = data.getClipData();
                    int count = Math.min(clipData.getItemCount(), 5);
                    for (int i = 0; i < count; i++) {
                        Uri uri = clipData.getItemAt(i).getUri();
                        String mime = getContentResolver().getType(uri);
                        if (mime != null) {
                            uris.add(uri);
                            if (mime.startsWith("image")) types.add("image");
                            else if (mime.startsWith("video")) types.add("video");

                            // ✅ Persist URI permission
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                );
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    String mime = getContentResolver().getType(uri);
                    if (mime != null) {
                        uris.add(uri);
                        if (mime.startsWith("image")) types.add("image");
                        else if (mime.startsWith("video")) types.add("video");

                        // ✅ Persist URI permission
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            );
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (!uris.isEmpty()) showPreview(uris, types);
                else Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Complaint object for Firebase
    public static class Complaint {
        public String user_id, user_password, complaint_text, seriousness, status;
        public List<String> proofUris; // Add this field

        public Complaint() {}
        public Complaint(String user_id, String user_password, String complaint_text, String seriousness, String status) {
            this.user_id = user_id;
            this.user_password = user_password;
            this.complaint_text = complaint_text;
            this.seriousness = seriousness;
            this.status = status;
            this.proofUris = new ArrayList<>();
        }
    }
}
