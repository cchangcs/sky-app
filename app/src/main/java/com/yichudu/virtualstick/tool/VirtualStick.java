package com.yichudu.virtualstick.tool;


import java.util.Timer;
import java.util.TimerTask;

import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

/**
 * 实现对无人机的操作
 */

public class VirtualStick {

    float rollPitchSpeed= (float) (DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity*0.2);
    float verticalSpeed= (float) (DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity*0.2);
    float yawSpeed=(float) (DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngle*0.5);
    DJIFlightController flightController;
    float mPitch=0,mRoll=0,mYaw=0,mThrottle=1.5f;
    private Timer mSendVirtualStickDataTimer;
    MyTimerTask taskReference;
    public boolean isEnable;
    public static long count=0;


    public VirtualStick() {
    }

    public void setAircraft(DJIBaseProduct djiAircraft){
        if(djiAircraft==null){

            return;
        }
        flightController=((DJIAircraft)djiAircraft).getFlightController();
        if(flightController!=null) {

        }

    }//setAircraft


    //实现数据透传
    public void getDataFromExternalDevice() {
        DJIFlightControllerDelegate.FlightControllerReceivedDataFromExternalDeviceCallback flightControllerReceivedDataFromExternalDeviceCallback = new DJIFlightControllerDelegate.FlightControllerReceivedDataFromExternalDeviceCallback() {
            @Override
            public void onResult(byte[] bytes) {
                VirtualStickApplication.logInfo("数据透传成功");
            }
        };
        if(flightController != null){
            flightController.setReceiveExternalDeviceDataCallback(flightControllerReceivedDataFromExternalDeviceCallback);
        }
    }


    /**
     * 实现计算角速度
     */
    //用于记录上一个偏角
    private static double oldRatation;
    //获取角速度
    public String getRatation(){
        if(flightController != null){
            DJIFlightControllerDataType.DJIFlightControllerCurrentState currentState = flightController.getCurrentState();
            double newRatation;
            double yaw = currentState.getAttitude().yaw;
            if (currentState != null) {
                if(oldRatation == 0 && yaw != 0){
                    oldRatation = newRatation = yaw;
                }else{
                    newRatation = yaw;
                }
                double result =  (newRatation-oldRatation)/0.2;
                oldRatation = newRatation;
                return ((double)Math.round(result*100))/100+"";
            }
        }
        return null;
    }

    //获取姿态数据
    public String getYaw(){
        if(flightController != null){
            DJIFlightControllerDataType.DJIFlightControllerCurrentState currentState = flightController.getCurrentState();
            if (currentState != null) {
                return "pitch:"+currentState.getAttitude().pitch+"  Yaw:"+currentState.getAttitude().yaw
                        +"  Roll:"+currentState.getAttitude().roll;
            }
        }
        return null;
    }


    //获取GPS信号
    public String getGPS(){
        if(flightController != null){
            DJIFlightControllerDataType.DJIFlightControllerCurrentState currentState = flightController.getCurrentState();
            if (currentState != null) {
                  return currentState.getGpsSignalStatus().value()+"";
            }
        }
        return null;
    }

    //获取飞行器高度
    public String getHeight(){
        if(flightController != null){
            DJIFlightControllerDataType.DJIFlightControllerCurrentState currentState = flightController.getCurrentState();
            if (currentState != null) {
                return currentState.getAircraftLocation().getAltitude() + "";
            }
        }
        return null;
    }


    //获取前后速度
    public String getX(){
        if(flightController != null){
        DJIFlightControllerDataType.DJIFlightControllerCurrentState currentState = flightController.getCurrentState();
            if (currentState != null) {
                currentState.getGpsSignalStatus();
                return currentState.getVelocityX() + "";
            }
        }

        return null;
    }

    //获取左右速度
    public String getY(){
        if(flightController != null){
            DJIFlightControllerDataType.DJIFlightControllerCurrentState currentState = flightController.getCurrentState();
            if (currentState != null) {
                return currentState.getVelocityY() + "";
            }
        }
        return null;
    }

    //获取上下速度
    public String getZ(){
        if(flightController != null){
            DJIFlightControllerDataType.DJIFlightControllerCurrentState currentState = flightController.getCurrentState();
            if (currentState != null) {
                return currentState.getVelocityZ() + "";
            }
        }
        return null;
    }

    //获取无人机的控制权限
    public void enable(){
        //enableVirtualStickControlMode
        flightController.enableVirtualStickControlMode(
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {

                        }
                    }
                }
        );



        if(mSendVirtualStickDataTimer==null) {
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(new MyTimerTask(this), 100, 200);
        }
        isEnable=true;
        VirtualStickApplication.logInfo("已获取对无人的控制权限！");
    }

    /**
     * 释放无人机的控制权限
     */
    public void disable(){
        isEnable=false;
        flightController.disableVirtualStickControlMode(
                new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null)
                        {

                        }
                    }
                }
        );
        if(taskReference!=null) {
            taskReference.cancel();
            taskReference=null;
        }
        if(mSendVirtualStickDataTimer!=null) {
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer=null;
        }
        VirtualStickApplication.logInfo("已释放对无人的控制权限！");
    }

    //实现无人机的起飞操作
    public void takeOff(){
       if(flightController instanceof dji.sdk.FlightController.c){
           dji.sdk.FlightController.c tmp=(dji.sdk.FlightController.c)flightController;
           tmp.takeOff(new DJIBaseComponent.DJICompletionCallback() {
               @Override
               public void onResult(DJIError djiError) {
                   if (djiError != null)
                       VirtualStickApplication.logError("TakeOff error:", djiError);
               }
           });
           VirtualStickApplication.logInfo("无人机开始起飞！");
        }
    }

    /**
     * 实现无人机的降落
     */
    public void land(){
        if(flightController instanceof dji.sdk.FlightController.c){
            dji.sdk.FlightController.c tmp=(dji.sdk.FlightController.c)flightController;
            tmp.autoLanding(new DJIBaseComponent.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null)
                                        VirtualStickApplication.logError("AutoLanding error:", djiError);
                                }
                            }
            );
            VirtualStickApplication.logInfo("无人机开始降落！");
        }
    }

    /**
     * 实现无人机的飞行控制
     */
    public void leftUpClick(){
        if(mThrottle<6)
            mThrottle++;
    }
    public void leftRightClick(){
        mYaw= yawSpeed;
    }
    public void leftDownClick(){
        if(mThrottle>=1)
             mThrottle--;

    }
    public void leftLeftClick(){
        mYaw= -yawSpeed;
    }

    //right
    public void rightUpClick(){
        mRoll =rollPitchSpeed;
    }
    public void rightRightClick(){
        mPitch =rollPitchSpeed;
    }
    public void rightDownClick(){
        mRoll =-rollPitchSpeed;
    }
    public void rightLeftClick(){
        mPitch=-rollPitchSpeed;
    }

    //发送透传数据
    public void sendDataToOnboard(byte[] data){
        if(flightController != null){
            if(flightController.isOnboardSDKDeviceAvailable()){
                flightController.sendDataToOnboardSDKDevice(data, new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if(djiError!=null) {
                            VirtualStickApplication.logError("SendDataToOnBoard error:", djiError);
                        }else{
                            VirtualStickApplication.logInfo("发送指令成功！");
                        }
                    }
                });
            }
        }else{
            VirtualStickApplication.logInfo("遥控器未连接！");
        }
    }

    //用于向无人机发送控制参数
    public void run() {
        flightController.sendVirtualStickFlightControlData(
                new DJIFlightControllerDataType.DJIVirtualStickFlightControlData(
                        mPitch, mRoll, mYaw, mThrottle
                ), new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if(djiError!=null)
                            VirtualStickApplication.logError("SendControlData error:",djiError );
                    }
                }
        );
        if(count++%3==0) {
            mPitch = mRoll = mYaw = 0;
        }
    }
}
class MyTimerTask  extends TimerTask{
    private VirtualStick master;
    public MyTimerTask(VirtualStick  master){
        this.master=master;
        master.taskReference=this;
    }
    @Override
    public void run(){
        master.run();
    }
}