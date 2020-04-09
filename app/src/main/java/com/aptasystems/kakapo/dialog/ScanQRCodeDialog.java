package com.aptasystems.kakapo.dialog;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aptasystems.kakapo.R;
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
import androidx.annotation.StringRes;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.app.DialogCompat;
import androidx.core.content.ContextCompat;

public class ScanQRCodeDialog extends BaseDialog {

    private static final String ARG_INSTRUCTIONS_STRING_ID = "instructionsStringId";

    private static final int PERMISSION_REQUEST_CAMERA = 100;

    private TextureView _cameraPreview;

    private QRCodeValidator _validator;
    private QrCodeResultHandler _resultHandler;

    public static ScanQRCodeDialog newInstance(@StringRes int instructionsStringId) {
        ScanQRCodeDialog result = new ScanQRCodeDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_INSTRUCTIONS_STRING_ID, instructionsStringId);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);
        _cameraPreview = view.findViewById(R.id.camera_preview);

        if (getArguments() != null) {
            TextView titleTextView = view.findViewById(R.id.text_view_instructions);
            titleTextView.setText(getArguments().getInt(ARG_INSTRUCTIONS_STRING_ID));
        }

        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            _cameraPreview.post(this::startCamera);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    _cameraPreview.post(this::startCamera);
                } else {
                    dismiss();
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

                ViewGroup parent = (ViewGroup) _cameraPreview.getParent();
                parent.removeView(_cameraPreview);
                parent.addView(_cameraPreview, 2);

                _cameraPreview.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }

            private void updateTransform() {
                Matrix matrix = new Matrix();

                float centreX = _cameraPreview.getWidth() / 2f;
                float centreY = _cameraPreview.getHeight() / 2f;

                float rotationDegrees = 0f;
                switch (_cameraPreview.getDisplay().getRotation()) {
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

                _cameraPreview.setTransform(matrix);
            }

        });

        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new QrCodeAnalyzer());

        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    @Override
    protected void okPressed() {
        // Noop.
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_scan_qr_code;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }

    public void setValidator(QRCodeValidator validator) {
        _validator = validator;
    }

    public void setResultHandler(QrCodeResultHandler resultHandler) {
        _resultHandler = resultHandler;
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

            // If we have a valid result, do the next thing.
            if (result != null) {
                if (_validator == null || _validator.isValid(result.getText())) {
                    if (_resultHandler != null) {
                        dismiss();
                        _resultHandler.handleResult(result.getText());
                    }
                }
            }
        }
    }

    public interface QRCodeValidator {
        boolean isValid(String qrCode);
    }

    public interface QrCodeResultHandler {
        void handleResult(String qrCode);
    }
}

//                AccountBackupInfo accountBackupInfo = AccountBackupInfo.from(result.getText());
//                if (accountBackupInfo != null) {
