package com.forfan.bigbang.component.activity.setting;

import android.Manifest;
import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.ViewPager;

import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import com.alibaba.sdk.android.feedback.impl.FeedbackAPI;
import com.alibaba.sdk.android.feedback.util.IWxCallback;
import com.forfan.bigbang.BigBangApp;
import com.forfan.bigbang.R;
import com.forfan.bigbang.component.activity.FeedbackActivity;
import com.forfan.bigbang.component.base.BaseActivity;
import com.forfan.bigbang.util.ChanelUtil;
import com.forfan.bigbang.util.ConstantUtil;
import com.forfan.bigbang.util.UpdateUtil;
import com.forfan.bigbang.util.UrlCountUtil;
import com.shang.commonjar.contentProvider.SPHelper;
import com.shang.utils.StatusBarCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import static com.forfan.bigbang.util.ConstantUtil.BROADCAST_RELOAD_SETTING;


public class SettingActivity extends BaseActivity {

    private static final String TAG = "SettingActivity";



    private Toolbar toolbar;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private List<BaseFragment> fragmentList;
    private List<String> fragmentTitles;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_setting);
        StatusBarCompat.setupStatusBarView(this, (ViewGroup) getWindow().getDecorView(), true, R.color.colorPrimary);

        initToolBar();
        initFragments();
        initViewPager();
        initIndiator();

        Observable.timer(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .flatMap(new Func1<Long, Observable<String>>() {
                    @Override
                    public Observable<String> call(Long aLong) {
                        return Observable.just("");
                    }
                })
                .subscribe(s -> {
                    if (s.equals("")) {
                        boolean hadEnterIntro = SPHelper.getBoolean(ConstantUtil.HAD_ENTER_INTRO, false);
                        boolean hasShared = SPHelper.getBoolean(ConstantUtil.HAD_SHARED, false);
                        int openTimes = SPHelper.getInt(ConstantUtil.SETTING_OPEN_TIMES, 0);

                        if (!hadEnterIntro){
                            ViewStub viewStub = (ViewStub) findViewById(R.id.intro_card);
                            viewStub.inflate();
                            return;
                        }

                        //// TODO: 2016/11/1 第一期先不上分享功能了
                        // TODO: 2016/10/31 如果用户选择不分享，应该短期内不再显示
                        if (!hasShared && openTimes >= 3 && openTimes % 8 == 0) {
                            ViewStub viewStub = (ViewStub) findViewById(R.id.share_card);
                            viewStub.inflate();
                            ShareCard shareCard= (ShareCard) findViewById(R.id.share);
                            shareCard.setDisMissListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            });
                        }
                    }
                });


        Observable.timer(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .flatMap(new Func1<Long, Observable<String>>() {
                    @Override
                    public Observable<String> call(Long aLong) {
                        return Observable.just("");
                    }
                })
                .subscribe(s -> {
                    if (s.equals("")) {
                        try {
                            if (!ChanelUtil.isXposedApk(getApplicationContext())) {
                                UpdateUtil.autoCheckUpdate();
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });


        checkPermission();

        int openTimes = SPHelper.getInt(ConstantUtil.SETTING_OPEN_TIMES, 0);
        SPHelper.save(ConstantUtil.SETTING_OPEN_TIMES, openTimes + 1);
    }
    private void initViewPager(){
        viewPager= (ViewPager) findViewById(R.id.container);
//        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return fragmentList.size();
            }

            @Override
            public Fragment getItem(int position) {
                return fragmentList.get(position);
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return fragmentTitles.get(position);
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                if (position==3){
                    fragmentList.get(position).setUserVisibleHint(true);
                }
                UrlCountUtil.onEvent(UrlCountUtil.CLICK_FRAGMENT_SWITCHES);
                super.onPageSelected(position);
            }
        });

    }
    private void initFragments(){
        fragmentList=new ArrayList<>();
        fragmentTitles=new ArrayList<>();
        fragmentList.add(new PickWordFragment());
        fragmentList.add(new DisplayFragment());
        fragmentList.add(new OthersFragment());

        fragmentTitles.add(getString(R.string.fragment_segment));
        fragmentTitles.add(getString(R.string.fragment_display));
        fragmentTitles.add(getString(R.string.fragment_other));
    }

    private void initToolBar() {
        // TODO: 2016/1/21 加入评价
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initIndiator() {
//        indicator= (FragmentTabIndicator) findViewById(R.id.indicator);
//        indicator.setSelete(0);
//        indicator.setOnTabSelectedListener(new FragmentTabIndicator.OnTabSelectedListener() {
//            @Override
//            public void onTabSelectedListener(int position) {
//                viewPager.setCurrentItem(position);
//            }
//        });

        tabLayout= (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void checkPermission() {
        checkPermission(new CheckPermListener() {
                            @Override
                            public void grantPermission() {
                                checkFeedbackAnswer();
                            }

                            @Override
                            public void denyPermission() {
                                checkFeedbackAnswer();
                            }
                        }, R.string.ask_again,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void checkFeedbackAnswer() {
        FeedbackAPI.initAnnoy( BigBangApp.getInstance(), ConstantUtil.ALI_APP_KEY);
        FeedbackAPI.getFeedbackUnreadCount(getApplicationContext(), null, new IWxCallback() {
            @Override
            public void onSuccess(Object... objects) {
                int unread=((Integer) objects[0]);
                if (unread==0){
                    return;
                }
                String msg=getString(R.string.unread_feed_back,unread+"");
                NotificationCompat.Builder notification = new NotificationCompat.Builder(BigBangApp.getInstance());
                Bitmap notifyIcon;
                notifyIcon= BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher);
                notification.setLargeIcon(notifyIcon);
                // TODO: 2016/11/15
                notification.setSmallIcon(R.mipmap.ic_launcher);
                notification.setWhen(System.currentTimeMillis());
                notification.setAutoCancel(true);
                notification.setContentTitle(getString(R.string.app_name));
                notification.setContentText(msg);
                notification.setTicker(msg);
                Intent notificationIntent = new Intent(SettingActivity.this, FeedbackActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(SettingActivity.this, 1000, notificationIntent, 0);
                notification.setContentIntent(pendingIntent);
                int t=0;
                t|= Notification.DEFAULT_VIBRATE;
                t|=Notification.DEFAULT_SOUND;
                notification.setDefaults(t);
                NotificationManagerCompat nm= NotificationManagerCompat.from(SettingActivity.this);
                if (Build.VERSION.SDK_INT<16){
                    nm.notify(1008600, notification.getNotification());
                }else{
                    nm.notify(1008600, notification.build());
                }

            }

            @Override
            public void onError(int i, String s) {

            }

            @Override
            public void onProgress(int i) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        sendBroadcast(new Intent(BROADCAST_RELOAD_SETTING));
    }
}
