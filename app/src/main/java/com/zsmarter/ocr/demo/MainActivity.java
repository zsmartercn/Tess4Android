/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.zsmarter.ocr.demo;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.zsmarter.ocr.camera.CameraActivity;
import com.zsmarter.ocr.camera.CameraThreadPool;
import com.zsmarter.ocr.translator.ImageTranslator;

import static com.zsmarter.ocr.translator.ImageTranslator.LANGUAGE_CHINESE;
import static com.zsmarter.ocr.translator.ImageTranslator.LANGUAGE_ENG;
import static com.zsmarter.ocr.translator.ImageTranslator.LANGUAGE_NUM;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_BANKCARD = 101;
    private static final int REQUEST_CODE_IDCARD = 102;
    private static final int REQUEST_CODE_NUM = 103;
    private static final int REQUEST_CODE_CHI_SIM = 104;
    private static final int REQUEST_CODE_CHI_TRA = 105;
    private static final int REQUEST_CODE_ENG = 106;

    private static final int REQUEST_CODE_LOCAL = 107;
    private static final int RESIZE_REQUEST_CODE = 108;

    private String PIC_PATH;
    private ImageView imageView;
    private ImageView imageView2;
    private TextView resultView;
    private TextView processView;
    private Spinner psmMode;
    private Spinner languageType;
//    private CheckBox isScan;
    private CheckBox isCrop;
    private CheckBox isAutoCrop;
    private Button transAgain;

    private boolean isEnableScan = false;

    private Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.image_view);
        imageView2 = findViewById(R.id.image_view2);
        resultView = findViewById(R.id.result_text);
        psmMode = findViewById(R.id.psm_mode_spinner);
        languageType = findViewById(R.id.language_spinner);
        processView = findViewById(R.id.process_view);
        isCrop = findViewById(R.id.is_crop);
        isAutoCrop = findViewById(R.id.is_auto_crop);

        PIC_PATH = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/ocr_pic.jpg";

//        isScan = findViewById(R.id.is_scan);

        // 银行卡号识别
//        findViewById(R.id.bankcard_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
//                intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH, PIC_PATH);
//
//                intent.putExtra(CameraActivity.KEY_CONTENT_TYPE,
//                        CameraActivity.CONTENT_TYPE_BANK_CARD);
//                intent.putExtra(CameraActivity.KEY_SCAN_MODE, isScan.isChecked());
//                startActivityForResult(intent, REQUEST_CODE_BANKCARD);
//            }
//        });

        //身份证识别
//        findViewById(R.id.IDcard_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
//                intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH, PIC_PATH);
//                intent.putExtra(CameraActivity.KEY_CONTENT_TYPE,
//                        CameraActivity.CONTENT_TYPE_ID_CARD);
//                intent.putExtra(CameraActivity.KEY_SCAN_MODE, isScan.isChecked());
//                startActivityForResult(intent, REQUEST_CODE_IDCARD);
//            }
//        });



        //拍照识别
        findViewById(R.id.take_photo_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                intent.putExtra(CameraActivity.KEY_OUTPUT_FILE_PATH, PIC_PATH);
                intent.putExtra(CameraActivity.KEY_CONTENT_TYPE,
                        CameraActivity.CONTENT_TYPE_GENERAL);
                intent.putExtra(CameraActivity.KEY_LANGUAGE_TYPE,
                        convertLanguageType(languageType.getSelectedItemPosition()));
                intent.putExtra(CameraActivity.KEY_PAGE_SEG_MODE,
                        convertPageSegMode(psmMode.getSelectedItemPosition()));
//                intent.putExtra(CameraActivity.KEY_SCAN_MODE, isScan.isChecked());
                intent.putExtra(CameraActivity.KEY_CROP_ENABLED, isCrop.isChecked());
                intent.putExtra(CameraActivity.KEY_AUTO_CROP_ENABLED, isAutoCrop.isChecked());
                startActivityForResult(intent, REQUEST_CODE_CHI_SIM);
            }
        });

        //本地照片识别
        findViewById(R.id.local_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_CODE_LOCAL);
            }
        });

        //继续识别当前图片

        transAgain = findViewById(R.id.trans_again);
        transAgain.setVisibility(View.INVISIBLE);
        transAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBitmap != null) {
                    doTranslator(mBitmap);
                } else {
                    processView.setText(R.string.picture_invalid);
                }
             }
        });

    }

    private int convertPageSegMode(int positon) {
        psmMode.getSelectedItemPosition();
        int mPageSegMode = 11;
        switch (positon) {
            case 0:
                mPageSegMode = 7;
                break;
            case 1:
                break;
        }
        Log.d(TAG, "mPageSegMode : " + mPageSegMode);
        return mPageSegMode;
    }

    private String convertLanguageType(int positon) {
        languageType.getSelectedItemPosition();
        String mLanguageType = LANGUAGE_CHINESE;
        switch (positon) {
            case 0:
                break;
            case 1:
                mLanguageType = LANGUAGE_ENG;
                break;
            case 2:
                mLanguageType = LANGUAGE_NUM;
                break;
        }
        Log.d(TAG, "mContentType : " + mLanguageType);
        return mLanguageType;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        } else {
            switch (requestCode) {
                case REQUEST_CODE_LOCAL:
                    if (isCrop.isChecked()) {
                        resizeImage(data.getData());
                    } else {
                        showImage(data);
                    }
                    break;

                case RESIZE_REQUEST_CODE:
                    if (data != null) {
                        showResizeImage(data);
                    }
                    break;
                default:
                    if (!isEnableScan) {
                        Bitmap bitmap = BitmapFactory.decodeFile(PIC_PATH);
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            doTranslator(bitmap);
                        }
                    } else {
                        resultView.setText(data.getStringExtra(CameraActivity.RESULT_TRANSLATE));
                    }
                    break;
            }
        }

    }

    public void resizeImage(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("return-data", true);
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);

        startActivityForResult(intent, RESIZE_REQUEST_CODE);
    }

    private void showResizeImage(Intent data) {
        Bundle extras = data.getExtras();
        if (extras != null) {
            Bitmap photo = extras.getParcelable("data");
            imageView.setImageBitmap(photo);
            doTranslator(photo);
        }
    }

    private void showImage(Intent data) {
        try {
            Uri selectedImage = data.getData(); //获取系统返回的照片的Uri
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);//从系统表中查询指定Uri对应的照片
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String path = cursor.getString(columnIndex);  //获取照片路径
            cursor.close();
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            imageView.setImageBitmap(bitmap);

            doTranslator(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doTranslator(final Bitmap bitmap) {
        mBitmap = bitmap;
        transAgain.setVisibility(View.INVISIBLE);
        resultView.setText("");
        CameraThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "**************doTranslate: start**************");

                recLen = 0;
                second = 0;
                handler.postDelayed(runnable, 100);

                ImageTranslator imageTranslator = ImageTranslator.getInstance();
                imageTranslator.setContentType(convertLanguageType(languageType.getSelectedItemPosition()));
                imageTranslator.setLanguage(convertLanguageType(languageType.getSelectedItemPosition()));
                imageTranslator.setmPageSegMode(convertPageSegMode(psmMode.getSelectedItemPosition()));
                imageTranslator.translate(MainActivity.this, bitmap, new ImageTranslator.TesseractCallback() {
                    @Override
                    public void onStart(final Bitmap bmp) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView2.setImageBitmap(bmp);
                            }
                        });
                    }

                    @Override
                    public void onResult(final String result) {
                        handler.removeCallbacksAndMessages(null);
                        Log.d(TAG, getString(R.string.translate_success) + result + "\n"
                                + "-------------End------------------");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processView.setText(getString(R.string.translate_success) +
                                        getString(R.string.translate_time) + second + "." + recLen + "s");
                                resultView.setText(result);
                                transAgain.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public void onFail(final String reason) {
                        handler.removeCallbacksAndMessages(null);
                        Log.d(TAG, getString(R.string.translate_failed) + reason + "\n"
                                + "-------------End------------------");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processView.setText(getString(R.string.translate_failed) +
                                        getString(R.string.translate_time) + second + "." + recLen + "s");
                                resultView.setText(reason);
                                transAgain.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }
        });
    }

    //增量0.1s
    private int recLen = 0;
    private int second = 0;
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            recLen++;
            if (recLen >= 9) {
                second++;
                recLen = 0;
            }
            processView.setText(getString(R.string.translating) +
                    getString(R.string.translate_time) + second + "." + recLen + "s");
            handler.postDelayed(this, 100);
        }
    };


}
