package com.zsmarter.ocr.translator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static com.zsmarter.ocr.camera.CameraActivity.CONTENT_TYPE_BANK_CARD;
import static com.zsmarter.ocr.camera.CameraActivity.CONTENT_TYPE_ID_CARD;

public class ImageTranslator {
    private static final String TAG = "ImageTranslator";
    private static String languageDir = "";
    private static ImageTranslator mImageTranslator = null;
    private static final String TRAINEDDATA_SUFFIX = ".traineddata";
    private String translateResult = "";
    public static final String LANGUAGE_NUM = "num";
    public static final String LANGUAGE_CHINESE = "chi_sim";
    public static final String LANGUAGE_ENG = "eng";

    private String mContentType;
    private String mLanguage;

    private int mPageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT;


    private ImageTranslator() {
    }

    public static ImageTranslator getInstance() {
        if (mImageTranslator == null) {
            synchronized (ImageTranslator.class) {
                if (mImageTranslator == null) {
                    mImageTranslator = new ImageTranslator();
                }
            }
        }
        return mImageTranslator;
    }

    public void setContentType(String contentType) {
        mContentType = contentType;
    }

    public void setLanguage(String language) {
        mLanguage = language;
    }

    public void setmPageSegMode(int mPageSegMode) {
        this.mPageSegMode = mPageSegMode;
    }

    /**
     * 扫描结果回调用
     */
    public interface TesseractCallback {
        void onStart(Bitmap bitmap);

        void onResult(String result);

        void onFail(String reason);
    }

    public void translate(Context context, final String filePath, final TesseractCallback callBack) {
        Bitmap bmp = null;
        try {
            FileInputStream fis = new FileInputStream(filePath);
            bmp = BitmapFactory.decodeStream(fis);
            translate(context, bmp, callBack);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void translate(Context context, Bitmap bmp, final TesseractCallback callBack) {
        checkLanguage(context, mLanguage);
        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        String tessdataPath = languageDir.substring(0, languageDir.length() - "tessdata/".length());
        Log.d(TAG, "translate: tessdataPath : " + tessdataPath);
        if (baseApi.init(tessdataPath, mLanguage, TessBaseAPI.OEM_CUBE_ONLY)) {
            //设置识别模式
            baseApi.setPageSegMode(mPageSegMode);
            if (bmp != null) {


                /**
                 * 以下为自定义图像预处理实现
                */
                //调整亮度
//                int bright = ImageUtil.getBright(bmp);
//                if (bright < 160) {
//                    bmp = ImageUtil.brightenBitmap(bmp, 160 - bright);
//                    //调整对比度
//                    bmp = ImageUtil.changeContrast(bmp, 1.1f);
//                }
//                //图像灰度化
//                bmp = ImageUtil.bitmap2dGray(bmp);
//                //中值滤波
//                bmp = ImageUtil.medianFilter(bmp, 3, 3);
//                //锐化
//                bmp = ImageUtil.sharpenImageAmeliorate(bmp);
//                //二值化
//                bmp = ImageUtil.gray2Binary(bmp);
//                bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);

                //图片预处理后回调
//                callBack.onStart(bmp);

                //设置要识别的图片
                baseApi.setImage(bmp);
                baseApi.setVariable(TessBaseAPI.VAR_SAVE_BLOB_CHOICES, TessBaseAPI.VAR_TRUE);
                //开始识别
                String result = baseApi.getUTF8Text();
                baseApi.clear();
                baseApi.end();
//                bmp.recycle();
                switch (mContentType) {
                    case CONTENT_TYPE_BANK_CARD:
                        if (filterResult2(result)) {
                            callBack.onResult(translateResult);
                        } else {
                            callBack.onFail("ocr translate failed: " + result);
                        }
                        break;
                    case CONTENT_TYPE_ID_CARD:
                        if (filterResultEmpty(result)) {
                            callBack.onResult(translateResult);
                        } else {
                            callBack.onFail("ocr translate failed: " + result);
                        }
                        break;
                    default:
                        if (filterResultEmpty(result)) {
                            callBack.onResult(translateResult);
                        } else {
                            callBack.onFail("ocr translate failed: " + result);
                        }
                        break;
                }

            } else {
                callBack.onFail("bitmap is null");
            }
        } else {
            callBack.onFail("TessBaseAPI init failed");
        }
    }

    private boolean filterResultEmpty(String result) {
        if (!TextUtils.isEmpty(result)) {
            translateResult = result;
            return true;
        }
        return false;
    }

    private boolean filterResult2(String result) {
        Log.d(TAG, "filterResult2: result : " + result);
        if (result != null && (result.contains("\n") || result.contains("\r\n"))) {
            if (result.contains("\n")) {
                Log.d(TAG, "result.contains('n')");
            }
            if (result.contains("\r\n")) {
                Log.d(TAG, "result.contains('rn')");
            }
            String[] strings = result.split("\\n");
            Log.d(TAG, "filterResult2: strings : " + Arrays.toString(strings));
            if (strings != null) {
                for (String str : strings) {
                    if (str != null && str.length() > 19) {
                        if (str.contains("\r")) {
                            Log.d(TAG, "result.contains('r')");
                            str = str.replace("\r", "");
                            Log.d(TAG, "str : " + str);
                        }
                        return filterRow(str);
                    }
                }
            }
            return false;
        } else {
            return filterRow(result);
        }
    }

    private boolean filterRow(String string) {
        String[] nums = string.split(" ");
        Log.d(TAG, "nums : " + Arrays.toString(nums));
        if (nums != null && nums.length >= 2) {
            for (int i = 0; i < nums.length - 1; i++) {
                if (nums[i].length() == 6 && nums[i + 1].length() == 13) {
                    translateResult = nums[i] + nums[i + 1];
                    Log.d(TAG, "translateResult : " + translateResult);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 筛选扫描结果
     */
    private boolean filterResultForIDCard(String result) {
        Log.d(TAG, "filterResult2: result : " + result);
        if (result != null && (result.contains("\n") || result.contains("\r\n"))) {
            if (result.contains("\n")) {
                Log.d(TAG, "result.contains('n')");
            }
            if (result.contains("\r\n")) {
                Log.d(TAG, "result.contains('rn')");
            }
            String[] strings = result.split("\\n");
            Log.d(TAG, "filterResult2: strings : " + Arrays.toString(strings));
            if (strings != null) {
                for (String str : strings) {
                    if (str != null && str.length() >= 18) {
                        if (str.contains("\r")) {
                            Log.d(TAG, "result.contains('r')");
                            str = str.replace("\r", "");
                            Log.d(TAG, "str : " + str);
                        }
                        return filterRowForIDCard(str);
                    }
                }
            }
            return false;
        } else {
            return filterRowForIDCard(result);
        }
    }

    private boolean filterRowForIDCard(String string) {
        String[] nums = string.split(" ");
        Log.d(TAG, "nums : " + Arrays.toString(nums));
        if (nums != null) {
            for (String num : nums) {
                if (num.length() == 18) {
                    translateResult = num;
                    Log.d(TAG, "translateResult : " + translateResult);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查语言包
     */
    private void checkLanguage(Context context, String language) {
//        languageDir = "/sdcard/aaa/tessdata/";
        languageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/tessdata/";

        String tessdata = languageDir + language + ".traineddata";

        File file = new File(tessdata);
        if (!file.exists()) {
            copyTraineddata(context, languageDir, tessdata, language);

        }

        if ("chi_sim".equals(language) || "chi_tra".equals(language)) {
            String tessdata_vert = languageDir + language + "_vert.traineddata";

            File file_vert = new File(tessdata_vert);
            if (!file_vert.exists()) {
                copyTraineddata(context, languageDir, tessdata_vert, language + "_vert");
            }
        }
    }

    private void copyTraineddata(Context context, String filePath, String sdCardFile, String language) {
        InputStream inputStream;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.mkdirs();
            }

            if (!file.isDirectory()) {
                file.delete();
                file.mkdirs();
            }
            inputStream = context.getResources().getAssets().open(language + TRAINEDDATA_SUFFIX);
            readInputStream(sdCardFile, inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readInputStream(String storageFile, InputStream inputStream) {
        File file = new File(storageFile);
        FileOutputStream fos = null;
        try {
            if (!file.exists()) {

                fos = new FileOutputStream(file);

                byte[] buffer = new byte[inputStream.available()];

                int lenght = 0;
                while ((lenght = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, lenght);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


}
