package com.aptasystems.kakapo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.ViewGroup;

import com.aptasystems.kakapo.databinding.ActivityAddAccountBinding;
import com.aptasystems.kakapo.service.AccountBackupInfo;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ScanQRCodeActivity extends AppCompatActivity {

    public static final String EXTRA_SCANNED_VALUE = "SCANNED_VALUE";

    private static final int PERMISSION_REQUEST_CAMERA = 100;

    private ActivityAddAccountBinding _binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _binding = ActivityAddAccountBinding.inflate(getLayoutInflater());
        setContentView(_binding.getRoot());

        setSupportActionBar(_binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            _binding.includes.viewCamera.post(this::startCamera);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    _binding.includes.viewCamera.post(this::startCamera);
                }
                return;
            }
        }
    }

    private void startCamera() {
        PreviewConfig previewConfig =
                new PreviewConfig.Builder().setTargetResolution(new Size(640, 640)).build();
        Preview preview = new Preview(previewConfig);

        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(@NonNull Preview.PreviewOutput output) {
                System.out.println("[AAA] onUpdated()");

                ViewGroup parent = (ViewGroup) _binding.includes.viewCamera.getParent();
                parent.removeView(_binding.includes.viewCamera);
                parent.addView(_binding.includes.viewCamera, 0);

                _binding.includes.viewCamera.setSurfaceTexture(output.getSurfaceTexture());
                udpateTransform();
            }

            private void udpateTransform() {
                Matrix matrix = new Matrix();

                float centreX = _binding.includes.viewCamera.getWidth() / 2f;
                float centreY = _binding.includes.viewCamera.getHeight() / 2f;

                float rotationDegrees = 0f;
                switch (_binding.includes.viewCamera.getDisplay().getRotation()) {
                    case Surface.ROTATION_0:
                        rotationDegrees = 0f;
                        break;
                    case Surface.ROTATION_90:
                        rotationDegrees = 90f;
                        break;
                    case Surface.ROTATION_180:
                        rotationDegrees = 180f;
                        break;
                    case Surface.ROTATION_270:
                        rotationDegrees = 270f;
                        break;
                }

                matrix.postRotate(rotationDegrees * -1f, centreX, centreY);

                _binding.includes.viewCamera.setTransform(matrix);
            }

        });

        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new QrCodeAnalyzer());

        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    private class QrCodeAnalyzer implements ImageAnalysis.Analyzer {

        private QRCodeReader _qrCodeReader;
        private final Map<DecodeHintType, String> _hints = new HashMap<>();

        QrCodeAnalyzer() {
            _qrCodeReader = new QRCodeReader();
            _hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        }

        @Override
        public void analyze(ImageProxy image, int rotationDegrees) {

            // Append the three YUV planes into a single byte array.
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            for (ImageProxy.PlaneProxy planeProxy : image.getPlanes()) {
                byte[] data = new byte[planeProxy.getBuffer().capacity()];
                ((ByteBuffer) planeProxy.getBuffer().duplicate().clear()).get(data);
                try {
                    outStream.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Build our image source and build a binary bitmap from it so that the QR code
            // reader can do it's thing.
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(outStream.toByteArray(), image.getWidth(), image.getHeight(),
                    0, 0, image.getWidth(), image.getHeight(),
                    false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = null;
            try {
                result = _qrCodeReader.decode(bitmap, _hints);
            } catch (ReaderException e) {
                // Keep calm. Carry on.
            }

            // If we have a valid result, populate the text fields with the scanned values.
            if (result != null) {
                AccountBackupInfo accountBackupInfo = AccountBackupInfo.from(result.getText());
                if (accountBackupInfo != null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_SCANNED_VALUE, result.getText());
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
            }
        }
    }
}
