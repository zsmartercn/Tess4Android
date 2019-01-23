/*
 * (C) Copyright 2018, ZSmarter Technology Co, Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
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


public class CameraView extends FrameLayout {


    interface OnTakePictureCallback {
        void onPictureTaken(Bitmap bitmap);
    }

    public static final int ORIENTATION_PORTRAIT = 0;

    public static final int ORIENTATION_HORIZONTAL = 90;

    public static final int ORIENTATION_INVERT = 270;

    @IntDef({ORIENTATION_PORTRAIT, ORIENTATION_HORIZONTAL, ORIENTATION_INVERT})
    public @interface Orientation {

    }

    private ICameraControl cameraControl;

    private View displayView;

    private MaskView maskView;

    private ImageView hintView;

    private TextView hintViewText;

    private LinearLayout hintViewTextWrapper;

    private boolean isEnableScan;

    public void setEnableScan(boolean enableScan) {
        isEnableScan = enableScan;
    }

    private File picSave;

    public void setSaveFile(File file) {
        picSave = file;
    }

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

            // BitmapRegionDecoder, The entire picture will not be loaded into memory
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

            if (previewFrame.top < 0) {
                int adjustedPreviewHeight = previewFrame.height() * getWidth() / previewFrame.width();
                int topInFrame = ((adjustedPreviewHeight - frameRect.height()) / 2)
                        * getWidth() / previewFrame.width();
                int bottomInFrame = ((adjustedPreviewHeight + frameRect.height()) / 2) * getWidth()
                        / previewFrame.width();

                top = topInFrame * height / previewFrame.height();
                bottom = bottomInFrame * height / previewFrame.height();
            } else {
                if (previewFrame.left < 0) {
                    int adjustedPreviewWidth = previewFrame.width() * getHeight() / previewFrame.height();
                    int leftInFrame = ((adjustedPreviewWidth - frameRect.width()) / 2) * getHeight()
                            / previewFrame.height();
                    int rightInFrame = ((adjustedPreviewWidth + frameRect.width()) / 2) * getHeight()
                            / previewFrame.height();

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

            if (rotation % 180 == 90) {
                int x = decoder.getWidth() / 2;
                int y = decoder.getHeight() / 2;

                int rotatedWidth = region.height();
                int rotated = region.width();

                region.left = x - rotatedWidth / 2;
                region.top = y - rotated / 2;
                region.right = x + rotatedWidth / 2;
                region.bottom = y + rotated / 2;
                region.sort();
            }

            BitmapFactory.Options options = new BitmapFactory.Options();

            //Maximum picture size.
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
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                if (bitmap != rotatedBitmap) {
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
