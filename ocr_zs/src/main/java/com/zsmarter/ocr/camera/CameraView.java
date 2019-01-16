/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.zsmarter.ocr.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zsmarter.ocr.R;
import com.zsmarter.ocr.util.DimensionUtil;
import com.zsmarter.ocr.util.ImageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 负责，相机的管理。同时提供，裁剪遮罩功能。
 */
public class CameraView extends FrameLayout {

    /**
     * 照相回调
     */
    interface OnTakePictureCallback {
        void onPictureTaken(Bitmap bitmap);
    }

    /**
     * 垂直方向 {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_PORTRAIT = 0;
    /**
     * 水平方向 {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_HORIZONTAL = 90;
    /**
     * 水平翻转方向 {@link #setOrientation(int)}
     */
    public static final int ORIENTATION_INVERT = 270;

    @IntDef({ORIENTATION_PORTRAIT, ORIENTATION_HORIZONTAL, ORIENTATION_INVERT})
    public @interface Orientation {

    }

    private ICameraControl cameraControl;

    /**
     * 相机预览View
     */
    private View displayView;
    /**
     * 身份证，银行卡，等裁剪用的遮罩
     */
    private MaskView maskView;

    /**
     * 用于显示提示证 "请对齐身份证正面" 之类的背景
     */
    private ImageView hintView;

    /**
     * 用于显示提示证 "请对齐身份证正面" 之类的文字
     */
    private TextView hintViewText;

    /**
     * 提示文案容器
     */
    private LinearLayout hintViewTextWrapper;

    /**
     * 是否扫描
     */
    private boolean isEnableScan;

    public void setEnableScan(boolean enableScan) {
        isEnableScan = enableScan;
    }

    /**
     * 图片保存路径
     */
    private File picSave;

    public void setSaveFile(File file) {
        picSave = file;
    }
    /**
     * 是否有图片采集框
     */
    private boolean isAutoCropEnabled;

    public void setAutoCropEnabled(boolean enableCrop) {
        isAutoCropEnabled = enableCrop;
    }

    public ICameraControl getCameraControl() {
        return cameraControl;
    }

    public void setOrientation(@Orientation int orientation) {
        cameraControl.setDisplayOrientation(orientation);
    }

    public CameraView(Context context) {
        super(context);
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void start() {
        cameraControl.start();
        setKeepScreenOn(true);
    }

    public void stop() {
        cameraControl.stop();
        setKeepScreenOn(false);
    }

    private OnTakePictureCallback takePictureCallback;

    public void setTakePictureCallback(OnTakePictureCallback callback) {
        takePictureCallback = callback;
    }

    public void setMaskType(int maskType, final Context ctx) {
        maskView.setMaskType(maskType);

        switch (maskType) {
            case MaskView.MASK_TYPE_BANK_CARD:
                maskView.setVisibility(VISIBLE);
                hintView.setVisibility(VISIBLE);
                break;
            case MaskView.MASK_TYPE_ID_CARD_FRONT:
                break;
            case MaskView.MASK_TYPE_MUL_LINE:
            case MaskView.MASK_TYPE_SINGLE_LINE:
                maskView.setVisibility(VISIBLE);
                hintView.setVisibility(INVISIBLE);
                break;
            case MaskView.MASK_TYPE_NONE:
            default:
                maskView.setVisibility(INVISIBLE);
                hintView.setVisibility(INVISIBLE);
                break;
        }


        cameraControl.setDetectCallback(isEnableScan, new ICameraControl.OnDetectPictureCallback() {
            @Override
            public void onDetect(byte[] data, int rotation) {
                Log.d("CameraView", "onDetect() called ");
                detect(picSave, data, rotation);
            }
        });

    }

    private void init() {
        cameraControl = new CameraControl(getContext());

        displayView = cameraControl.getDisplayView();
        addView(displayView);

        maskView = new MaskView(getContext());
        addView(maskView);

        hintView = new ImageView(getContext());
        addView(hintView);

        hintViewTextWrapper = new LinearLayout(getContext());
        hintViewTextWrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                DimensionUtil.dpToPx(25));

        lp.gravity = Gravity.CENTER;
        hintViewText = new TextView(getContext());
        hintViewText.setBackgroundResource(R.drawable.bd_ocr_round_corner);
        hintViewText.setAlpha(0.5f);
        hintViewText.setPadding(DimensionUtil.dpToPx(10), 0, DimensionUtil.dpToPx(10), 0);
        hintViewTextWrapper.addView(hintViewText, lp);


        hintViewText.setGravity(Gravity.CENTER);
        hintViewText.setTextColor(Color.WHITE);
        hintViewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hintViewText.setText(R.string.bank_card_hint_view_text);
        hintViewText.setVisibility(View.GONE);

        addView(hintViewTextWrapper, lp);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        displayView.layout(left, 0, right, bottom - top);
        maskView.layout(left, 0, right, bottom - top);

        int hintViewWidth = DimensionUtil.dpToPx(250);
        int hintViewHeight = DimensionUtil.dpToPx(25);

        int hintViewLeft = (getWidth() - hintViewWidth) / 2;
        int hintViewTop = maskView.getCardFrameRect().bottom + DimensionUtil.dpToPx(16);

        hintViewTextWrapper.layout(hintViewLeft, hintViewTop,
                hintViewLeft + hintViewWidth, hintViewTop + hintViewHeight);

        hintView.layout(hintViewLeft, hintViewTop,
                hintViewLeft + hintViewWidth, hintViewTop + hintViewHeight);
    }

    private void detect(File outputFile, byte[] data, final int rotation) {

        try {
            Rect previewFrame = cameraControl.getPreviewFrame();

            if (maskView.getWidth() == 0 || maskView.getHeight() == 0
                    || previewFrame.width() == 0 || previewFrame.height() == 0) {
                return;
            }

            // BitmapRegionDecoder不会将整个图片加载到内存。
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, true);

            int width = rotation % 180 == 0 ? decoder.getWidth() : decoder.getHeight();
            int height = rotation % 180 == 0 ? decoder.getHeight() : decoder.getWidth();

            Rect frameRect;
            if (isAutoCropEnabled || maskView.getMaskType() == MaskView.MASK_TYPE_BANK_CARD) {
                frameRect = maskView.getCardFrameRect();
            } else {
                frameRect = maskView.getFrameRect();
            }

            int left = width * frameRect.left / maskView.getWidth();
            int top = height * frameRect.top / maskView.getHeight();
            int right = width * frameRect.right / maskView.getWidth();
            int bottom = height * frameRect.bottom / maskView.getHeight();

            // 高度大于图片
            if (previewFrame.top < 0) {
                // 宽度对齐。
                int adjustedPreviewHeight = previewFrame.height() * getWidth() / previewFrame.width();
                int topInFrame = ((adjustedPreviewHeight - frameRect.height()) / 2)
                        * getWidth() / previewFrame.width();
                int bottomInFrame = ((adjustedPreviewHeight + frameRect.height()) / 2) * getWidth()
                        / previewFrame.width();

                // 等比例投射到照片当中。
                top = topInFrame * height / previewFrame.height();
                bottom = bottomInFrame * height / previewFrame.height();
            } else {
                // 宽度大于图片
                if (previewFrame.left < 0) {
                    // 高度对齐
                    int adjustedPreviewWidth = previewFrame.width() * getHeight() / previewFrame.height();
                    int leftInFrame = ((adjustedPreviewWidth - frameRect.width()) / 2) * getHeight()
                            / previewFrame.height();
                    int rightInFrame = ((adjustedPreviewWidth + frameRect.width()) / 2) * getHeight()
                            / previewFrame.height();

                    // 等比例投射到照片当中。
                    left = leftInFrame * width / previewFrame.width();
                    right = rightInFrame * width / previewFrame.width();
                }
            }

            Rect region = new Rect();
            region.left = left;
            region.top = top;
            region.right = right;
            region.bottom = bottom;
            if (maskView.getMaskType() == MaskView.MASK_TYPE_BANK_CARD) {
                region = maskView.getCropFrameRect(region);
                region = maskView.getLessFrameRect(region);
            }

            // 90度或者270度旋转
            if (rotation % 180 == 90) {
                int x = decoder.getWidth() / 2;
                int y = decoder.getHeight() / 2;

                int rotatedWidth = region.height();
                int rotated = region.width();

                // 计算，裁剪框旋转后的坐标
                region.left = x - rotatedWidth / 2;
                region.top = y - rotated / 2;
                region.right = x + rotatedWidth / 2;
                region.bottom = y + rotated / 2;
                region.sort();
            }

            BitmapFactory.Options options = new BitmapFactory.Options();

            // 最大图片大小。
            int maxPreviewImageSize = 2560;
            int size = Math.min(decoder.getWidth(), decoder.getHeight());
            size = Math.min(size, maxPreviewImageSize);

            options.inSampleSize = ImageUtil.calculateInSampleSize(options, size, size);
            options.inScaled = true;
            options.inDensity = Math.max(options.outWidth, options.outHeight);
            options.inTargetDensity = size;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = decoder.decodeRegion(region, options);

            if (rotation != 0) {
                // 只能是裁剪完之后再旋转了。有没有别的更好的方案呢？
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                if (bitmap != rotatedBitmap) {
                    // 有时候 createBitmap会复用对象
                    bitmap.recycle();
                }
                bitmap = rotatedBitmap;
            }

            if (outputFile != null) {
                try {
                    if (!outputFile.exists()) {
                        outputFile.createNewFile();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            takePictureCallback.onPictureTaken(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
