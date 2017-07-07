package com.shuyu.gsyvideoplayer.listener;

/**
 * Created by Nathen
 * On 2016/04/04 22:13
 */
public interface VideoAllCallBack {

    //加载成功
    void onPrepared(String url);

    //点击了开始按键播放
    void onClickStartIcon(String url);

    //点击了错误状态下的开始按键
    void onClickStartError(String url);

    //点击了播放状态下的开始按键--->停止
    void onClickStop(String url);

    //点击了全屏播放状态下的开始按键--->停止
    void onClickStopFullscreen(String url);

    //点击了暂停状态下的开始按键--->播放
    void onClickResume(String url);

    //点击了全屏暂停状态下的开始按键--->播放
    void onClickResumeFullscreen(String url);

    //点击了空白弹出seekbar
    void onClickSeekbar(String url);

    //点击了全屏的seekbar
    void onClickSeekbarFullscreen(String url);

    //播放完了
    void onAutoComplete(String url);

    //进去全屏
    void onEnterFullscreen(String url);

    //退出全屏
    void onQuitFullscreen(String url);

    //触摸调整声音
    void onTouchScreenSeekVolume(String url);

    //触摸调整进度
    void onTouchScreenSeekPosition(String url);

    //触摸调整亮度
    void onTouchScreenSeekLight(String url);

    //播放错误
    void onPlayError(String url);

}
