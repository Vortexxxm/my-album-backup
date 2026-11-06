package com.example.mybackup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQ = 42;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setPadding(60, 60, 60, 60);

        TextView title = new TextView(this);
        title.setText("My Album Backup");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        ll.addView(title);

        status = new TextView(this);
        status.setText("Tap the button to back up your photos to your server.");
        status.setTextSize(14);
        status.setPadding(0, 30, 0, 30);
        status.setGravity(Gravity.CENTER);
        ll.addView(status);

        Button btn = new Button(this);
        btn.setText("Back Up My Photos");
        btn.setOnClickListener(v -> requestPerms());
        ll.addView(btn);

        setContentView(ll);
    }

    private void requestPerms() {
        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED)
                need.add(Manifest.permission.READ_MEDIA_IMAGES);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED)
                need.add(Manifest.permission.READ_MEDIA_VIDEO);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED)
                need.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
                need.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (need.isEmpty()) startBackup();
        else ActivityCompat.requestPermissions(this, need.toArray(new String[0]), REQ);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        for (int r : res) {
            if (r != PackageManager.PERMISSION_GRANTED) {
                status.setText("Permission denied. Cannot back up.");
                return;
            }
        }
        startBackup();
    }

    private void startBackup() {
        status.setText("Backup started. You can close the app.");
        Intent svc = new Intent(this, UploadService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
        else startService(svc);
    }
}
