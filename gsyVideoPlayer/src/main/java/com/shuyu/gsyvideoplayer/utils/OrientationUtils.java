package com.shuyu.gsyvideoplayer.utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.shuyu.gsyvideoplayer.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.R;
import com.shuyu.gsyvideoplayer.video.GSYBaseVideoPlayer;

/**
 * 处理屏幕旋转的的逻辑
 * Created by shuyu on 2016/11/11.
 */

public class OrientationUtils {
    private static final String TAG = "OrientationUtils";

    private Activity activity;
    private GSYBaseVideoPlayer gsyVideoPlayer;

    private int currentScreenType;
    private int preferredLandScreenType = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

    /**
     * @param activity
     * @param gsyVideoPlayer
     */
    public OrientationUtils(Activity activity, GSYBaseVideoPlayer gsyVideoPlayer) {
        this.activity = activity;
        this.gsyVideoPlayer = gsyVideoPlayer;
        this.currentScreenType = getScreenOrientation();
    }

    // 切换横屏和竖屏
    public void togglePortAndLand() {
        if (currentScreenType == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            currentScreenType = preferredLandScreenType;
            activity.setRequestedOrientation(currentScreenType);
            gsyVideoPlayer.getFullscreenButton().setImageResource(R.drawable.video_shrink);
        } else {
            currentScreenType = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            activity.setRequestedOrientation(currentScreenType);
            gsyVideoPlayer.getFullscreenButton().setImageResource(R.drawable.video_enlarge);
        }
    }

    // 强制切换回竖屏
    public void backToPort() {
        currentScreenType = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        activity.setRequestedOrientation(currentScreenType);
        gsyVideoPlayer.getFullscreenButton().setImageResource(R.drawable.video_enlarge);
    }

    // 切换横屏和反向横屏
    public void toggleLandReverse() {
        if (currentScreenType == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            preferredLandScreenType = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        } else {
            preferredLandScreenType = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        currentScreenType = preferredLandScreenType;
        activity.setRequestedOrientation(currentScreenType);
    }

    public void setPreferredLandScreenType(int preferredLandScreenType) {
        this.preferredLandScreenType = preferredLandScreenType;
    }

    private int getScreenOrientation() {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                            "portrait.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                            "landscape.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }
}
