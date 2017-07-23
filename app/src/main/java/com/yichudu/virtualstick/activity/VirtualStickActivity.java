package com.yichudu.virtualstick.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.yichudu.virtualstick.R;
import com.yichudu.virtualstick.tool.VirtualStick;
import com.yichudu.virtualstick.tool.VirtualStickApplication;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.AirLink.DJILBAirLink;
import dji.sdk.AirLink.DJILBAirLink.DJIOnReceivedVideoCallback;
import dji.sdk.Battery.DJIBattery;
import dji.sdk.Battery.DJIBattery.DJIBatteryState;
import dji.sdk.Camera.DJICamera;
import dji.sdk.Camera.DJICamera.CameraReceivedVideoDataCallback;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIBaseProduct.Model;
import dji.sdk.base.DJIError;

/**
 * 无人机的操作界面
 */

public class VirtualStickActivity extends Activity implements SurfaceTextureListener, View.OnTouchListener,View.OnClickListener{
    //用于记录第一次点击返回按钮时间
   private long exitTime = 0;
    long count=0;
    public Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
           if(msg.what==0x123){
           }
        }
    };
    private static final String TAG = VirtualStickActivity.class.getName();

    private static final int INTERVAL_LOG = 300;
    private static long mLastTime = 0l;

    //视频回调
    protected CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;
    protected DJIOnReceivedVideoCallback mOnReceivedVideoCallback = null;

    //无人机实例
    private DJIBaseProduct mProduct = null;

    //控制类
    private VirtualStick virtualStick = null;
    private DJICamera mCamera = null;
    // 视频编码
    protected DJICodecManager mCodecManager = null;
    private DJIFlightControllerDelegate.FlightControllerReceivedDataFromExternalDeviceCallback mRecvCallback = null;
    //视频显示控件
    protected TextureView mVideoSurface = null;
    //左侧无人机飞行控制按钮
    private Button leftUpButton, leftRightButton, leftDownButton, leftLeftButton;
    //右侧无人机飞行控制按钮
    private Button rightUpButton, rightRightButton, rightDownButton, rightLeftButton;
    //获取释放权限、起飞降落按钮
    private Button enableButton,disableButton,takeOffButton,landButton;
    //发送透传数据
    private Button sendData;
    private TextView dataText;
   // private  mRecvDataCallBack = null;
    //实现下拉框
    private Spinner spinner;
    private ArrayAdapter<String> adapter;
  //private DJIDrone djiDrone;
    //画矩形框
//    private DrawImageView div;

    //显示信息
//    private Button showInfo;
    //显示无人机数据
    private TextView tvSpeedUpdown;//上下速度
    private TextView tvSpeedLeftRight;//左右速度
    private TextView tvSpeedBackForeward;//前后速度
    private TextView tvHeight;//高度
    private TextView tvBattery;//剩余电量
    private TextView tvYaw;//姿态
    private TextView tvGPS;//GPS信号

    //调用定时刷新函数
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            refresh();
        }
    };

    private static boolean entry = true;


    //刷新数据函数
    private void refresh() {
        {
            try {
                mProduct = VirtualStickApplication.getProductInstance();
            } catch (Exception exception) {
                mProduct = null;
            }
            if(mProduct != null && mProduct.isConnected()) {
                //获取无人机的剩余电量，以百分比的形式返回
                try{
                    mProduct.getBattery().setBatteryStateUpdateCallback(new DJIBattery.DJIBatteryStateUpdateCallback() {
                        @Override
                        public void onResult(DJIBatteryState djiBatteryState) {
                            if(djiBatteryState == null){
                                VirtualStickApplication.logInfo("电量为空");
                            }else{
                                StringBuffer mBuffer = new StringBuffer();
                                mBuffer.append(djiBatteryState.getBatteryEnergyRemainingPercent());
                                tvBattery.setText(mBuffer.toString()+"%");
                                if(djiBatteryState.getBatteryEnergyRemainingPercent() != 0 && djiBatteryState.getBatteryEnergyRemainingPercent() <= 30){
                                      if(entry){
                                          AlertDialog.Builder builder =  new  AlertDialog.Builder(VirtualStickActivity.this);
                                          builder.setTitle("警告" )
                                                  .setMessage("电池电量不足！" )
                                                  .setPositiveButton("确认" ,  null )
                                                  .setNegativeButton("取消" , null)
                                                  .show();
                                          entry = false;
                                      }
                                }else{
                                    entry = true;
                                }
                            }
                        }
                    });
                }catch(Exception e){
                    Toast.makeText(VirtualStickActivity.this,"1"+e.toString(),Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

                //初始化virtualStick
                initVirtualStick();

                //获取飞行器所在高度
                try {
                    tvHeight.setText(virtualStick.getHeight()+"m");
                }catch(Exception e){
                    Toast.makeText(VirtualStickActivity.this,"2"+e.toString(),Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

                /*
                获取飞行的前后左右上下的速度
                 */
                //前后速度
                try{
                    tvSpeedBackForeward.setText("x："+virtualStick.getX());
                }catch(Exception e){
                    Toast.makeText(VirtualStickActivity.this,"3"+e.toString(),Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

                //左右速度
                try{
                    tvSpeedLeftRight.setText("y:"+virtualStick.getY());
                }catch(Exception e){
                    Toast.makeText(VirtualStickActivity.this,"4"+e.toString(),Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

                //上下速度
                try{
                    tvSpeedUpdown.setText("z:"+virtualStick.getZ()+"   ω:"+virtualStick.getRatation());
                }catch(Exception e){
                    Toast.makeText(VirtualStickActivity.this,"5"+e.toString(),Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

                //获取无人机姿态
                try{
                    tvYaw.setText(virtualStick.getYaw());
                }catch (Exception e){
                    e.printStackTrace();
                }

                //GPS信号
                try{
                    tvGPS.setText(virtualStick.getGPS());
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    //实现定时刷新
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            while(true){
                try {
                    //sleep2秒
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mHandler.sendMessage(mHandler.obtainMessage());
            }
        }
    };

    public VirtualStickActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置为全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_virtualstick);
//        setContentView(new MyView(this));
        //初始化主界面中的控件
        initUI();
        //初始化并设置spinner下拉框控件
        adapter =new ArrayAdapter<String>(VirtualStickActivity.this,android.R.layout.simple_spinner_dropdown_item,getDataSource());
        spinner.setAdapter(adapter);

        //设置视频的回调
        mReceivedVideoDataCallBack = new CameraReceivedVideoDataCallback() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                count++;
                if (mCodecManager != null) {
                        mCodecManager.sendDataToDecoder(videoBuffer, size);
                } else {
                    Log.e(TAG, "mCodecManager is null");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(VirtualStickApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        //实现定时刷新功能
        new Thread(mRunnable).start();

    }//onCreate

    //对下拉框中的数据进行初始化
    public List<String> getDataSource(){

        List<String> list = new ArrayList<String>();
        //在下拉框中添加项
        list.add("Start Mission1:"+101);
        list.add("Start Mission2:"+102);
        list.add("Start Mission3:"+103);
        list.add("Start Mission4:"+104);
        list.add("Shutdown:"+107);
        return list;
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        /**
         * 设置为横屏
         */
        if(getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onResume();
        //对回传的视频进行初始化
        initVideo();
        if (mVideoSurface == null){
             Log.e(TAG, "mVideoSurface is null");
        }


    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /**
     * 初始化界面中的控件
     */
    private void initUI() {

        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
        //发送数据
        spinner = (Spinner)findViewById(R.id.spinner);
        sendData = (Button) findViewById(R.id.send_button);
        dataText = (TextView) findViewById(R.id.data_text);
        sendData.setOnClickListener(this);
        //

        //获取释放权限、起飞降落按钮初始化
        enableButton=(Button) findViewById(R.id.enableButton);disableButton=(Button) findViewById(R.id.disableButton);takeOffButton=(Button) findViewById(R.id.takeOffButton);landButton=(Button) findViewById(R.id.landButton);
        enableButton.setOnClickListener(this); disableButton.setOnClickListener(this);takeOffButton.setOnClickListener(this);landButton.setOnClickListener(this);


        //无人机飞行控制按钮
        leftUpButton = (Button) findViewById(R.id.leftUpButton);  leftRightButton = (Button) findViewById(R.id.leftRightButton); leftDownButton = (Button) findViewById(R.id.leftDownButton); leftLeftButton = (Button) findViewById(R.id.leftLeftButton);
        rightUpButton = (Button) findViewById(R.id.rightUpButton);  rightRightButton = (Button) findViewById(R.id.rightRightButton); rightDownButton = (Button) findViewById(R.id.rightDownButton);   rightLeftButton = (Button) findViewById(R.id.rightLeftButton);
        //bind sticks' onClick event
        leftUpButton.setOnTouchListener(this); leftRightButton.setOnTouchListener(this); leftLeftButton.setOnTouchListener(this); leftDownButton.setOnTouchListener(this);
        rightUpButton.setOnTouchListener(this); rightRightButton.setOnTouchListener(this); rightDownButton.setOnTouchListener(this); rightLeftButton.setOnTouchListener(this);

        //绘制矩形框
//        div = (DrawImageView) findViewById(R.id.paint_iv);
//        div.draw(new Canvas());

        //显示信息
//        showInfo = (Button) findViewById(R.id.show_info);
//        showInfo.setOnClickListener(this);

        //信息显示控件
        tvSpeedLeftRight = (TextView)findViewById(R.id.tv_speed_leftright);
        tvSpeedBackForeward = (TextView)findViewById(R.id.tv_speed_backforeward);
        tvSpeedUpdown = (TextView)findViewById(R.id.tv_speed_updown);
        tvHeight = (TextView)findViewById(R.id.tv_height);
        tvBattery = (TextView)findViewById(R.id.tv_battery);
        tvYaw = (TextView)findViewById(R.id.tv_yaw);
        tvGPS = (TextView)findViewById(R.id.tv_gps);

    }

    /**
     * 对视频进行初始化
     */
    private void initVideo() {
        try {
            mProduct = VirtualStickApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }

        if (null == mProduct || !mProduct.isConnected()) {
            mCamera = null;
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }

            if (!mProduct.getModel().equals(Model.UnknownAircraft)) {
                mCamera = mProduct.getCamera();
                if (mCamera != null) {
                    mCamera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
                }
            } else {
                if (null != mProduct.getAirLink()) {
                    if (null != mProduct.getAirLink().getLBAirLink()) {
                        // Set the callback
                        mProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(mOnReceivedVideoCallback);
                        mProduct.getAirLink().getLBAirLink().setFPVQualityLatency(DJILBAirLink.LBAirLinkFPVVideoQualityLatency.LowLatency,new DJIBaseComponent.DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null)
                                    VirtualStickApplication.logError("setFPVQualityLatency（）error:", djiError);
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * 初始化控制类virtualStick
     */
    private void initVirtualStick() {
        try {
            mProduct = VirtualStickApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }
        //virtual stick
        virtualStick=new VirtualStick();
        virtualStick.setAircraft(mProduct);
    }

    private void uninitPreviewer() {
        try {
            mProduct = VirtualStickApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }

        if (null == mProduct || !mProduct.isConnected()) {
            mCamera = null;
        } else {
            if (!mProduct.getModel().equals(Model.UnknownAircraft)) {
                mCamera = mProduct.getCamera();
                if (mCamera != null) {
                    // Set the callback
                    mCamera.setDJICameraReceivedVideoDataCallback(null);

                }
            } else {
                if (null != mProduct.getAirLink()) {
                    if (null != mProduct.getAirLink().getLBAirLink()) {
                        // Set the callback
                        mProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(null);
                    }
                }
            }
        }
    }

    //
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            Log.e(TAG, "mCodecManager is null 2");
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }



    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }


    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureUpdated");
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initVideo();
            initVirtualStick();

            VirtualStickApplication.logInfo("已连接遥控器");
        }
    };

    //onClick事件
    @Override
    public void onClick(View v) {
        try{
            switch(v.getId()){
                case R.id.enableButton: {
                    virtualStick.enable();
                    break;
                }
                case R.id.disableButton: {
                    virtualStick.disable();
                    break;
                }

                case R.id.takeOffButton: {

                    virtualStick.takeOff();
                    byte[] d = new byte[4];
                    d[0] = 2;
                    try{
                        virtualStick.sendDataToOnboard(d);
                    } catch (Exception e) {
                        Toast.makeText(VirtualStickActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }


                    break;
                }
                case R.id.landButton: {
                    virtualStick.land();
                    break;
                }
                case R.id.send_button: {
                    //用于实现向onBoard SDK发送数据
                    try {
                        String data = spinner.getSelectedItem().toString();
                        int index = data.indexOf(':');
                        String result ="0" +data.substring(index+1);
                        try {
                            mProduct = VirtualStickApplication.getProductInstance();
                        } catch (Exception exception) {
                            mProduct = null;
                        }
                        if(mProduct != null && mProduct.isConnected()){
                            virtualStick.sendDataToOnboard(result.getBytes("iso-8859-1"));
                        }else{
                            VirtualStickApplication.logInfo("请连接遥控器！");
                        }
                    } catch (Exception e) {
                        Toast.makeText(VirtualStickActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    PopupWindow popupWindow = null;

    //显示信息框
    private void showPopupWindow(View view){
        //一个自定义的布局，作为显示的内容
        //对布局里面的TextView进行赋值

        try {
            mProduct = VirtualStickApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }
        if(mProduct != null && mProduct.isConnected()) {
            //获取无人机的剩余电量，以百分比的形式返回
           try{
               mProduct.getBattery().setBatteryStateUpdateCallback(new DJIBattery.DJIBatteryStateUpdateCallback() {
                   @Override
                   public void onResult(DJIBatteryState djiBatteryState) {
                       if(djiBatteryState == null){
                           VirtualStickApplication.logInfo("电量为空");
                       }else{
                           StringBuffer mBuffer = new StringBuffer();
                           mBuffer.append(djiBatteryState.getBatteryEnergyRemainingPercent()).append("%\n");
                           VirtualStickApplication.logInfo(djiBatteryState.getBatteryEnergyRemainingPercent()+"");
                           tvBattery.setText(mBuffer.toString());
                           mBuffer.delete(0,mBuffer.length());
                       }
                   }
               });
           }catch(Exception e){
               Toast.makeText(VirtualStickActivity.this,"1"+e.toString(),Toast.LENGTH_LONG).show();
               e.printStackTrace();
           }
            //获取飞行器所在高度
            try {
                tvHeight.setText(virtualStick.getHeight());
            }catch(Exception e){
                  Toast.makeText(VirtualStickActivity.this,"2"+e.toString(),Toast.LENGTH_LONG).show();
                  e.printStackTrace();
             }
            //获取飞行的前后左右上下的速度
            //前后速度
            try{
            tvSpeedBackForeward.setText(virtualStick.getX());
            }catch(Exception e){
                Toast.makeText(VirtualStickActivity.this,"3"+e.toString(),Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            try{
            tvSpeedLeftRight.setText(virtualStick.getY());
            }catch(Exception e){
                Toast.makeText(VirtualStickActivity.this,"4"+e.toString(),Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            try{
            tvSpeedUpdown.setText(virtualStick.getZ());
            }catch(Exception e){
                Toast.makeText(VirtualStickActivity.this,"5"+e.toString(),Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

        }

//        //初始化PopupWindow
//        popupWindow = new PopupWindow(contentView,
//                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT
//                ,true);
//
//        popupWindow.setTouchable(true);
//
//        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                Toast.makeText(VirtualStickActivity.this,"已加载",Toast.LENGTH_SHORT).show();
//                return false;
//                //这里如果返回true的话，touch事件将会被拦截
//                //拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
//            }
//        });
//
//        //如果不设置PopupWindow的背景，无论点击外部区域还是back键都无法dismiss弹窗
//        //popupWindow.setBackgroundDrawable();
//
//        //设置好参数之后再show
//        popupWindow.setBackgroundDrawable(new BitmapDrawable());
//        popupWindow.showAsDropDown(view);
    }



    //onTouch事件
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        try {
            switch (v.getId()) {


                case R.id.leftUpButton: {
                        virtualStick.leftUpClick();

                    break;
                }
                case R.id.leftRightButton: {
                        virtualStick.leftRightClick();

                    break;
                }
                case R.id.leftDownButton: {
                        virtualStick.leftDownClick();

                    break;
                }
                case R.id.leftLeftButton: {
                        virtualStick.leftLeftClick();

                    break;
                }

                //right
                case R.id.rightUpButton: {

                    virtualStick.rightUpClick();
                    break;
                }
                case R.id.rightRightButton: {
                        virtualStick.rightRightClick();

                    break;
                }
                case R.id.rightDownButton: {
                    virtualStick.rightDownClick();
                    break;
                }
                case R.id.rightLeftButton: {
                    virtualStick.rightLeftClick();
                    break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    //实现点击两次back键退出应用程序
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void exit(){
        if((System.currentTimeMillis() -exitTime) > 2000){
            Toast.makeText(getApplicationContext(),"再按一次退出应用程序",
                    Toast.LENGTH_SHORT).show();;
            exitTime = System.currentTimeMillis();
        }else{
            finish();
            System.exit(0);
        }
    }

}