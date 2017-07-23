package com.yichudu.virtualstick.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.yichudu.virtualstick.R;
import com.yichudu.virtualstick.tool.VirtualStickApplication;

import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseProduct;

/**
 * 闪屏页
 */
public class SplishActivity extends Activity {

    private DJIBaseProduct mProduct = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置为全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splish);
        try {
            mProduct = VirtualStickApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                jumpToIntent();
            }
        }, 1000 * 2);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        jumpToIntent();
        return super.onTouchEvent(event);
    }

    //跳转函数
    public void jumpToIntent(){
        if (null == mProduct || !mProduct.isConnected()) {
           //如果未连接，跳转到连接步骤界面
           Intent intent = new Intent(SplishActivity.this, ConnectActivity.class);
            startActivity(intent);
            //跳转结束后结束当前界面
            finish();
        } else {
            //如果已连接，跳转到操作界面
            Intent intent = new Intent(SplishActivity.this,VirtualStickActivity.class);
            startActivity(intent);
            //跳转结束后结束当前界面
            finish();
        }
    }

    @Override
    protected void onResume() {
        /**
         * 设置为横屏
         */
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onResume();
    }
}
