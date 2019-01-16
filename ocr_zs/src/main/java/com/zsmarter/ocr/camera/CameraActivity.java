/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.zsmarter.ocr.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.zsmarter.ocr.R;
import com.zsmarter.ocr.crop.CropView;
import com.zsmarter.ocr.crop.FrameOverlayView;
import com.zsmarter.ocr.translator.ImageTranslator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import static com.zsmarter.ocr.translator.ImageTranslator.LANGUAGE_CHINESE;
import static com.zsmarter.ocr.translator.ImageTranslator.LANGUAGE_NUM;

public class CameraActivity extends Activity {

    private static final String TAG = "CameraActivity";
    public static final String KEY_OUTPUT_FILE_PATH = "outputFilePath";
    public static final String KEY_CONTENT_TYPE = "contentType";
    public static final String KEY_LANGUAGE_TYPE = "languageType";
    public static final String KEY_PAGE_SEG_MODE = "page_seg_mode";
    public static final String KEY_SCAN_MODE = "scan_mode";
    public static final String KEY_CROP_ENABLED = "crop_enabled";
    public static final String KEY_AUTO_CROP_ENABLED = "auto_crop_enabled";

    public static final String CONTENT_TYPE_GENERAL = "general";

    public static final String CONTENT_TYPE_BANK_CARD = "bankCard";
    public static final String CONTENT_TYPE_ID_CARD = "idCard";
    public static final String CONTENT_TYPE_NUM = "num";
    public static final String CONTENT_TYPE_CHI_SIM = "chi_sim";
    public static final String CONTENT_TYPE_ENG = "eng";

    public static final String RESULT_TRANSLATE = "result_translate";

    private static final int REQUEST_CODE_PICK_IMAGE = 100;
    private static final int PERMISSIONS_REQUEST_CAMERA = 800;
    private static final int PERMISSIONS_EXTERNAL_STORAGE = 801;
    private static final int MSG_TAKE_PICTURE = 1001;
    private static final int MSG_SCAN_TO_GET_PICTURE = 1002;

    private int mPageSegMode;
    private boolean isScanMode;
    private boolean isCropEnabled;
    private boolean isAutoCropEnabled;
    private File outputFile;
    private String contentType;
    private Handler mHandler;
    private String mLanguage = LANGUAGE_CHINESE;

    private OCRCameraLayout takePictureContainer;
    private OCRCameraLayout cropContainer;
    private ImageView lightButton;
    private CameraView cameraView;
    private CropView cropView;
    private FrameOverlayView overlayView;
    private MaskView cropMaskView;
    private ImageView takePhotoBtn;
    private ImageView displayImageView;
    private PermissionCallback permissionCallback = new PermissionCallback() {
        @Override
        public boolean onRequestPermission() {
            ActivityCompat.requestPermissions(CameraActivity.this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_CAMERA);
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.zs_ocr_activity_camera);

        takePictureContainer = (OCRCameraLayout) findViewById(R.id.take_picture_container);

        cameraView = (CameraView) findViewById(R.id.camera_view);
        cameraView.getCameraControl().setPermissionCallback(permissionCallback);
        lightButton = (ImageView) findViewById(R.id.light_button);
        lightButton.setOnClickListener(lightButtonOnClickListener);
        takePhotoBtn = (ImageView) findViewById(R.id.take_photo_button);
        findViewById(R.id.album_button).setOnClickListener(albumButtonOnClickListener);
        takePhotoBtn.setOnClickListener(takeButtonOnClickListener);

        cropView = (CropView) findViewById(R.id.crop_view);
        cropContainer = (OCRCameraLayout) findViewById(R.id.crop_container);
        overlayView = (FrameOverlayView) findViewById(R.id.overlay_view);
        cropContainer.findViewById(R.id.confirm_button).setOnClickListener(cropConfirmButtonListener);
        cropMaskView = (MaskView) cropContainer.findViewById(R.id.crop_mask_view);
        cropContainer.findViewById(R.id.cancel_button).setOnClickListener(cropCancelButtonListener);

        setOrientation(getResources().getConfiguration());

        initParams();
    }

    private void initParams() {
        mHandler = new TakePictureHandler(this);

        String outputPath = getIntent().getStringExtra(KEY_OUTPUT_FILE_PATH);
        if (outputPath != null) {
            outputFile = new File(outputPath);
        }

        isScanMode = getIntent().getBooleanExtra(KEY_SCAN_MODE, false);
        isCropEnabled = getIntent().getBooleanExtra(KEY_CROP_ENABLED, false);
        isAutoCropEnabled = getIntent().getBooleanExtra(KEY_AUTO_CROP_ENABLED, false);
        mPageSegMode = getIntent().getIntExtra(KEY_PAGE_SEG_MODE, TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT);
        mLanguage = getIntent().getStringExtra(KEY_LANGUAGE_TYPE);
        contentType = getIntent().getStringExtra(KEY_CONTENT_TYPE);
        if (contentType == null) {
            contentType = CONTENT_TYPE_GENERAL;
        }
        int maskType;
        switch (contentType) {
            case CONTENT_TYPE_BANK_CARD:
                mLanguage = LANGUAGE_NUM;
                maskType = MaskView.MASK_TYPE_BANK_CARD;
                overlayView.setVisibility(View.INVISIBLE);
                break;
            case CONTENT_TYPE_ID_CARD:
                mLanguage = LANGUAGE_CHINESE;
                maskType = MaskView.MASK_TYPE_ID_CARD_FRONT;
                overlayView.setVisibility(View.INVISIBLE);
                break;
            case CONTENT_TYPE_GENERAL:
            default:
                if (isAutoCropEnabled) {
                    if (mPageSegMode == TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT) {
                        maskType = MaskView.MASK_TYPE_MUL_LINE;
                    } else {
                        maskType = MaskView.MASK_TYPE_SINGLE_LINE;
                    }
//                    overlayView.setVisibility(View.INVISIBLE);
                } else {
                    maskType = MaskView.MASK_TYPE_NONE;
                }
                break;
        }

        cameraView.setEnableScan(isScanMode);
        cameraView.setMaskType(maskType, this);
        cameraView.setAutoCropEnabled(isAutoCropEnabled);
        cropMaskView.setMaskType(maskType);

        cameraView.setTakePictureCallback(takePictureCallback);
        cameraView.setSaveFile(outputFile);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
        doClear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
        if (isScanMode) {
            mHandler.sendEmptyMessageDelayed(MSG_SCAN_TO_GET_PICTURE, 2000);
        }
    }

    private void showCrop() {
        cameraView.getCameraControl().pause();
        updateFlashMode();
        takePictureContainer.setVisibility(View.INVISIBLE);
        cropContainer.setVisibility(View.VISIBLE);
    }

    // take photo;
    private void updateFlashMode() {
        int flashMode = cameraView.getCameraControl().getFlashMode();
        if (flashMode == ICameraControl.FLASH_MODE_TORCH) {
            lightButton.setImageResource(R.drawable.bd_ocr_light_on);
        } else {
            lightButton.setImageResource(R.drawable.bd_ocr_light_off);
        }
    }

    private final View.OnClickListener albumButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ActivityCompat.requestPermissions(CameraActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSIONS_EXTERNAL_STORAGE);
                    return;
                }
            }
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        }
    };

    private View.OnClickListener lightButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (cameraView.getCameraControl().getFlashMode() == ICameraControl.FLASH_MODE_OFF) {
                cameraView.getCameraControl().setFlashMode(ICameraControl.FLASH_MODE_TORCH);
            } else {
                cameraView.getCameraControl().setFlashMode(ICameraControl.FLASH_MODE_OFF);
            }
            updateFlashMode();
        }
    };

    private View.OnClickListener takeButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            takePicture();
        }
    };

    private void takePicture() {
        cameraView.getCameraControl().getAbortingScan().compareAndSet(true, false);
    }

    private void getScanPicture() {
        cameraView.getCameraControl().getAbortingScan().compareAndSet(true, false);
    }

    private CameraView.OnTakePictureCallback takePictureCallback = new CameraView.OnTakePictureCallback() {
        @Override
        public void onPictureTaken(final Bitmap bitmap) {
            if (bitmap != null) {
                if (isScanMode) {
                    doTranslate(bitmap);
                } else {
                    if (isCropEnabled && cropMaskView.getMaskType() != MaskView.MASK_TYPE_SINGLE_LINE) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                takePictureContainer.setVisibility(View.INVISIBLE);
                                cropView.setFilePath(outputFile.getAbsolutePath());
                                cropMaskView.setVisibility(View.INVISIBLE);
                                showCrop();
                            }
                        });
                    } else {
                        Intent intent = new Intent();
                        intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, contentType);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                }

            }
        }
    };

    private boolean getFocusState() {
        return cameraView.getCameraControl().getFocusState();
    }

    private void showTakePicture() {
        cameraView.getCameraControl().resume();
        updateFlashMode();
        takePictureContainer.setVisibility(View.VISIBLE);
        cropContainer.setVisibility(View.INVISIBLE);
    }

    private View.OnClickListener cropCancelButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // 释放 cropView中的bitmap;
            cropView.setFilePath(null);
            showTakePicture();
        }
    };

    private View.OnClickListener cropConfirmButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Rect rect = overlayView.getFrameRect();
            Bitmap cropped = cropView.crop(rect);
            cropAndConfirm();
            doConfirmResult(cropped);
        }
    };

    private void cropAndConfirm() {
        cameraView.getCameraControl().pause();
        updateFlashMode();
    }

    private void doConfirmResult(final Bitmap bitmap) {
        CameraThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent();
                intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, contentType);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }

    //用于扫描模式
    private void doTranslate(final Bitmap bitmap) {
        CameraThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "**************doTranslate: start**************");

                ImageTranslator imageTranslator = ImageTranslator.getInstance();
                imageTranslator.setContentType(contentType);
                imageTranslator.setLanguage(mLanguage);
                imageTranslator.setmPageSegMode(mPageSegMode);
                imageTranslator.translate(CameraActivity.this, bitmap, new ImageTranslator.TesseractCallback() {
                    @Override
                    public void onStart(Bitmap bitmap) {

                    }

                    @Override
                    public void onResult(String result) {
                        Log.d(TAG, getString(R.string.translate_success) + result);
                        Log.d(TAG, "-------------End------------------");
                        if (mHandler != null) {
                            mHandler.removeCallbacksAndMessages(null);
                        }

                        Intent intent = new Intent();
                        intent.putExtra(CameraActivity.KEY_CONTENT_TYPE, contentType);
                        intent.putExtra(CameraActivity.RESULT_TRANSLATE, result);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }

                    @Override
                    public void onFail(final String reason) {
                        Log.d(TAG, getString(R.string.translate_failed)+ reason);
                        Log.d(TAG, "-------------End------------------");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(CameraActivity.this, getString(R.string.translate_failed)+ reason, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(contentURI, null, null, null, null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setOrientation(newConfig);
    }

    private void setOrientation(Configuration newConfig) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation;
        int cameraViewOrientation = CameraView.ORIENTATION_PORTRAIT;
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                cameraViewOrientation = CameraView.ORIENTATION_PORTRAIT;
                orientation = OCRCameraLayout.ORIENTATION_PORTRAIT;
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                orientation = OCRCameraLayout.ORIENTATION_HORIZONTAL;
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                    cameraViewOrientation = CameraView.ORIENTATION_HORIZONTAL;
                } else {
                    cameraViewOrientation = CameraView.ORIENTATION_INVERT;
                }
                break;
            default:
                orientation = OCRCameraLayout.ORIENTATION_PORTRAIT;
                cameraView.setOrientation(CameraView.ORIENTATION_PORTRAIT);
                break;
        }
        takePictureContainer.setOrientation(orientation);
        cameraView.setOrientation(cameraViewOrientation);
        cropContainer.setOrientation(orientation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                cropView.setFilePath(getRealPathFromURI(uri));
                showCrop();
            } else {
                cameraView.getCameraControl().resume();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraView.getCameraControl().refreshPermission();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.camera_permission_required, Toast.LENGTH_LONG)
                            .show();
                }
                break;
            }
            case PERMISSIONS_EXTERNAL_STORAGE:
            default:
                break;
        }
    }

    /**
     * 做一些收尾工作
     */
    private void doClear() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.doClear();
    }

    private static class TakePictureHandler extends Handler {
        private final WeakReference<CameraActivity> cameraActivityWeakReference;

        private TakePictureHandler(CameraActivity cameraActivity) {
            cameraActivityWeakReference = new WeakReference<CameraActivity>(cameraActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            CameraActivity cameraActivity = cameraActivityWeakReference.get();
            switch (msg.what) {
                case MSG_SCAN_TO_GET_PICTURE:
                    Log.d(TAG, "handleMessage MSG_SCAN_TO_GET_PICTURE");
                    if (cameraActivity != null) {
                        if (cameraActivity.getFocusState()){
                            cameraActivity.getScanPicture();
                        } else {
                            sendEmptyMessageDelayed(MSG_SCAN_TO_GET_PICTURE, 100);
                        }
                    }

                    break;
                default:
                    break;
            }
        }
    }
}
