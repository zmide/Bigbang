package com.forfan.bigbang.component.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.forfan.bigbang.BigBangApp;
import com.forfan.bigbang.R;
import com.forfan.bigbang.component.activity.BigBangActivity;
import com.forfan.bigbang.component.activity.KeepAliveActivity;
import com.forfan.bigbang.component.activity.floatviewwhitelist.AppListAdapter;
import com.forfan.bigbang.component.activity.setting.SettingActivity;
import com.forfan.bigbang.component.activity.whitelist.SelectionDbHelper;
import com.forfan.bigbang.copy.CopyActivity;
import com.forfan.bigbang.copy.CopyNode;
import com.forfan.bigbang.util.ArcTipViewController;
import com.forfan.bigbang.util.ConstantUtil;
import com.forfan.bigbang.util.KeyPressedTipViewController;
import com.forfan.bigbang.util.LogUtil;
import com.forfan.bigbang.util.RunningTaskUtil;
import com.forfan.bigbang.util.ToastUtil;
import com.forfan.bigbang.util.UrlCountUtil;
//import com.forfan.bigbang.util.XposedEnableUtil;
import com.shang.commonjar.contentProvider.SPHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC;
import static com.forfan.bigbang.component.activity.setting.MonitorSettingCard.SPINNER_ARRAY;


public class BigBangMonitorService extends AccessibilityService {

    private static final String TAG="BigBangMonitorService";

    private static final int TYPE_VIEW_CLICKED=AccessibilityEvent.TYPE_VIEW_CLICKED;
    private static final int TYPE_VIEW_LONG_CLICKED=AccessibilityEvent.TYPE_VIEW_LONG_CLICKED;
    private static final int TYPE_VIEW_DOUBLD_CLICKED=3;
    private static final int TYPE_VIEW_NONE=0;
    public static final String ACCESSIBILITY_ENABLED = "settings put secure accessibility_enabled 1";
    public  int double_click_interval = ConstantUtil.DEFAULT_DOUBLE_CLICK_INTERVAL;

    private CharSequence mWindowClassName;

    private boolean monitorClick = true;
    private boolean showFloatView = true;
    private boolean onlyText = true;
    private boolean isRun;

    private int qqSelection = TYPE_VIEW_LONG_CLICKED;
    private int weixinSelection = TYPE_VIEW_LONG_CLICKED;
    private int otherSelection = TYPE_VIEW_LONG_CLICKED;

    private boolean hasShowTipToast;
    private boolean hasShowTooShortToast;

    private Handler handler;
    private Map<String,Integer> selections;
    private String mCurrentPackage;
    private int mCurrentType;

    private AccessibilityServiceInfo mAccessibilityServiceInfo;

    private List<String> floatWhiteList;
    private RunningTaskUtil mRunningTaskUtil;

    String back ;
    String home ;
    String recent ;

    @Override
    public void onCreate() {
        super.onCreate();
        back = getVitualNavigationKey(this, "accessibility_back", "com.android.systemui", "");
        home = getVitualNavigationKey(this, "accessibility_home", "com.android.systemui", "");
        recent = getVitualNavigationKey(this, "accessibility_recent", "com.android.systemui", "");
        readSettingFromSp();

        ArcTipViewController.getInstance().addActionListener(actionListener);

        mRunningTaskUtil=new RunningTaskUtil(this);

        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(ConstantUtil.BROADCAST_BIGBANG_MONITOR_SERVICE_MODIFIED);
        intentFilter.addAction(ConstantUtil.REFRESH_WHITE_LIST_BROADCAST);
        intentFilter.addAction(ConstantUtil.FLOAT_REFRESH_WHITE_LIST_BROADCAST);
        intentFilter.addAction(ConstantUtil.UNIVERSAL_COPY_BROADCAST);
        intentFilter.addAction(ConstantUtil.UNIVERSAL_COPY_BROADCAST_DELAY);
        intentFilter.addAction(ConstantUtil.SCREEN_CAPTURE_OVER_BROADCAST);
        intentFilter.addAction(ConstantUtil.EFFECT_AFTER_REBOOT_BROADCAST);
        intentFilter.addAction(ConstantUtil.MONITOR_CLICK_BROADCAST);
        registerReceiver(bigBangBroadcastReceiver,intentFilter);




        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenReceiver,filter);

        handler=new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    startService(new Intent(BigBangMonitorService.this,ListenClipboardService.class));
                    if (showFloatView){
                        String packageName;
                        if (!TextUtils.isEmpty(mCurrentPackage)){
                            packageName=mCurrentPackage;
                        }else {
                            ComponentName task = mRunningTaskUtil.getTopActivtyFromLolipopOnwards();
                            packageName = task.getPackageName();
                        }
                        if (floatWhiteList!=null&&floatWhiteList.contains(packageName)) {
                            ArcTipViewController.getInstance().remove();
                        }else {
                            if (ArcTipViewController.getInstance().isRemoved()) {
                                ArcTipViewController.getInstance().showHideFloatImageView();
                            }else {
                                ArcTipViewController.getInstance().show();
                            }
                        }
                    }
                    keepAccessibilityOpen();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                handler.postDelayed(this,3000);
            }
        });
        mAccessibilityServiceInfo=new AccessibilityServiceInfo();
        mAccessibilityServiceInfo.feedbackType=FEEDBACK_GENERIC;
        mAccessibilityServiceInfo.eventTypes=AccessibilityEvent.TYPE_VIEW_CLICKED|AccessibilityEvent.TYPE_VIEW_LONG_CLICKED|AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        int flag=0;
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            flag=flag|AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        }
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR2){
            flag=flag|AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        }
        mAccessibilityServiceInfo.flags=flag;
        mAccessibilityServiceInfo.notificationTimeout=100;
        setServiceInfo(mAccessibilityServiceInfo);

        readWhiteList();
        readFloatWhiteList();

        keepAccessibilityOpen();

    }

    @Override
    public void onDestroy() {
        ArcTipViewController.getInstance().removeActionListener(actionListener);
        ArcTipViewController.getInstance().remove();
        try {
            unregisterReceiver(bigBangBroadcastReceiver);
            unregisterReceiver(mScreenReceiver);
        } catch (Throwable e) {
        }
        super.onDestroy();
    }

    private ArcTipViewController.ActionListener actionListener=new ArcTipViewController.ActionListener() {
        @Override
        public void isShow(boolean isShow) {
            isRun=isShow;
            int text = isShow ? R.string.bigbang_open: R.string.bigbang_close;
            ToastUtil.show(text);
        }

        @Override
        public boolean longPressed() {
            Intent intent=new Intent(BigBangMonitorService.this, SettingActivity.class);
            intent.addFlags(intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        setServiceInfo(mAccessibilityServiceInfo);
    }

    public static String getVitualNavigationKey(Context paramContext, String paramString1, String paramString2, String paramString3)
    {
        try
        {
            Resources packageManager = paramContext.getPackageManager().getResourcesForApplication(paramString2);
            String key = packageManager.getString(packageManager.getIdentifier(paramString1, "string", paramString2));
            return key;
        }
        catch (PackageManager.NameNotFoundException e) {}
        return paramString3;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //虚拟手机按键处理，优先级高于是否点击分词的判断
        if ((event.getEventType() == TYPE_VIEW_LONG_CLICKED) && ("com.android.systemui".equals(event.getPackageName())))
        {
            if (TextUtils.isEmpty(event.getContentDescription())){
                return;
            }
            //长按虚拟机触发的，需要转到按键处理去
            if (!TextUtils.isEmpty(back) && event.getContentDescription().equals(back)){
                KeyPressedTipViewController.getInstance().onKeyLongPress(KeyEvent.KEYCODE_BACK);
            }else if (!TextUtils.isEmpty(home) && event.getContentDescription().equals(home)){
                KeyPressedTipViewController.getInstance().onKeyLongPress(KeyEvent.KEYCODE_HOME);
            }else if (!TextUtils.isEmpty(recent) && event.getContentDescription().equals(recent)){
                KeyPressedTipViewController.getInstance().onKeyLongPress(KeyEvent.KEYCODE_APP_SWITCH);
            }
        }
        if (!isRun){
            return;
        }
        LogUtil.d(TAG,"onAccessibilityEvent:"+event);
        int type=event.getEventType();
        switch (type){
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                mWindowClassName = event.getClassName();
                mCurrentPackage = event.getPackageName()==null?"":event.getPackageName().toString();
                Integer selectType=selections.get(mCurrentPackage);
                mCurrentType = selectType==null?TYPE_VIEW_NONE:(selectType+1);
                if ("com.tencent.mm.plugin.sns.ui.SnsTimeLineUI".equals(mWindowClassName)){
                    setCapabilities(true);
                }else {
                    setCapabilities(false);
                }
                break;
            case TYPE_VIEW_CLICKED:
            case TYPE_VIEW_LONG_CLICKED:
                getText(event);
                break;
        }
    }

    private void setCapabilities(boolean isPengYouQuan) {
        int flag= 0;
        flag = mAccessibilityServiceInfo.flags;
        if (isPengYouQuan) {
            mAccessibilityServiceInfo.flags=flag | (AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS);
        }else {
            mAccessibilityServiceInfo.flags=flag & (~AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS);
        }
        this.setServiceInfo(mAccessibilityServiceInfo);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent paramKeyEvent) {
        KeyPressedTipViewController.getInstance().onKeyEvent(paramKeyEvent);
        return false;
    }


    @Override
    public void onInterrupt() {
        Log.e(TAG,"onInterrupt");
    }

    private synchronized void getText(AccessibilityEvent event){
//        if(XposedEnableUtil.isEnable())
//            return;
        LogUtil.d(TAG,"getText:"+event);
        if (!monitorClick || event==null ) {
            return;
        }
        if (showFloatView && !isRun) {
            return;
        }
        int type=getClickType(event);
        CharSequence className = event.getClassName();
        if (mWindowClassName==null){
            return;
        }
        if (mWindowClassName.toString().startsWith("com.forfan.bigbang")){
            //自己的应用不监控
            return;
        }
        if (mCurrentPackage.equals(event.getPackageName())){
            if (type!=mCurrentType){
                //点击方式不匹配，直接返回
                return;
            }
        }else {
            //包名不匹配，直接返回
            return;
        }
        if (className==null || className.equals("android.widget.EditText")){
            //输入框不监控
            return;
        }
        if (onlyText){
            //onlyText方式下，只获取TextView的内容
            if (className==null || !className.equals("android.widget.TextView")){
                if (!hasShowTipToast){
                    ToastUtil.show(R.string.toast_tip_content);
                    hasShowTipToast=true;
                }
                return;
            }
        }
        AccessibilityNodeInfo info=event.getSource();
        if(info==null){
            return;
        }
        CharSequence txt=info.getText();
        if (TextUtils.isEmpty(txt) && !onlyText){
            //非onlyText方式下获取文字更多，但是可能并不是想要的文字
            //比如系统短信页面需要这样才能获取到内容。
            List<CharSequence> txts=event.getText();
            if (txts!=null) {
                StringBuilder sb=new StringBuilder();
                for (CharSequence t : txts) {
                    sb.append(t);
                }
                txt=sb.toString();
            }
        }
        if (!TextUtils.isEmpty(txt)) {
            if (txt.length()<=2 ){
                //对于太短的词进行屏蔽，因为这些词往往是“发送”等功能按钮，其实应该根据不同的activity进行区分
                if (!hasShowTooShortToast) {
                    ToastUtil.show(R.string.too_short_to_split);
                    hasShowTooShortToast = true;
                }
                return;
            }
            Intent intent=new Intent(this, BigBangActivity.class);
            intent.addFlags(intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(BigBangActivity.TO_SPLIT_STR,txt.toString());
//            startActivity(intent);
            //放到ArcTipViewController中触发试试
            ArcTipViewController.getInstance().showTipViewForStartActivity(intent);
        }
    }


    private Method getSourceNodeIdMethod;
    private long mLastSourceNodeId;
    private long mLastClickTime;

    private long getSourceNodeId(AccessibilityEvent event)  {
        //用于获取点击的View的id，用于检测双击操作
        if (getSourceNodeIdMethod==null) {
            Class<AccessibilityEvent> eventClass = AccessibilityEvent.class;
            try {
                getSourceNodeIdMethod = eventClass.getMethod("getSourceNodeId");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        if (getSourceNodeIdMethod!=null) {
            try {
                return (long) getSourceNodeIdMethod.invoke(event);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    private int getClickType(AccessibilityEvent event){
        int type = event.getEventType();
        long time = event.getEventTime();
        long id=getSourceNodeId(event);
        if (type!=TYPE_VIEW_CLICKED){
            mLastClickTime=time;
            mLastSourceNodeId=-1;
            return type;
        }
        if (id==-1){
            mLastClickTime=time;
            mLastSourceNodeId=-1;
            return type;
        }
        if (type==TYPE_VIEW_CLICKED && time - mLastClickTime<= double_click_interval && id==mLastSourceNodeId){
            mLastClickTime=-1;
            mLastSourceNodeId=-1;
            return TYPE_VIEW_DOUBLD_CLICKED;
        }else {
            mLastClickTime=time;
            mLastSourceNodeId=id;
            return type;
        }
    }



    private int retryTimes = 0;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void UniversalCopy() {
        boolean isSuccess=false;
        labelOut: {
            AccessibilityNodeInfo rootInActiveWindow = this.getRootInActiveWindow();
            if(retryTimes < 10) {
                String packageName;
                if(rootInActiveWindow != null) {
                    packageName = String.valueOf(rootInActiveWindow.getPackageName());
                } else {
                    packageName = null;
                }

                if(rootInActiveWindow == null || packageName != null && packageName.contains("com.android.systemui")) {
                    //如果通知栏没有收起来，则延迟进行
                    ++retryTimes;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            UniversalCopy();
                        }
                    }, 100);
                    return;
                }

                //获取屏幕高宽，用于遍历数据时确定边界。
                WindowManager windowManager = (WindowManager)this.getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics displayMetrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                int heightPixels = displayMetrics.heightPixels;
                int widthPixels = displayMetrics.widthPixels;

                ArrayList nodeList = traverseNode(new AccessibilityNodeInfoCompat(rootInActiveWindow), widthPixels, heightPixels);
                if(nodeList.size() > 0) {
                    Intent intent = new Intent(this, CopyActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putParcelableArrayListExtra("copy_nodes", nodeList);
                    intent.putExtra("source_package", packageName);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        this.startActivity(intent, ActivityOptions.makeCustomAnimation(this.getBaseContext(), android.R.anim.fade_in, android.R.anim.fade_out).toBundle());
                    }else {
                        startActivity(intent);
                    }
                    isSuccess = true;
                    break labelOut;
                }
            }

            isSuccess = false;
        }

        if(!isSuccess) {
            if (!BigBangMonitorService.isAccessibilitySettingsOn(this)){
                ToastUtil.show(R.string.error_in_permission);
            }else {
                ToastUtil.show(R.string.error_in_copy);
            }

        }

        retryTimes = 0;
    }

    private ArrayList<CopyNode> traverseNode(AccessibilityNodeInfoCompat nodeInfo, int width, int height) {
        ArrayList<CopyNode> nodeList = new ArrayList();
        if(nodeInfo != null && nodeInfo.getInfo() != null) {
            nodeInfo.refresh();

            for(int i = 0; i < nodeInfo.getChildCount(); ++i) {
                //递归遍历nodeInfo
                nodeList.addAll(traverseNode(nodeInfo.getChild(i), width, height));
            }

            if(nodeInfo.getClassName() != null && nodeInfo.getClassName().equals("android.webkit.WebView")) {
                return nodeList;
            } else {
                String content = null;
                String description = content;
                if(nodeInfo.getContentDescription() != null) {
                    description = content;
                    if(!"".equals(nodeInfo.getContentDescription())) {
                        description = nodeInfo.getContentDescription().toString();
                    }
                }

                content = description;
                if(nodeInfo.getText() != null) {
                    content = description;
                    if(!"".equals(nodeInfo.getText())) {
                        content = nodeInfo.getText().toString();
                    }
                }

                if(content != null) {
                    Rect outBounds = new Rect();
                    nodeInfo.getBoundsInScreen(outBounds);
                    if(checkBound(outBounds, width, height)) {
                        nodeList.add(new CopyNode(outBounds, content));
                    }
                }

                return nodeList;
            }
        } else {
            return nodeList;
        }
    }


    private boolean checkBound(Rect var1, int var2, int var3) {
        //检测边界是否符合规范
        return var1.bottom >= 0 && var1.right >= 0 && var1.top <= var3 && var1.left <= var2;
    }

    // To check if service is enabled
    public static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = BigBangApp.getInstance().getPackageName() + "/" + BigBangMonitorService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            LogUtil.v(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            LogUtil.d(TAG, "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            LogUtil.v(TAG, "***ACCESSIBILITY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    LogUtil.v(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        LogUtil.v(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            LogUtil.v(TAG, "***ACCESSIBILITY IS DISABLED***");
        }

        return false;
    }


    public static final String GET_ENABLED_SERVICES = "settings get secure enabled_accessibility_services\n";
    public static final String PUT_ENABLED_SERVICES = "settings put secure enabled_accessibility_services";
    public static final String SU = "su";
    private static Thread keepOpenThread;

    public static void keepAccessibilityOpen() {
        boolean isopen=SPHelper.getBoolean(ConstantUtil.AUTO_OPEN_SETTING,false);
        if (!isopen){
            return;
        }
        if (keepOpenThread==null || !keepOpenThread.isAlive()) {
            keepOpenThread = new Thread(new Runnable() {
                int count=120;
                @Override
                public void run() {
                    boolean isopen=SPHelper.getBoolean(ConstantUtil.AUTO_OPEN_SETTING,false);
                    if (!isopen){
                        return;
                    }
                    BufferedWriter bufferedWriter = null;
                    BufferedReader bufferedReader = null;
                    java.lang.Process process=null;
                    try {
                        Runtime runtime = Runtime.getRuntime();
                        process = runtime.exec(SU);
                        InputStream inputStream = process.getInputStream();
                        OutputStream outputStream = process.getOutputStream();
                        bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        String service = BigBangApp.getInstance().getPackageName() + "/" + BigBangMonitorService.class.getCanonicalName();

                        do {
                            --count;
                            isopen=SPHelper.getBoolean(ConstantUtil.AUTO_OPEN_SETTING,false);
                            if (!isopen){
                                Thread.sleep(10000);
                                continue;
                            }
                            bufferedWriter.write(GET_ENABLED_SERVICES);
                            bufferedWriter.flush();

                            String current = bufferedReader.readLine();

                            if(current!=null) {
                                current=current.replaceAll(service, "");
                                current=current.replaceAll("::", ":");
                                current += ":" + service;
                            }else {
                                current = service;
                            }

                            bufferedWriter.write(PUT_ENABLED_SERVICES + " " + current + "\n");
                            bufferedWriter.flush();
                            bufferedWriter.write(ACCESSIBILITY_ENABLED + "\n");
                            bufferedWriter.flush();

                            Thread.sleep(10000);
                        }
                        while (count>0);

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    } finally {
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (bufferedWriter != null) {
                            try {
                                bufferedWriter.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (process!=null){
                            process.destroy();
                        }
                    }
                }
            });
            keepOpenThread.start();
        }

    }

    private synchronized void readSettingFromSp(){
        isRun=SPHelper.getBoolean(ConstantUtil.TOTAL_SWITCH,true);
        KeyPressedTipViewController.getInstance().updateTriggerType();
        if (!isRun){
            monitorClick=false;
            showFloatView=false;
            onlyText=true;
//            ArcTipViewController.getInstance().remove();
            return;
        }

        monitorClick = SPHelper.getBoolean(ConstantUtil.MONITOR_CLICK,true);
        showFloatView =SPHelper.getBoolean(ConstantUtil.SHOW_FLOAT_VIEW,false);
        onlyText = SPHelper.getBoolean(ConstantUtil.TEXT_ONLY,true) ;
        double_click_interval=SPHelper.getInt(ConstantUtil.DOUBLE_CLICK_INTERVAL,ConstantUtil.DEFAULT_DOUBLE_CLICK_INTERVAL);

        String[] spinnerArray= getResources().getStringArray(SPINNER_ARRAY);
        String qq = SPHelper.getString(ConstantUtil.QQ_SELECTION,spinnerArray[1]);
        String weixin = SPHelper.getString(ConstantUtil.WEIXIN_SELECTION,spinnerArray[1]);
        String other = SPHelper.getString(ConstantUtil.OTHER_SELECTION,spinnerArray[1]);
        if (showFloatView){
            ArcTipViewController.getInstance().show();
        }else {
            ArcTipViewController.getInstance().remove();
        }

        qqSelection=spinnerArrayIndex(spinnerArray, qq)+1;
        weixinSelection=spinnerArrayIndex(spinnerArray, weixin)+1;
        otherSelection=spinnerArrayIndex(spinnerArray, other)+1;

        keepAccessibilityOpen();
    }


    private int spinnerArrayIndex(String[] array,String txt){
        int length=array.length;
        for (int i=0;i<length;i++){
            if (array[i].equals(txt)){
                return i;
            }
        }
        return 3;
    }


    public synchronized void readWhiteList(){
        selections=new SelectionDbHelper(this).getSelections();
    }

    public synchronized void readFloatWhiteList(){
        int numbers = SPHelper.getInt(ConstantUtil.FLOAT_WHITE_LIST_COUNT,0);
        List<String> selectedPackageNames=new ArrayList<>();
        for (int i=0;i<numbers;i++){
            selectedPackageNames.add(SPHelper.getString(ConstantUtil.FLOAT_WHITE_LIST+i,""));
        }
        floatWhiteList=selectedPackageNames;
    }

    private BroadcastReceiver bigBangBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConstantUtil.REFRESH_WHITE_LIST_BROADCAST)){
                readWhiteList();
            }else if (intent.getAction().equals(ConstantUtil.FLOAT_REFRESH_WHITE_LIST_BROADCAST)){
                readFloatWhiteList();
            }else if (intent.getAction().equals(ConstantUtil.UNIVERSAL_COPY_BROADCAST)){
//                if (XposedEnableUtil.isEnable()){
//                    sendBroadcast(new Intent(ConstantUtil.UNIVERSAL_COPY_BROADCAST_XP));
//                }else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        UniversalCopy();
                    }
//                }
            }else if (intent.getAction().equals(ConstantUtil.UNIVERSAL_COPY_BROADCAST_DELAY)){
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                        if (XposedEnableUtil.isEnable()){
//                            sendBroadcast(new Intent(ConstantUtil.UNIVERSAL_COPY_BROADCAST_XP));
//                        }else {
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//                                UniversalCopy();
//                            }
//                        }
                    }
                },500);

            }else if (intent.getAction().equals(ConstantUtil.SCREEN_CAPTURE_OVER_BROADCAST)){

            }else if (intent.getAction().equals(ConstantUtil.EFFECT_AFTER_REBOOT_BROADCAST)){
                Process.killProcess(Process.myPid());
            } else if(intent.getAction().equals(ConstantUtil.MONITOR_CLICK_BROADCAST)){
                if (!isRun){
                    ToastUtil.show(R.string.open_total_switch_first);
                    return;
                }
                UrlCountUtil.onEvent(UrlCountUtil.STATUS_NOFITY_CLICK,!monitorClick);
                SPHelper.save(ConstantUtil.MONITOR_CLICK,!monitorClick);
                readSettingFromSp();
                if (monitorClick){
                    if (isAccessibilitySettingsOn(context)) {
                        ToastUtil.show(R.string.monitor_click_open);
                    }else {
                        ToastUtil.show(R.string.error_in_permission);
                    }
                }else {
                    ToastUtil.show(R.string.monitor_click_close);
                }
                sendBroadcast(new Intent(ConstantUtil.BROADCAST_CLIPBOARD_LISTEN_SERVICE_MODIFIED));
            } else {
                readSettingFromSp();
            }
        }
    };


    private  BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                // 开屏
//                isScreenOn=true;
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                // 锁屏
                Intent alive =  new Intent(BigBangMonitorService.this, KeepAliveActivity.class);
                alive.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(alive);
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
            }
        }
    };

}
