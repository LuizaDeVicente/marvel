package com.example.marvelcerto;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.example.marvelcerto.api.Character;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local call-style experience: character portrait and live self preview; no remote call. */
public class Camera extends AppCompatActivity {
    private PreviewView preview;
    private ProcessCameraProvider provider;
    private TextView status;
    private Button snapshot;
    private boolean cameraEnabled = true;
    private boolean requesting;
    private boolean saving;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                requesting = false;
                if (granted) startCamera();
                else status.setText("Câmera não autorizada. Toque em Câmera para permitir o acesso.");
            });

    public static Intent intent(Context context, Character character) {
        return new Intent(context, Camera.class).putExtra("name", character.name)
                .putExtra("image", character.getImageUrl());
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!getIntent().hasExtra("name")) { startActivity(new Intent(this, CharacterPickerActivity.class)); finish(); return; }
        setContentView(R.layout.activity_camera);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ((TextView)findViewById(R.id.txtCallName)).setText(getIntent().getStringExtra("name"));
        Glide.with(this).load(getIntent().getStringExtra("image"))
                .placeholder(android.R.drawable.ic_menu_gallery).error(android.R.drawable.ic_menu_gallery)
                .into((ImageView)findViewById(R.id.imgCharacter));
        status = findViewById(R.id.txtStatus);
        preview = findViewById(R.id.selfPreview);
        preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        snapshot = findViewById(R.id.btnSnapshot);
        preview.getPreviewStreamState().observe(this, stream -> snapshot.setEnabled(
                stream == PreviewView.StreamState.STREAMING && cameraEnabled && !saving));
        if (state != null) cameraEnabled = state.getBoolean("cameraEnabled", true);
        findViewById(R.id.btnEndCall).setOnClickListener(v -> finish());
        findViewById(R.id.btnToggleCamera).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cameraEnabled = true;
                if (!requesting) { requesting = true; permission.launch(Manifest.permission.CAMERA); }
                return;
            }
            cameraEnabled = !cameraEnabled;
            if (cameraEnabled) startCamera(); else {
                if (provider != null) provider.unbindAll();
                preview.setVisibility(View.INVISIBLE);
                snapshot.setEnabled(false);
                status.setText("Sua câmera está desligada.");
            }
            ((Button)v).setText(cameraEnabled ? "Desligar câmera" : "Ligar câmera");
        });
        snapshot.setOnClickListener(v -> saveSnapshot());
        if (cameraEnabled) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera();
            else { requesting = true; permission.launch(Manifest.permission.CAMERA); }
        } else { preview.setVisibility(View.INVISIBLE); ((Button)findViewById(R.id.btnToggleCamera)).setText("Ligar câmera"); }
    }
    private void startCamera() {
        if (!cameraEnabled || isDestroyed() || isFinishing()) return;
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            if (isDestroyed() || isFinishing() || !cameraEnabled) return;
            try {
                provider = future.get();
                if (!provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    status.setText("Este aparelho não possui câmera frontal disponível."); return;
                }
                preview.setVisibility(View.VISIBLE);
                Preview useCase = new Preview.Builder().build();
                useCase.setSurfaceProvider(preview.getSurfaceProvider());
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, useCase);
                status.setText("Sua câmera aparece apenas neste aparelho.");
                ((Button)findViewById(R.id.btnToggleCamera)).setText("Desligar câmera");
            } catch (Exception e) {
                status.setText("Não foi possível abrir a câmera. Toque em Câmera para tentar novamente.");
                cameraEnabled = false;
                snapshot.setEnabled(false);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private void saveSnapshot() {
        View stage = findViewById(R.id.callStage);
        Bitmap self = preview.getBitmap();
        if (self == null || stage.getWidth() == 0 || stage.getHeight() == 0) {
            status.setText("Aguarde a câmera ficar pronta para tirar a foto."); return;
        }
        Bitmap photo = Bitmap.createBitmap(stage.getWidth(), stage.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(photo);
        stage.draw(canvas);
        int[] base = new int[2]; int[] corner = new int[2];
        stage.getLocationInWindow(base); preview.getLocationInWindow(corner);
        int left = corner[0] - base[0], top = corner[1] - base[1];
        canvas.save();
        android.graphics.Path clip = new android.graphics.Path();
        float radius = 18 * getResources().getDisplayMetrics().density;
        clip.addRoundRect(new android.graphics.RectF(left, top, left + preview.getWidth(), top + preview.getHeight()), radius, radius, android.graphics.Path.Direction.CW);
        canvas.clipPath(clip);
        canvas.drawBitmap(self, null, new Rect(left, top, left + preview.getWidth(), top + preview.getHeight()), null);
        canvas.restore();
        self.recycle();
        saving = true;
        snapshot.setEnabled(false);
        status.setText("Salvando foto…");
        writer.execute(() -> {
            Uri uri = null;
            boolean success = false;
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "MarvelVerse-" + System.currentTimeMillis() + ".jpg");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MarvelVerse");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new java.io.IOException("No media destination");
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null || !photo.compress(Bitmap.CompressFormat.JPEG, 92, out)) throw new java.io.IOException("Write failed");
                }
                values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
                success = true;
            } catch (Exception e) {
                if (uri != null) getContentResolver().delete(uri, null, null);
            } finally { photo.recycle(); }
            final boolean saved = success;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                saving = false;
                status.setText(saved ? "Foto salva em Pictures/MarvelVerse." : "Não foi possível salvar a foto. Tente novamente.");
                snapshot.setEnabled(cameraEnabled && preview.getPreviewStreamState().getValue() == PreviewView.StreamState.STREAMING);
            });
        });
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("cameraEnabled", cameraEnabled); super.onSaveInstanceState(state); }
    @Override protected void onDestroy() { if (provider != null) provider.unbindAll(); writer.shutdown(); super.onDestroy(); }
}
