package com.shuyu.gsyvideoplayer.listener;

public interface StandardVideoAllCallBack extends VideoAllCallBack {

    //点击了空白区域开始播放
    void onClickStartThumb(String url);

    //点击了播放中的空白区域
    void onClickBlank(String url);

    //点击了全屏播放中的空白区域
    void onClickBlankFullscreen(String url);
}
