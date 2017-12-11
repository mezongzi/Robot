package com.dq.robot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class WeiXinService extends AccessibilityService {

    private static final String TAG = "WeiXinService";

    private static WeiXinService service;

    private Handler mHandler = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service destory");
        service = null;
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "service interrupt");
        Toast.makeText(this, "中断服务", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        service = this;
        Toast.makeText(this, "已连接服务", Toast.LENGTH_SHORT).show();
    }

    //发送消息操作中
    private boolean isSendMsg = false;

    //同意好友请求操作中
    private boolean isAddFriend = false;

    //同意好友请求成功
    private boolean isAddSuccess = false;

    //好友请求总申请数量
    private int newFriendNum = 0;

    //好友请求已同意数量
    private int newFriendIndex = 0;

    //执行返回操作
    private boolean isBack = false;

    //微信号
    private String wxNoStr;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (BuildConfig.DEBUG) {
            //Log.e(TAG, "事件--->" + event);
        }

        String pkn = String.valueOf(event.getPackageName());
        if ("com.tencent.mm".equals(pkn)) {
            final AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (nodeInfo == null) {
                Log.w(TAG, "rootWindow为空");
                return;
            }

            if (isBack) {
                String homeWx = "com.tencent.mm:id/ble";
                AccessibilityNodeInfo targetHomeWx = AccessibilityHelper.findNodeInfosById(nodeInfo, homeWx);
                //执行返回主页面操作
                if (targetHomeWx == null || targetHomeWx.getText() == null ||
                        !"微信".equals(targetHomeWx.getText().toString().trim())) {
                    execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                    return;
                }
            }

            String dialogCancelId = "com.tencent.mm:id/a_q";
            AccessibilityNodeInfo targetCancel = AccessibilityHelper.findNodeInfosById(nodeInfo, dialogCancelId);
            String dialogConfirmId = "com.tencent.mm:id/a_r";
            AccessibilityNodeInfo targetConfirm = AccessibilityHelper.findNodeInfosById(nodeInfo, dialogConfirmId);
            //微信是否安装新版本提示
            if (targetCancel != null && targetConfirm != null) {
                if (targetCancel.getText().toString().trim().contains("取消")) {
                    AccessibilityHelper.performClick(targetCancel);
                } else if (targetConfirm.getText().toString().trim().contains("是")) {
                    AccessibilityHelper.performClick(targetConfirm);
                }
                return;
            }

            if (isSendMsg) {
                //聊天界面---右上角图片
                String chattingId = "com.tencent.mm:id/f5";
                AccessibilityNodeInfo targetChatting = AccessibilityHelper.findNodeInfosById(nodeInfo, chattingId);
                //详细资料界面---微信号
                String wxNoId = "com.tencent.mm:id/ab_";
                AccessibilityNodeInfo targetWxNo = AccessibilityHelper.findNodeInfosById(nodeInfo, wxNoId);
                if (targetChatting != null) {
                    if (StringUtil.isBlank(wxNoStr)) {
                        //聊天界面对方头像
                        String chattingOtherId = "com.tencent.mm:id/i8";
                        AccessibilityNodeInfo targetChattingOther = AccessibilityHelper.findNodeInfosByIdDesc(nodeInfo, chattingOtherId);
                        if (targetChattingOther != null) {
                            //可以看到头像
                            AccessibilityHelper.performClick(targetChattingOther);
                        } else {
                            //看不到头像
                        }
                    } else {
                        isSendMsg = false;
                        //聊天界面对方发的消息
                        String otherMsgId = "com.tencent.mm:id/i_";
                        AccessibilityNodeInfo targetMsg = AccessibilityHelper.findNodeInfosByIdDesc(nodeInfo, otherMsgId);
                        if (targetMsg != null && targetMsg.getText() != null) {
                            Log.e(TAG, "other:" + targetMsg.getText().toString().trim());
                        }
                        //给对方发消息
                        getHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                String sendEtId = "com.tencent.mm:id/a2_";
                                AccessibilityNodeInfo targetSendEt = AccessibilityHelper.findNodeInfosById(nodeInfo, sendEtId);
                                AccessibilityHelper.performClick(targetSendEt);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                //String msg = "hello word !!!";
                                String msg = "这是一段汉字";
                                exeCmd(String.format("am broadcast -a ADB_INPUT_TEXT --es msg \"%s\"", msg));
                                Log.e(TAG, "send msg: " + msg);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                //发送按钮
                                String wxSendId = "com.tencent.mm:id/a2f";
                                AccessibilityNodeInfo targetSend = AccessibilityHelper.findNodeInfosById(nodeInfo, wxSendId);
                                if (targetSend != null) {
                                    AccessibilityHelper.performClick(targetSend);
                                }
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                            }
                        }, 100);
                    }
                } else if (targetWxNo != null) {
                    if (StringUtil.isBlank(wxNoStr)) {
                        //获取微信号
                        wxNoStr = targetWxNo.getText().toString().trim().replace("微信号: ", "");
                        Log.e(TAG, "wxNoStr:" + wxNoStr);
                        execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                        if ("weixin".equals(wxNoStr)) {
                            getHandler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                                }
                            }, 500);
                        }
                    }
                } else {
                    //腾讯新闻
                    String wxNewId = "android:id/text1";
                    AccessibilityNodeInfo targetNew = AccessibilityHelper.findNodeInfosById(nodeInfo, wxNewId);
                    if (targetNew != null && targetNew.getText() != null && targetNew.getText().toString().trim().equals("腾讯新闻")) {
                        isSendMsg = false;
                        execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                    }
                }
            } else if (isAddFriend) {
                //新的朋友小红点
                String newFriendRedId = "com.tencent.mm:id/ar1";
                AccessibilityNodeInfo targetNewFriendRed = AccessibilityHelper.findNodeInfosById(nodeInfo, newFriendRedId);
                //新的朋友icon
                String newFriendImgId = "com.tencent.mm:id/hx";
                AccessibilityNodeInfo targetNewFriendImg = AccessibilityHelper.findNodeInfosById(nodeInfo, newFriendImgId);
                //同意按钮
                String agreeId = "com.tencent.mm:id/ara";
                AccessibilityNodeInfo targetAgree = AccessibilityHelper.findNodeInfosById(nodeInfo, agreeId);
                List<AccessibilityNodeInfo> targetAgreeList = AccessibilityHelper.findNodeInfosByIds(nodeInfo, agreeId);
                //详细资料
                String infoId = "android:id/text1";
                AccessibilityNodeInfo targetInfo = AccessibilityHelper.findNodeInfosById(nodeInfo, infoId);
                //weixin
                String weixinId = "com.tencent.mm:id/ble";
                AccessibilityNodeInfo targetWeixin = AccessibilityHelper.findNodeInfosById(nodeInfo, weixinId);
                if (targetNewFriendRed != null) {
                    if (!isAddSuccess)
                        AccessibilityHelper.performClick(targetNewFriendRed);
                } else if (targetAgree != null) {
                    if (!isAddSuccess) {
                        if (newFriendNum == 0) {
                            if (targetAgreeList != null) {
                                newFriendNum = targetAgreeList.size();
                            }
                        }
                        newFriendIndex++;
                        isAddSuccess = true;
                        AccessibilityHelper.performClick(targetAgree);
                    }
                } else if (isAddSuccess && targetInfo != null && targetInfo.getText() != null &&
                        "详细资料".equals(targetInfo.getText().toString().trim())) {
                    isAddSuccess = false;
                    execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                } else if (targetNewFriendImg != null) {
                    if (targetWeixin != null && targetWeixin.getText() != null) {
                        isAddFriend = false;
                        execShellCmd(String.format("input tap  %s %s", getRandom(20, 260), getRandom(1780, 1900)));
                    }
                    if (newFriendIndex == newFriendNum) {
                        //全部好友都添加了
                        execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                    }
                }
            } else {
                //列表小红点
                String listRedId = "com.tencent.mm:id/hy";
                final AccessibilityNodeInfo targetListRed = AccessibilityHelper.findNodeInfosById(nodeInfo, listRedId);
                //底部小红点
                String bottomRedId = "com.tencent.mm:id/blc";
                final AccessibilityNodeInfo targetBottomRed = AccessibilityHelper.findNodeInfosById(nodeInfo, bottomRedId);
                if (targetListRed != null) {
                    isSendMsg = true;
                    //获取微信号作为唯一标示，为空则去获取，有默认值则不去获取
                    wxNoStr = null;
                    Log.w(TAG, "聊天界面有消息了");
                    //有列表小红点
                    getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AccessibilityHelper.performClick(targetListRed);
                        }
                    }, 1000);
                } else if (targetBottomRed != null) {
                    isAddFriend = true;
                    getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            newFriendNum = 0;
                            newFriendIndex = 0;
                            AccessibilityHelper.performClick(targetBottomRed);
                        }
                    }, 1000);
                } else {
                    //详细资料界面
                    String sendMsgId = "com.tencent.mm:id/ab4";
                    AccessibilityNodeInfo targetSendMsg = AccessibilityHelper.findNodeInfosById(nodeInfo, sendMsgId);
                    if (targetSendMsg != null) {
                        String homeWx = "com.tencent.mm:id/ble";
                        AccessibilityNodeInfo targetHomeWx = AccessibilityHelper.findNodeInfosById(nodeInfo, homeWx);
                        if (targetHomeWx == null || targetHomeWx.getText() == null ||
                                !"微信".equals(targetHomeWx.getText().toString().trim())) {
                            execShellCmd(String.format("input tap  %s %s", getRandom(5, 80), getRandom(80, 130)));
                        }
                    }
                }
            }
        }

    }

    public String getRandom(int min, int max) {
        Random random = new Random();
        int s = random.nextInt(max) % (max - min + 1) + min;
        return String.valueOf(s);
    }

    /**
     * 执行shell命令
     *
     * @param cmd
     */
    private void execShellCmd(String cmd) {
        try {
            // 申请获取root权限，这一步很重要，不然会没有作用
            Process process = Runtime.getRuntime().exec("su");
            // 获取输出流
            OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(
                    outputStream);
            dataOutputStream.writeBytes(cmd);
            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Shell命令封装类
     *
     * @param cmd Shell命令
     */
    public static void exeCmd(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec("su");

            OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(
                    outputStream);
            byte[] t_utf8 = (cmd + "\n").getBytes("UTF-8");
            dataOutputStream.write(t_utf8);
            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();
        } catch (Throwable t) {
            Log.e("test", "execCommonShell[ " + cmd + " ] error.", t);
        }
    }

    private void clickId(AccessibilityNodeInfo nodeInfo, String id) {
        AccessibilityNodeInfo targetHomeAdd = AccessibilityHelper.findNodeInfosById(nodeInfo, id);
        if (targetHomeAdd != null) {
            Toast.makeText(this, id + " is find", Toast.LENGTH_SHORT).show();
            AccessibilityHelper.performClick(targetHomeAdd);
        }
    }

    private void clickIds(AccessibilityNodeInfo nodeInfo, String id) {
        List<AccessibilityNodeInfo> targetHomeAdd = AccessibilityHelper.findNodeInfosByIds(nodeInfo, id);
        if (targetHomeAdd != null) {
            for (int i = 0; i < targetHomeAdd.size(); i++) {
                if (targetHomeAdd.get(i) != null)
                    AccessibilityHelper.performClick(targetHomeAdd.get(i));
            }
        }
    }

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    /**
     * 判断当前服务是否正在运行
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isRunning() {
        if (service == null) {
            return false;
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) service.getSystemService(Context.ACCESSIBILITY_SERVICE);
        AccessibilityServiceInfo info = service.getServiceInfo();
        if (info == null) {
            return false;
        }
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_GENERIC);
        Iterator<AccessibilityServiceInfo> iterator = list.iterator();

        boolean isConnect = false;
        while (iterator.hasNext()) {
            AccessibilityServiceInfo i = iterator.next();
            if (i.getId().equals(info.getId())) {
                isConnect = true;
                break;
            }
        }
        if (!isConnect) {
            return false;
        }
        return true;
    }

    /**
     * 快速读取通知栏服务是否启动
     */
    public static boolean isNotificationServiceRunning() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }
        //部份手机没有NotificationService服务
        return false;
    }


}