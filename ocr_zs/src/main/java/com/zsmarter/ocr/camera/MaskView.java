/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.zsmarter.ocr.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

import java.io.File;

@SuppressWarnings("unused")
public class MaskView extends View {

    public static final int MASK_TYPE_NONE = 0;
    public static final int MASK_TYPE_ID_CARD_FRONT = 1;
    public static final int MASK_TYPE_ID_CARD_BACK = 2;
    public static final int MASK_TYPE_BANK_CARD = 11;
    public static final int MASK_TYPE_SINGLE_LINE = 21;
    public static final int MASK_TYPE_MUL_LINE = 22;

    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    public void setMaskColor(int maskColor) {
        this.maskColor = maskColor;
    }

    private int lineColor = Color.WHITE;

    private int maskType = MASK_TYPE_BANK_CARD;

    private int maskColor = Color.argb(100, 0, 0, 0);

    private Paint eraser = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint insidePen = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Rect frameCard = new Rect();
    private Rect frameInside = new Rect();

    private Rect frame = new Rect();
    private Rect frameCrop = new Rect();

    public Rect getFrameRect() {
        return new Rect(frame);
    }

    public Rect getCardFrameRect() {
        return new Rect(frameCard);
    }

    public Rect getCropFrameRect(Rect rect) {
        int height = rect.height();
        int width = rect.width();
        Rect frameCrop = new Rect(rect);
        frameCrop.top = (int) (rect.bottom - 0.31 * height);
        frameCrop.bottom = (int) (rect.bottom - 0.18 * height);
        frameCrop.left = (int) (rect.left + 0.18 * width);
        frameCrop.right = (int) (rect.right - 0.07 * width);
        return frameCrop;
    }

    public Rect getExFrameRectForPic(Rect rect) {
        Rect rc = new Rect(rect);
        int eWidth = (int) (0.02f * rect.width());
        int eHeight = (int) (0.02f * rect.height());
        rc.left -= eWidth;
        rc.right += eWidth;
        rc.top = rc.top - (int) (0.5f * rect.height());
        rc.bottom = rc.bottom - (int) (0.5f * rect.height());
        return rc;
    }

    public Rect getLessFrameRect(Rect rect) {
        Rect rc = new Rect(rect);
        int eWidth = (int) (0.02f * rect.width());
        int eHeight = (int) (0.02f * rect.height());
        rc.left -= eWidth;
        rc.right += eWidth;
        rc.top = rc.top - (int) (0.4f * rect.height());
        rc.bottom = rc.bottom - (int) (0.6f * rect.height());
        return rc;
    }
    public Rect getExFrameRect(Rect rect) {
        Rect rc = new Rect(rect);
        int eWidth = (int) (0.06f * rect.width());
        int eHeight = (int) (0.1f * rect.height());
        rc.left -= eWidth;
        rc.right += eWidth;
        rc.top  -= eHeight;
        rc.bottom += eHeight;
        return rc;
    }

    public Rect getCardFrameRect(Rect rect) {
        Rect rc = new Rect(rect);
        int ex = (int)((rect.height() - 0.64f * rect.width()) * 0.5f);
        rc.left = rect.left;
        rc.right = rect.right;
        rc.top  = rect.top + ex;
        rc.bottom = rect.bottom - ex;
        return rc;
    }

    public void setMaskType(int maskType) {
        this.maskType = maskType;
        invalidate();
    }

    public int getMaskType() {
        return maskType;
    }

    public void setOrientation(@CameraView.Orientation int orientation) {
    }

    public MaskView(Context context) {
        super(context);
        init();
    }

    public MaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        insidePen.setColor(Color.WHITE);
        insidePen.setStyle(Paint.Style.STROKE);
        insidePen.setStrokeWidth(2);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            int width;
            int height;

            if (maskType == MASK_TYPE_BANK_CARD) {

                float ratio = h > w ? 0.9f : 0.64f;

                width = (int) (w * ratio);
                height = (int) (width * 0.64);

                int left = (w - width) / 2;
                int top = (h - height) / 2;
                int right = width + left;
                int bottom = height + top;

                frameCard.left = left;
                frameCard.top = top;
                frameCard.right = right;
                frameCard.bottom = bottom;

                frameInside = getCropFrameRect(frameCard);

            } else if (maskType == MASK_TYPE_MUL_LINE){
                float ratio = h > w ? 0.9f : 0.64f;
                width = (int) (w * ratio);
                height = (int) (width * 0.64);

                int left = (w - width) / 2;
                int top = (h - height) / 2;
                int right = width + left;
                int bottom = height + top;

                frameCard.left = left;
                frameCard.top = top;
                frameCard.right = right;
                frameCard.bottom = bottom;

            }else if (maskType == MASK_TYPE_SINGLE_LINE) {
                float ratio = h > w ? 0.9f : 0.64f;

                width = (int) (w * ratio);
                height = (int) (width * 0.08f);

                int left = (w - width) / 2;
                int top = (h - height) / 2;
                int right = width + left;
                int bottom = height + top;

                frameCard.left = left;
                frameCard.top = top;
                frameCard.right = right;
                frameCard.bottom = bottom;

            } else {

                if (h > w) {
                    width = w;
                    height = width * 620 / 400;
                } else {
                    height = h;
                    width = height * 620 / 400;
                }

                int left = (w - width) / 2;
                int top = (h - height) / 2;
                int right = width + left;
                int bottom = height + top;

                frame.left = left;
                frame.top = top;
                frame.right = right;
                frame.bottom = bottom;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect frame = this.frameCard;

        int width = frame.width();
        int height = frame.height();

        int left = frame.left;
        int top = frame.top;
        int right = frame.right;
        int bottom = frame.bottom;

        canvas.drawColor(maskColor);
        if (maskType != MASK_TYPE_SINGLE_LINE) {
            fillRectRound(left, top, right, bottom, 20, 20, false);
        } else {
            fillRectRound(left, top, right, bottom, 3, 3, false);
        }
        canvas.drawPath(path, pen);
        canvas.drawPath(path, eraser);

        canvas.drawRect(frameInside, insidePen);

    }

    private Path path = new Path();

    private Path fillRectRound(float left, float top, float right, float bottom, float rx, float ry, boolean
            conformToOriginalPost) {

        path.reset();
        if (rx < 0) {
            rx = 0;
        }
        if (ry < 0) {
            ry = 0;
        }
        float width = right - left;
        float height = bottom - top;
        if (rx > width / 2) {
            rx = width / 2;
        }
        if (ry > height / 2) {
            ry = height / 2;
        }
        float widthMinusCorners = (width - (2 * rx));
        float heightMinusCorners = (height - (2 * ry));

        path.moveTo(right, top + ry);
        path.rQuadTo(0, -ry, -rx, -ry);
        path.rLineTo(-widthMinusCorners, 0);
        path.rQuadTo(-rx, 0, -rx, ry);
        path.rLineTo(0, heightMinusCorners);

        if (conformToOriginalPost) {
            path.rLineTo(0, ry);
            path.rLineTo(width, 0);
            path.rLineTo(0, -ry);
        } else {
            path.rQuadTo(0, ry, rx, ry);
            path.rLineTo(widthMinusCorners, 0);
            path.rQuadTo(rx, 0, rx, -ry);
        }

        path.rLineTo(0, -heightMinusCorners);
        path.close();
        return path;
    }

    {
        // 硬件加速不支持，图层混合。
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        pen.setColor(Color.WHITE);
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeWidth(6);

        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    private void capture(File file) {

    }
}
