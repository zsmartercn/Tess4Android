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

import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

public interface ICameraControl {

    int FLASH_MODE_OFF = 0;

    int FLASH_MODE_TORCH = 1;

    int FLASH_MODE_AUTO = 2;

    @IntDef({FLASH_MODE_TORCH, FLASH_MODE_OFF, FLASH_MODE_AUTO})
    @interface FlashMode {

    }


    interface OnTakePictureCallback {
        void onPictureTaken(byte[] data);
    }

    void setDetectCallback(boolean isScan, OnDetectPictureCallback callback);

    interface OnDetectPictureCallback {
        void onDetect(byte[] data, int rotation);
    }

    void start();

    void stop();

    void pause();

    void resume();

    View getDisplayView();

    Rect getPreviewFrame();

    void takePicture(OnTakePictureCallback callback);

    void setPermissionCallback(PermissionCallback callback);

    void setDisplayOrientation(@CameraView.Orientation int displayOrientation);

    void refreshPermission();

    AtomicBoolean getAbortingScan();

    void setFlashMode(@FlashMode int flashMode);

    @FlashMode
    int getFlashMode();

    void setFocusState(boolean focusState);

    boolean getFocusState();
}
