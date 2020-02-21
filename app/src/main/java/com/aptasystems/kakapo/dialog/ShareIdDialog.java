package com.aptasystems.kakapo.dialog;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.aptasystems.kakapo.R;

import net.glxn.qrgen.android.QRCode;

public class ShareIdDialog extends BaseDialog {

    private static final String TAG = ShareIdDialog.class.getSimpleName();

    private static final String ARG_GUID = "guid";
    private ImageView _imageView;

    private String _guid;

    public static ShareIdDialog newInstance(String guid) {
        ShareIdDialog result = new ShareIdDialog();
        Bundle args = new Bundle();
        args.putString(ARG_GUID, guid);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _guid = getArguments().getString(ARG_GUID);

        _imageView = view.findViewById(R.id.dialog_share_id_qr_code_image_view);

        showQrCode();
    }

    private void showQrCode() {
        // Generate the QR code bitmap and set it in the image view.
        int pixelSize = getResources().getDimensionPixelSize(R.dimen.qr_code_size);
        Bitmap qrCodeBitmap = QRCode.from(_guid).withSize(pixelSize, pixelSize).bitmap();
        _imageView.setImageBitmap(qrCodeBitmap);
    }

    @Override
    protected void okPressed() {
        dismiss();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_share_id;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }
}
