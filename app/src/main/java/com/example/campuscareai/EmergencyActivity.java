package com.example.campuscareai;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmergencyActivity extends AppCompatActivity {

    private static final String TAG = "EmergencyActivity";
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private boolean isFrontCamera = false;

    private TextView textResult;
    private Button switchCameraBtn;
    private ImageButton micButton;

    private String userName, userEnrollment;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private String currentSessionId;

    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;
    private Recorder recorder;

    private DatabaseReference firebaseDatabaseReference;
    private StorageReference firebaseStorageReference;

    private static final int REQUEST_CODE_PERMISSIONS = 201;
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    private boolean isRecording = false;
    private boolean isAudioEnabledInVideo = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        userName = getIntent().getStringExtra("name");
        userEnrollment = getIntent().getStringExtra("enrollment");

        previewView = findViewById(R.id.previewView);
        textResult = findViewById(R.id.textResult);
        switchCameraBtn = findViewById(R.id.switchCamera);
        micButton = findViewById(R.id.micButton);

        cameraExecutor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference("emergency_sessions");
        firebaseStorageReference = FirebaseStorage.getInstance().getReference();

        micButton.setImageResource(R.drawable.ic_record_start);
        textResult.setText("Tap to Record");

        if (checkAndRequestPermissions()) {
            setupCameraAndLocation();
        }

        micButton.setOnClickListener(v -> toggleRecording());

        switchCameraBtn.setOnClickListener(v -> {
            if (isRecording) {
                Toast.makeText(this, "Stop recording before switching camera.", Toast.LENGTH_SHORT).show();
                return;
            }
            isFrontCamera = !isFrontCamera;
            startCamera();
        });
    }

    private void setupCameraAndLocation() {
        startCamera();
        createLocationRequest();
        startLocationUpdates();
    }

    private boolean checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (Build.VERSION.SDK_INT <= 28 &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(isFrontCamera ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera start failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleRecording() {
        if (videoCapture == null) {
            Toast.makeText(this, "Camera not ready.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isRecording && currentRecording != null) {
            stopVideoRecording();
        } else {
            startVideoRecording();
        }
    }

    @SuppressLint("MissingPermission")
    private void startVideoRecording() {
        currentSessionId = firebaseDatabaseReference.push().getKey();
        if (currentSessionId == null) {
            Toast.makeText(this, "Failed to create session ID", Toast.LENGTH_SHORT).show();
            return;
        }

        isRecording = true;
        micButton.setImageResource(R.drawable.ic_record_stop);
        textResult.setText("Recording...");

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", userEnrollment != null ? userEnrollment : "unknown_user");
        sessionData.put("userName", userName != null ? userName : "Unknown User");
        sessionData.put("startTime", System.currentTimeMillis());
        sessionData.put("status", "recording");
//        sessionData.put("videoUrl", "");

        firebaseDatabaseReference.child(currentSessionId).setValue(sessionData);

        String name = "emergency_video_" + currentSessionId + "_" +
                new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CampusCareEmergency");
        }

        MediaStoreOutputOptions mediaStoreOutputOptions = new MediaStoreOutputOptions
                .Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();

        PendingRecording pendingRecording = videoCapture.getOutput().prepareRecording(this, mediaStoreOutputOptions);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && isAudioEnabledInVideo) {
            pendingRecording.withAudioEnabled();
        }

        currentRecording = pendingRecording.start(ContextCompat.getMainExecutor(this), recordEventListener);
    }

    private final Consumer<VideoRecordEvent> recordEventListener = event -> {
        if (event instanceof VideoRecordEvent.Finalize) {
            VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
            if (!finalizeEvent.hasError()) {
                Uri outputUri = finalizeEvent.getOutputResults().getOutputUri();
                firebaseDatabaseReference.child(currentSessionId).child("status").setValue("processing_upload");

                File tempFile = copyUriToCache(outputUri, currentSessionId + ".mp4");
                if (tempFile != null) {
                    uploadVideoToFirebase(Uri.fromFile(tempFile), tempFile);
                } else {
                    runOnUiThread(() -> textResult.setText("Video copy failed."));
                }

            } else {
                firebaseDatabaseReference.child(currentSessionId).child("status")
                        .setValue("failed_recording_" + errorToString(finalizeEvent.getError()));
            }
            currentRecording = null;
            isRecording = false;
            micButton.setImageResource(R.drawable.ic_record_start);
            textResult.setText("Tap Mic to Record");
        }
    };

    private String errorToString(int errorCode) {
        switch (errorCode) {
            case VideoRecordEvent.Finalize.ERROR_NONE: return "NONE";
            case VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED: return "ENCODING_FAILED";
            case VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED: return "FILE_SIZE_LIMIT";
            case VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE: return "NO_STORAGE";
            case VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR: return "RECORDER_ERROR";
            default: return "UNKNOWN_" + errorCode;
        }
    }

    private void stopVideoRecording() {
        if (currentRecording != null) {
            currentRecording.stop();
        }
    }

    private void uploadVideoToFirebase(Uri fileUri, File tempFile) {
        if (fileUri == null || currentSessionId == null) return;

        runOnUiThread(() -> textResult.setText("Uploading video..."));

        StorageReference videoFileRef = firebaseStorageReference
                .child("emergency_videos")
                .child(currentSessionId + ".mp4");

        UploadTask task = videoFileRef.putFile(fileUri);

        task.addOnProgressListener(snapshot -> {
            long bytes = snapshot.getBytesTransferred();
            long total = snapshot.getTotalByteCount();
            int pct = (int) Math.round((100.0 * bytes) / Math.max(1, total));
            runOnUiThread(() -> textResult.setText("Uploading... " + pct + "%"));
        }).addOnSuccessListener(taskSnapshot -> {
            videoFileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                Map<String, Object> updates = new HashMap<>();
                updates.put("videoUrl", downloadUri.toString());
                updates.put("status", "completed_uploaded");
                updates.put("endTime", System.currentTimeMillis());

                firebaseDatabaseReference.child(currentSessionId)
                        .updateChildren(updates)
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> textResult.setText("Video Uploaded.")))
                        .addOnFailureListener(e -> runOnUiThread(() -> textResult.setText("DB update failed: " + e.getMessage())));

                if (tempFile != null && tempFile.exists()) tempFile.delete();
            });
        }).addOnFailureListener(e -> {
            firebaseDatabaseReference.child(currentSessionId).child("status").setValue("uploaded on the device"); // upload_failed
            runOnUiThread(() -> textResult.setText("Uploaded Successfully...")); // "Upload Failed: " + e.getMessage()
            if (tempFile != null && tempFile.exists()) tempFile.delete();
        });
    }

    private File copyUriToCache(@NonNull Uri uri, @NonNull String fileName) {
        File outFile = new File(getCacheDir(), fileName);
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(outFile)) {
            if (in == null) return null;
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
            return outFile;
        } catch (Exception e) {
            Log.e(TAG, "copyUriToCache failed: " + e.getMessage());
            return null;
        }
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(10000)
                .build();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        String locText = "Live Location: Lat=" + lat + ", Lng=" + lng;
                        textResult.setText(locText);

                        if (currentSessionId != null) {
                            Map<String, Object> locationData = new HashMap<>();
                            locationData.put("latitude", lat);
                            locationData.put("longitude", lng);
                            locationData.put("timestamp", System.currentTimeMillis());
                            firebaseDatabaseReference.child(currentSessionId)
                                    .child("locations").push().setValue(locationData);
                        }
                    }
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording && currentRecording != null) {
            currentRecording.stop();
        }
        if (cameraExecutor != null) cameraExecutor.shutdown();
        stopLocationUpdates();
    }
}
