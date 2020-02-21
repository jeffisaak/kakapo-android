package com.aptasystems.kakapo.dialog;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.aptasystems.kakapo.R;
import com.aptasystems.kakapo.service.AccountBackupInfo;

import net.glxn.qrgen.android.QRCode;

public class ShareAccountDialog extends BaseDialog {

    private static final String TAG = ShareAccountDialog.class.getSimpleName();

    private static final String ARG_ACCOUNT_BACKUP_INFO = "accountBackupInfo";

    private ImageView _imageView;

    private AccountBackupInfo _accountBackupInfo;

    public static ShareAccountDialog newInstance(AccountBackupInfo accountBackupInfo) {
        ShareAccountDialog result = new ShareAccountDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ACCOUNT_BACKUP_INFO, accountBackupInfo);
        result.setArguments(args);
        return result;
    }

    @Override
    protected void onCreateViewInternal(View view) {
        setCancelable(true);

        _accountBackupInfo = (AccountBackupInfo) getArguments().getSerializable(ARG_ACCOUNT_BACKUP_INFO);

        _imageView = view.findViewById(R.id.dialog_share_account_qr_code_image_view);

        showQrCode();
    }

    private void showQrCode() {

        // Generate the QR code bitmap.
        int pixelSize = getResources().getDimensionPixelSize(R.dimen.qr_code_size);
        Bitmap qrCodeBitmap = QRCode.from(_accountBackupInfo.toString()).withSize(pixelSize, pixelSize).bitmap();

        // Set the QR code bitmap.
        _imageView.setImageBitmap(qrCodeBitmap);
    }

    @Override
    protected void okPressed() {
        // Dismiss the dialog.
        dismiss();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_share_account;
    }

    @Override
    protected boolean usesEventBus() {
        return false;
    }

}
