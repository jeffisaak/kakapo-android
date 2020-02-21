package com.aptasystems.kakapo.entities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Pair;

import com.aptasystems.kakapo.R;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;

import androidx.core.content.ContextCompat;
import io.requery.Entity;
import io.requery.ForeignKey;
import io.requery.Generated;
import io.requery.Key;
import io.requery.ManyToOne;
import io.requery.OneToMany;
import io.requery.Table;
import kakapo.client.ItemSerializer;
import kakapo.client.model.BaseHeaderOrContent;
import kakapo.client.model.ItemSerializeException;
import kakapo.client.model.RegularContentV1;
import kakapo.client.model.RegularHeaderV1;
import kakapo.client.model.ResponseContentV1;
import kakapo.client.model.ResponseHeaderV1;
import kakapo.client.model.UnknownItemTypeException;

@Entity
@Table(name = "share")
public class AbstractShare {

    @Key
    @Generated
    long _id;

    ShareType _type;

    long _timestampGmt;

    @ForeignKey
    @ManyToOne
    UserAccount _userAccount;

    ShareState _state;

    String _errorMessage;

    @OneToMany
    Set<ShareRecipient> _recipients;

    Long _parentItemRemoteId;

    Long _rootItemRemoteId;

    String _title;

    String _url;

    String _message;

    String _attachmentUri;

    String _mimeType;

    public void setId(long id) {
        _id = id;
    }

    public Pair<byte[], byte[]> serialize(Context context)
            throws IOException, UnknownItemTypeException, ItemSerializeException {

        BaseHeaderOrContent header;
        BaseHeaderOrContent content;
        switch (_type) {
            case RegularV1:
                Pair<RegularHeaderV1, RegularContentV1> regularV1 = buildRegularV1(context);
                header = regularV1.first;
                content = regularV1.second;
                break;
            case ResponseV1:
                Pair<ResponseHeaderV1, ResponseContentV1> responseV1 = buildResponseV1(context);
                header = responseV1.first;
                content = responseV1.second;
                break;
            default:
                throw new UnknownItemTypeException("Unknown item type: " + _type.name());
        }

        // Serialize the header and content.
        ItemSerializer itemSerializer = new ItemSerializer();
        byte[] headerData = itemSerializer.serialize(header);
        byte[] contentData = itemSerializer.serialize(content);

        // Return byte arrays.
        return new Pair<>(headerData, contentData);
    }

    public byte[] extractThumbnailData(Context context) throws IOException {

        if (_attachmentUri == null) {
            return null;
        }

        // If we don't have permission, just return null; we'll rely on our caller to handle
        // permissions stuff.
        int permissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return null;
        }

        Uri attachmentUri = Uri.parse(_attachmentUri);
        InputStream attachmentInputStream =
                context.getContentResolver().openInputStream(attachmentUri);

        byte[] attachmentData = IOUtils.toByteArray(attachmentInputStream);
        return generateThumbnailData(context, _mimeType, attachmentData);
    }

    private Pair<RegularHeaderV1, RegularContentV1> buildRegularV1(Context context)
            throws IOException {

        // Open the attachment and create thumbnail and attachment byte arrays for later use.
        // If there is no attachment, or if the attachment isn't of a type that can be thumbnailed,
        // one or both of these byte arrays will be null.
        byte[] attachmentData = null;
        byte[] thumbnailData = null;
        if (_attachmentUri != null) {

            Uri attachmentUri = Uri.parse(_attachmentUri);
            InputStream attachmentInputStream =
                    context.getContentResolver().openInputStream(attachmentUri);

            // Extract the attachment data.
            attachmentData = IOUtils.toByteArray(attachmentInputStream);
            thumbnailData = generateThumbnailData(context, _mimeType, attachmentData);

            attachmentInputStream.close();
        }

        // Build our header and content objects.
        RegularHeaderV1 header = new RegularHeaderV1(_title, _url, _message, thumbnailData);
        RegularContentV1 content = new RegularContentV1(attachmentData);

        return new Pair<>(header, content);
    }

    private byte[] generateThumbnailData(Context context, String mimeType, byte[] attachmentData) {
        byte[] thumbnailData = null;

        // If the mime type indicates the attachment is an image, we will generate a thumbnail
        // of the image for the item header.
        if (Objects.equals(mimeType, "image/jpeg") || Objects.equals(mimeType, "image/png")) {

            // Load the byte array as a bitmap, get the aspect ratio, scale the width
            // and height, generate a thumbnail bitmap, and write it to a byte array.
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(attachmentData,
                    0,
                    attachmentData.length);
            float aspectRatio = originalBitmap.getWidth() / (float) originalBitmap.getHeight();
            int width = context.getResources().getInteger(R.integer.thumbnail_image_width);
            int height = Math.round(width / aspectRatio);
            Bitmap thumbnailBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true);
            ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG,
                    context.getResources().getInteger(R.integer.thumbnail_jpeg_quality),
                    thumbnailOutputStream);
            thumbnailData = thumbnailOutputStream.toByteArray();

            // Recycle our bitmaps.
            originalBitmap.recycle();
            thumbnailBitmap.recycle();
        }

        return thumbnailData;
    }

    private Pair<ResponseHeaderV1, ResponseContentV1> buildResponseV1(Context context) {
        ResponseHeaderV1 header = new ResponseHeaderV1(_message);
        ResponseContentV1 content = new ResponseContentV1();
        return new Pair<>(header, content);
    }
}
