package com.example.admin.mybledemo;

import android.app.Application;

import com.orhanobut.logger.Logger;
/**
 * 应用入口
 * @author yzz
 * Created on 2017/11/6 16:24
 */

public class MyApplication extends Application {

    private static MyApplication mApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        mApplication = this;
        //修改打印的tag值
        Logger.init("yzz");

    }

    public static MyApplication getInstance() {
        return mApplication;
    }

}
