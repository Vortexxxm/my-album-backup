package com.example.mybackup;

import android.app.*;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class UploadService extends Service {
    private static final String SERVER = "https://YOUR-TUNNEL.trycloudflare.com";
    private String sessionId;

    @Override
    public void onCreate() {
        super.onCreate();
        sessionId = UUID.randomUUID().toString();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = new NotificationCompat.Builder(this, "backup")
                .setContentTitle("My Album Backup")
                .setContentText("Backing up your photos to your server...")
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    "backup", "Backup", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Shows backup progress");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        startForeground(1, n);

        new Thread(this::backupAll).start();
        return START_NOT_STICKY;
    }

    private void backupAll() {
        Uri[] uris = {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        };
        for (Uri base : uris) {
            String[] cols = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE
            };
            try (Cursor c = getContentResolver().query(base, cols, null, null,
                    MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
                if (c == null) continue;
                int idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                int nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                int mimeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
                while (c.moveToNext()) {
                    long id = c.getLong(idCol);
                    String name = c.getString(nameCol);
                    String mime = c.getString(mimeCol);
                    Uri file = ContentUris.withAppendedId(base, id);
                    try { upload(file, name, mime); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        stopSelf();
    }

    private void upload(Uri uri, String name, String mime) throws IOException {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) return;

        String boundary = "----" + System.currentTimeMillis();
        URL url = new URL(SERVER + "/upload/" + sessionId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        try (OutputStream out = conn.getOutputStream();
             PrintWriter w = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true)) {

            w.append("--").append(boundary).append("\r\n");
            w.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
             .append(name).append("\"\r\n");
            w.append("Content-Type: ").append(mime != null ? mime : "application/octet-stream")
             .append("\r\n\r\n").flush();

            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
            out.flush();
            w.append("\r\n--").append(boundary).append("--\r\n");
            w.flush();
        } finally {
            in.close();
        }
        conn.getResponseCode();
        conn.disconnect();
    }

    @Nullable @Override public IBinder onBind(Intent i) { return null; }
}
