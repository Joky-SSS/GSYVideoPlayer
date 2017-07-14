package com.shuyu.gsyvideoplayer;


import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Surface;

import com.shuyu.gsyvideoplayer.listener.GSYMediaPlayerListener;
import com.shuyu.gsyvideoplayer.model.GSYModel;
import com.shuyu.gsyvideoplayer.model.VideoOptionModel;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.utils.Debuger;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import tv.danmaku.ijk.media.player.AbstractMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 视频管理，单例
 * 使用IjkMediaPlayer
 * Created by shuyu on 2016/11/11.
 */
public class GSYVideoManager implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener, IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnInfoListener {

    public static String TAG = "GSYVideoManager";

    public static final int HANDLER_PREPARE = 0;
    public static final int HANDLER_SET_DISPLAY = 1;
    public static final int HANDLER_RELEASE = 2;

    private AbstractMediaPlayer mediaPlayer;
    private HandlerThread mediaHandlerThread;
    private MediaHandler mediaHandler;
    private Handler mainThreadHandler;

    private WeakReference<GSYMediaPlayerListener> listener;
    private WeakReference<GSYMediaPlayerListener> lastListener;

    private List<VideoOptionModel> optionModelList;//配置ijk option

    private String playTag = ""; //播放的tag，防止错位置，因为普通的url也可能重复

    private int currentVideoWidth = 0; //当前播放的视频宽的高

    private int currentVideoHeight = 0; //当前播放的视屏的高

    private int lastState;//当前视频的最后状态

    private int playPosition = -22; //播放的tag，防止错位置，因为普通的url也可能重复

    private boolean needMute = false; //是否需要静音

    private int bufferPoint = 0;

    private static class SingletonHolder {
        private static final GSYVideoManager INSTANCE = new GSYVideoManager();
    }

    public static final GSYVideoManager instance() {
        return SingletonHolder.INSTANCE;
    }

    public GSYMediaPlayerListener listener() {
        if (listener == null)
            return null;
        return listener.get();
    }

    public GSYMediaPlayerListener lastListener() {
        if (lastListener == null)
            return null;
        return lastListener.get();
    }

    public void setListener(GSYMediaPlayerListener listener) {
        if (listener == null)
            this.listener = null;
        else
            this.listener = new WeakReference<>(listener);
    }

    public void setLastListener(GSYMediaPlayerListener lastListener) {
        if (lastListener == null)
            this.lastListener = null;
        else
            this.lastListener = new WeakReference<>(lastListener);
    }

    private GSYVideoManager() {
        mediaPlayer = new IjkMediaPlayer();
        mediaHandlerThread = new HandlerThread(TAG);
        mediaHandlerThread.start();
        mediaHandler = new MediaHandler((mediaHandlerThread.getLooper()));
        mainThreadHandler = new Handler();
    }

    public class MediaHandler extends Handler {
        public MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    initVideo(msg);
                    break;
                case HANDLER_SET_DISPLAY:
                    showDisplay(msg);
                    break;
                case HANDLER_RELEASE:
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                    }
                    setNeedMute(false);
                    break;
            }
        }
    }

    private void initVideo(Message msg) {
        try {
            currentVideoWidth = 0;
            currentVideoHeight = 0;
            mediaPlayer.release();

            initIJKPlayer(msg);

            setNeedMute(needMute);
            mediaPlayer.setOnCompletionListener(GSYVideoManager.this);
            mediaPlayer.setOnBufferingUpdateListener(GSYVideoManager.this);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnPreparedListener(GSYVideoManager.this);
            mediaPlayer.setOnSeekCompleteListener(GSYVideoManager.this);
            mediaPlayer.setOnErrorListener(GSYVideoManager.this);
            mediaPlayer.setOnInfoListener(GSYVideoManager.this);
            mediaPlayer.setOnVideoSizeChangedListener(GSYVideoManager.this);
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initIJKPlayer(Message msg) {
        mediaPlayer = new IjkMediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            if (GSYVideoType.isMediaCodec()) {
                Debuger.printfLog("enable mediaCodec");
                ((IjkMediaPlayer) mediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-avc", 1);
                ((IjkMediaPlayer) mediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
                ((IjkMediaPlayer) mediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
            }
            ((IjkMediaPlayer) mediaPlayer).setDataSource(((GSYModel) msg.obj).getUrl(), ((GSYModel) msg.obj).getMapHeadData());
            mediaPlayer.setLooping(((GSYModel) msg.obj).isLooping());
            if (((GSYModel) msg.obj).getSpeed() != 1 && ((GSYModel) msg.obj).getSpeed() > 0) {
                ((IjkMediaPlayer) mediaPlayer).setSpeed(((GSYModel) msg.obj).getSpeed());
            }
            initIJKOption((IjkMediaPlayer) mediaPlayer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initIJKOption(IjkMediaPlayer ijkMediaPlayer) {
        if (optionModelList != null && optionModelList.size() > 0) {
            for (VideoOptionModel videoOptionModel : optionModelList) {
                if (videoOptionModel.getValueType() == VideoOptionModel.VALUE_TYPE_INT) {
                    ijkMediaPlayer.setOption(videoOptionModel.getCategory(),
                            videoOptionModel.getName(), videoOptionModel.getValueInt());
                } else {
                    ijkMediaPlayer.setOption(videoOptionModel.getCategory(),
                            videoOptionModel.getName(), videoOptionModel.getValueString());
                }
            }
        }
    }


    private void showDisplay(Message msg) {
        if (msg.obj == null && mediaPlayer != null) {
            mediaPlayer.setSurface(null);
        } else {
            Surface holder = (Surface) msg.obj;
            if (mediaPlayer != null && holder.isValid()) {
                mediaPlayer.setSurface(holder);
            }
        }
    }


    public void prepare(final String url, final Map<String, String> mapHeadData, boolean loop, float speed) {
        if (TextUtils.isEmpty(url)) return;
        Message msg = new Message();
        msg.what = HANDLER_PREPARE;
        GSYModel fb = new GSYModel(url, mapHeadData, loop, speed);
        msg.obj = fb;
        mediaHandler.sendMessage(msg);
    }

    public void releaseMediaPlayer() {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE;
        mediaHandler.sendMessage(msg);
        playTag = "";
        playPosition = -22;
    }

    public void setDisplay(Surface holder) {
        Message msg = new Message();
        msg.what = HANDLER_SET_DISPLAY;
        msg.obj = holder;
        mediaHandler.sendMessage(msg);
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onPrepared();
                }
            }
        });
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onAutoCompletion();
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, final int percent) {
        bufferPoint = percent;
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onBufferingUpdate(percent);
                }
            }
        });
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onSeekComplete();
                }
            }
        });
    }

    @Override
    public boolean onError(IMediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onError(what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onInfo(what, extra);
                }
            }
        });
        return false;
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        currentVideoWidth = mp.getVideoWidth();
        currentVideoHeight = mp.getVideoHeight();
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onVideoSizeChanged();
                }
            }
        });
    }

    /**
     * 暂停播放
     */
    public static void onPause() {
        if (GSYVideoManager.instance().listener() != null) {
            GSYVideoManager.instance().listener().onVideoPause();
        }
    }

    /**
     * 恢复播放
     */
    public static void onResume() {
        if (GSYVideoManager.instance().listener() != null) {
            GSYVideoManager.instance().listener().onVideoResume();
        }
    }


    public AbstractMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public int getCurrentVideoWidth() {
        return currentVideoWidth;
    }

    public int getCurrentVideoHeight() {
        return currentVideoHeight;
    }

    public int getLastState() {
        return lastState;
    }

    public void setLastState(int lastState) {
        this.lastState = lastState;
    }

    public void setCurrentVideoHeight(int currentVideoHeight) {
        this.currentVideoHeight = currentVideoHeight;
    }

    public void setCurrentVideoWidth(int currentVideoWidth) {
        this.currentVideoWidth = currentVideoWidth;
    }

    public String getPlayTag() {
        return playTag;
    }

    public void setPlayTag(String playTag) {
        this.playTag = playTag;
    }

    public int getPlayPosition() {
        return playPosition;
    }

    public void setPlayPosition(int playPosition) {
        this.playPosition = playPosition;
    }


    public List<VideoOptionModel> getOptionModelList() {
        return optionModelList;
    }

    /**
     * 设置IJK视频的option
     */
    public void setOptionModelList(List<VideoOptionModel> optionModelList) {
        this.optionModelList = optionModelList;
    }

    public boolean isNeedMute() {
        return needMute;
    }

    /**
     * 是否需要静音
     */
    public void setNeedMute(boolean needMute) {
        this.needMute = needMute;
        if (mediaPlayer != null) {
            if (needMute) {
                mediaPlayer.setVolume(0,0);
            } else {
                mediaPlayer.setVolume(1, 1);
            }
        }
    }

    public int getBufferPoint() {
        return bufferPoint;
    }
}