package com.shuyu.gsyvideoplayer.video;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.rubensousa.previewseekbar.PreviewSeekBar;
import com.shuyu.gsyvideoplayer.GSYTextureView;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.R;
import com.shuyu.gsyvideoplayer.listener.GSYMediaPlayerListener;
import com.shuyu.gsyvideoplayer.listener.VideoAllCallBack;
import com.shuyu.gsyvideoplayer.utils.CommonUtil;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static com.shuyu.gsyvideoplayer.utils.CommonUtil.hideNavKey;
import static com.shuyu.gsyvideoplayer.utils.CommonUtil.hideSupportActionBar;
import static com.shuyu.gsyvideoplayer.utils.CommonUtil.showNavKey;

public abstract class GSYBaseVideoPlayer extends FrameLayout implements GSYMediaPlayerListener {

    @IdRes
    protected static final int FULLSCREEN_ID = 85597;

    protected static long CLICK_QUIT_FULLSCREEN_TIME = 0;

    protected boolean mActionBar = false;//是否需要在利用window实现全屏幕的时候隐藏Action Bar

    protected boolean mStatusBar = false;//是否需要在利用window实现全屏幕的时候隐藏Status Bar

    protected boolean mHideKey = true;//是否隐藏虚拟按键

    protected boolean mNeedShowWifiTip = true; //是否需要显示流量提示

    protected int mCurrentState = -1; //当前的播放状态

    protected int mRotate = 0; //针对某些视频的旋转信息做了旋转处理

    protected int mSystemUiVisibility;

    protected float mSpeed = 1;//播放速度，只支持6.0以上

    protected boolean mIfCurrentIsFullscreen = false;//当前是否全屏

    protected boolean mLockLand = false;//当前全屏是否锁定全屏

    protected boolean mLooping = false;//循环

    protected boolean mHadPlay = false;//是否播放过

    protected Context mContext;

    protected String url; // 视频URL

    protected ViewGroup mTextureViewContainer; //渲染控件父类

    protected VideoAllCallBack mVideoAllCallBack;

    protected Map<String, String> mapHeadData = new HashMap<>();

    protected GSYTextureView mTextureView;

    protected ImageView mCoverImageView; //内部使用，请勿操作哟~

    protected View mStartButton;

    protected PreviewSeekBar mProgressBar;

    protected ImageView mFullscreenButton;

    protected TextView mCurrentTimeTextView, mTotalTimeTextView;

    protected ViewGroup mTopContainer, mBottomContainer;

    protected ImageView mBackButton;

    protected Bitmap mFullPauseBitmap = null;//暂停时的全屏图片；

    protected OrientationUtils mOrientationUtils;

    /**
     * 1.5.0开始加入，如果需要不同布局区分功能，需要重载
     */
    public GSYBaseVideoPlayer(Context context, Boolean fullFlag) {
        super(context);
        mIfCurrentIsFullscreen = fullFlag;
    }

    public GSYBaseVideoPlayer(Context context) {
        super(context);
    }

    public GSYBaseVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.gsy_video_player, 0, 0);
        try {
            mIfCurrentIsFullscreen = a.getBoolean(R.styleable.gsy_video_player_full_mode, false);
        } finally {
            a.recycle();
        }
    }

    public GSYBaseVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    private ViewGroup getViewGroup() {
        return (ViewGroup) (CommonUtil.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
    }

    /**
     * 移除没用的
     */
    private void removeVideo(ViewGroup vp, int id) {
        View old = vp.findViewById(id);
        if (old != null) {
            if (old.getParent() != null) {
                ViewGroup viewGroup = (ViewGroup) old.getParent();
                vp.removeView(viewGroup);
            }
        }
    }

    /**
     * 全屏
     */
    protected void resolveFullVideoShow(Context context, final GSYBaseVideoPlayer gsyVideoPlayer, final FrameLayout frameLayout) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) gsyVideoPlayer.getLayoutParams();
        lp.setMargins(0, 0, 0, 0);
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.CENTER;
        gsyVideoPlayer.setLayoutParams(lp);

        gsyVideoPlayer.setVisibility(VISIBLE);
        frameLayout.setVisibility(VISIBLE);

        if (mVideoAllCallBack != null) {
            Debuger.printfError("onEnterFullscreen");
            mVideoAllCallBack.onEnterFullscreen(url);
        }
    }

    /**
     * 恢复
     */
    protected void resolveNormalVideoShow(View oldF, ViewGroup vp, GSYVideoPlayer gsyVideoPlayer) {

        if (oldF != null && oldF.getParent() != null) {
            ViewGroup viewGroup = (ViewGroup) oldF.getParent();
            vp.removeView(viewGroup);
        }
        mCurrentState = GSYVideoManager.instance().getLastState();
        if (gsyVideoPlayer != null) {
            mCurrentState = gsyVideoPlayer.getCurrentState();
        }
        GSYVideoManager.instance().setListener(GSYVideoManager.instance().lastListener());
        GSYVideoManager.instance().setLastListener(null);
        setStateAndUi(mCurrentState);
        addTextureView();
        CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
        if (mVideoAllCallBack != null) {
            Debuger.printfError("onQuitFullscreen");
            mVideoAllCallBack.onQuitFullscreen(url);
        }

        showNavKey(mContext, mSystemUiVisibility);
    }

    /**
     * 利用window层播放全屏效果
     *
     * @param context
     * @param actionBar 是否有actionBar，有的话需要隐藏
     * @param statusBar 是否有状态bar，有的话需要隐藏
     */
    public GSYBaseVideoPlayer startWindowFullscreen(final Context context, final boolean actionBar, final boolean statusBar) {

        mSystemUiVisibility = ((Activity) context).getWindow().getDecorView().getSystemUiVisibility();

        hideSupportActionBar(context, actionBar, statusBar);

        if (mHideKey) {
            hideNavKey(context);
        }

        this.mActionBar = actionBar;

        this.mStatusBar = statusBar;

        final ViewGroup vp = getViewGroup();

        removeVideo(vp, FULLSCREEN_ID);

        //处理暂停的逻辑
        pauseFullCoverLogic();

        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        boolean hadNewConstructor = true;

        try {
            GSYBaseVideoPlayer.this.getClass().getConstructor(Context.class, Boolean.class);
        } catch (Exception e) {
            hadNewConstructor = false;
        }

        try {
            //通过被重载的不同构造器来选择
            Constructor<GSYBaseVideoPlayer> constructor;
            final GSYBaseVideoPlayer gsyVideoPlayer;
            if (!hadNewConstructor) {
                constructor = (Constructor<GSYBaseVideoPlayer>) GSYBaseVideoPlayer.this.getClass().getConstructor(Context.class);
                gsyVideoPlayer = constructor.newInstance(getContext());
            } else {
                constructor = (Constructor<GSYBaseVideoPlayer>) GSYBaseVideoPlayer.this.getClass().getConstructor(Context.class, Boolean.class);
                gsyVideoPlayer = constructor.newInstance(getContext(), true);
            }

            gsyVideoPlayer.setId(FULLSCREEN_ID);
            gsyVideoPlayer.setVideoAllCallBack(mVideoAllCallBack);
            gsyVideoPlayer.setLooping(isLooping());
            gsyVideoPlayer.setSpeed(getSpeed());
            final FrameLayout.LayoutParams lpParent = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            final FrameLayout frameLayout = new FrameLayout(context);
            frameLayout.setBackgroundColor(Color.BLACK);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(getWidth(), getHeight());
            frameLayout.addView(gsyVideoPlayer, lp);
            vp.addView(frameLayout, lpParent);
            gsyVideoPlayer.setVisibility(INVISIBLE);
            frameLayout.setVisibility(INVISIBLE);
            resolveFullVideoShow(context, gsyVideoPlayer, frameLayout);

            gsyVideoPlayer.mHadPlay = mHadPlay;
            gsyVideoPlayer.mFullPauseBitmap = mFullPauseBitmap;
            gsyVideoPlayer.mNeedShowWifiTip = mNeedShowWifiTip;
            gsyVideoPlayer.setUp(url, mapHeadData);
            gsyVideoPlayer.setStateAndUi(mCurrentState);
            gsyVideoPlayer.addTextureView();

            gsyVideoPlayer.getFullscreenButton().setImageResource(R.drawable.video_shrink);
            gsyVideoPlayer.getFullscreenButton().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearFullscreenLayout();
                }
            });

            gsyVideoPlayer.getBackButton().setVisibility(VISIBLE);
            gsyVideoPlayer.getBackButton().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearFullscreenLayout();
                }
            });

            GSYVideoManager.instance().setLastListener(this);
            GSYVideoManager.instance().setListener(gsyVideoPlayer);
            return gsyVideoPlayer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 只有全屏的播放器会调用这个函数
     * 来自返回按钮和退出全屏按钮
     * 退出window层播放全屏效果
     */
    public void clearFullscreenLayout() {
        if (mOrientationUtils != null) {
            mOrientationUtils.backToPort();
//            mOrientationUtils = null;
        }
        backToNormal();
    }

    /**
     * 回到正常效果
     */
    private void backToNormal() {

        final ViewGroup vp = getViewGroup();

        final View oldF = vp.findViewById(FULLSCREEN_ID);
        final GSYVideoPlayer gsyVideoPlayer;
        if (oldF != null) {
            gsyVideoPlayer = (GSYVideoPlayer) oldF;
            //如果暂停了
            pauseFullBackCoverLogic(gsyVideoPlayer);

            resolveNormalVideoShow(oldF, vp, gsyVideoPlayer);
        } else {
            resolveNormalVideoShow(null, vp, null);
        }
    }

    /**
     * 全屏的暂停的时候返回页面不黑色
     */
    private void pauseFullCoverLogic() {
        if (mCurrentState == GSYVideoPlayer.CURRENT_STATE_PAUSE && mTextureView != null
                && (mFullPauseBitmap == null || mFullPauseBitmap.isRecycled())) {
            try {
                mFullPauseBitmap = mTextureView.getBitmap(mTextureView.getSizeW(), mTextureView.getSizeH());
            } catch (Exception e) {
                e.printStackTrace();
                mFullPauseBitmap = null;
            }
        }
    }

    /**
     * 全屏的暂停返回的时候返回页面不黑色
     */
    private void pauseFullBackCoverLogic(GSYBaseVideoPlayer gsyVideoPlayer) {
        //如果是暂停状态
        if (gsyVideoPlayer.mCurrentState == GSYVideoPlayer.CURRENT_STATE_PAUSE
                && gsyVideoPlayer.mTextureView != null) {
            //全屏的位图还在，说明没播放，直接用原来的
            if (gsyVideoPlayer.mFullPauseBitmap != null
                    && !gsyVideoPlayer.mFullPauseBitmap.isRecycled()) {
                mFullPauseBitmap = gsyVideoPlayer.mFullPauseBitmap;
            } else {
                //不在了说明已经播放过，还是暂停的话，我们拿回来就好
                try {
                    mFullPauseBitmap = mTextureView.getBitmap(mTextureView.getSizeW(), mTextureView.getSizeH());
                } catch (Exception e) {
                    e.printStackTrace();
                    mFullPauseBitmap = null;
                }
            }
        }
    }

    /**
     * 设置播放URL
     *
     * @param url
     * @return
     */
    public abstract boolean setUp(String url);

    /**
     * 设置播放URL
     *
     * @param url
     * @param mapHeadData
     * @return
     */

    public abstract boolean setUp(String url, Map<String, String> mapHeadData);

    /**
     * 设置播放显示状态
     *
     * @param state
     */
    protected abstract void setStateAndUi(int state);

    /**
     * 添加播放的view
     */
    protected abstract void addTextureView();

    protected abstract void onClickUiToggle();

    /**
     * 获取全屏按键
     */
    public abstract ImageView getFullscreenButton();

    /**
     * 获取返回按键
     */
    public abstract ImageView getBackButton();


    public boolean isIfCurrentIsFullscreen() {
        return mIfCurrentIsFullscreen;
    }

    public boolean isLooping() {
        return mLooping;
    }

    /**
     * 设置循环
     */
    public void setLooping(boolean looping) {
        this.mLooping = looping;
    }


    /**
     * 设置播放过程中的回调
     *
     * @param mVideoAllCallBack
     */
    public void setVideoAllCallBack(VideoAllCallBack mVideoAllCallBack) {
        this.mVideoAllCallBack = mVideoAllCallBack;
    }

    public boolean isLockLand() {
        return mLockLand;
    }

    /**
     * 一全屏就锁屏横屏，默认false竖屏，可配合setRotateViewAuto使用
     */
    public void setLockLand(boolean lockLand) {
        this.mLockLand = lockLand;
    }


    public float getSpeed() {
        return mSpeed;
    }

    /**
     * 播放速度
     */
    public void setSpeed(float speed) {
        this.mSpeed = speed;
        if (GSYVideoManager.instance().getMediaPlayer() != null
                && GSYVideoManager.instance().getMediaPlayer() instanceof IjkMediaPlayer) {
            if (speed > 0) {
                ((IjkMediaPlayer) GSYVideoManager.instance().getMediaPlayer()).setSpeed(speed);
            }
        }
    }

    public boolean isHideKey() {
        return mHideKey;
    }

    /**
     * 全屏隐藏虚拟按键，默认打开
     */
    public void setHideKey(boolean hideKey) {
        this.mHideKey = hideKey;
    }

    public boolean isNeedShowWifiTip() {
        return mNeedShowWifiTip;
    }

    /**
     * 是否需要显示流量提示,默认true
     */
    public void setNeedShowWifiTip(boolean needShowWifiTip) {
        this.mNeedShowWifiTip = needShowWifiTip;
    }
}
