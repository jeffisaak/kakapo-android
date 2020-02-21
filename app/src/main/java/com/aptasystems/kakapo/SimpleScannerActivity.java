package com.aptasystems.kakapo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class SimpleScannerActivity extends Activity implements ZXingScannerView.ResultHandler {

    public static final String EXTRA_SCANNED_VALUE = "scannedValue";

    private static final String TAG = SimpleScannerActivity.class.getSimpleName();

    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        if( rawResult.getBarcodeFormat() == BarcodeFormat.QR_CODE &&
                rawResult.getText() != null ) {
            Intent result = new Intent();
            result.putExtra(EXTRA_SCANNED_VALUE, rawResult.getText());
            setResult(RESULT_OK, result);
            finish();
        }

    }
}