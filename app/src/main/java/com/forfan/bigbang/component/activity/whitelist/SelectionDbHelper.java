package com.forfan.bigbang.component.activity.whitelist;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.forfan.bigbang.util.ConstantUtil;
import com.shang.commonjar.contentProvider.SPHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.forfan.bigbang.component.activity.setting.MonitorSettingCard.SPINNER_ARRAY;
import static com.forfan.bigbang.component.activity.whitelist.AppListAdapter.ApplicationInfoWrap.NON_SELECTION;

/**
 * Created by penglu on 2016/12/7.
 */

public class SelectionDbHelper extends SQLiteOpenHelper {

    public static final int VERSION=1;
    public static final String TABLE_NAME ="click_monitor";
    public static final String COLUMN_ID="_id";
    public static final String COLUMN_PACKAGE="package";
    public static final String COLUMN_TYPE="type";
    public static final String CREATE_EXEC=
            "create table if not exists "+TABLE_NAME+" ( "+
                    COLUMN_ID+" integer primary key, "+
                    COLUMN_PACKAGE+" varchar, "+
                    COLUMN_TYPE+" int "+
                    ")";

    private Context mContext;
    String[] spinnerArray;

    public SelectionDbHelper(Context context){
        super(context, TABLE_NAME,null,VERSION);
        mContext=context;
    }

    public SelectionDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public SelectionDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //????????????????????????????????????
        db.execSQL(CREATE_EXEC);

        spinnerArray = mContext.getResources().getStringArray(SPINNER_ARRAY);
        String qqSelection = SPHelper.getString(ConstantUtil.QQ_SELECTION, "");
        String weixinSelection = SPHelper.getString(ConstantUtil.WEIXIN_SELECTION, "");
        String otherSelection = SPHelper.getString(ConstantUtil.OTHER_SELECTION, "");
        int size = SPHelper.getInt(ConstantUtil.WHITE_LIST_COUNT, 0);
        if (!qqSelection.equals("")){
            insert(db,"com.tencent.mobileqq",spinnerArrayIndex(qqSelection));
        }
        if (!weixinSelection.equals("")){
            insert(db,"com.tencent.mm",spinnerArrayIndex(weixinSelection));
        }
        if (!otherSelection.equals("")){
            queryFilterAppInfo();
            querySelectedApp();
            int other=spinnerArrayIndex(otherSelection);
            for (AppListAdapter.ApplicationInfoWrap wrap:mCanOpenApplicationInfos){
                if (!wrap.isSelected){
                    insert(db,wrap.applicationInfo.packageName, other);
                }
            }
        }
//        db.close();
    }

    private int spinnerArrayIndex(String txt) {
        int length = spinnerArray.length;
        for (int i = 0; i < length; i++) {
            if (spinnerArray[i].equals(txt)) {
                return i;
            }
        }
        return NON_SELECTION;
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insert(SQLiteDatabase database,String packageName,int type){
        Cursor cursor=database.query(TABLE_NAME,null,COLUMN_PACKAGE+" = ?",new String[]{packageName},null,null,null);
        int rowId=-1;
        if (cursor!=null && cursor.moveToFirst()){
            int rowIndex = cursor.getColumnIndex(COLUMN_ID);
            rowId=cursor.getInt(rowIndex);
        }
        ContentValues values=new ContentValues();
        values.put(COLUMN_PACKAGE,packageName);
        values.put(COLUMN_TYPE,type);
        if (rowId==-1){
            database.insert(TABLE_NAME,null,values);
        }else {
            values.put(COLUMN_ID,rowId);
            database.update(TABLE_NAME,values,COLUMN_ID+" = ?",new String[]{""+rowId});
        }
    }

    public void insertAll(List<AppListAdapter.ApplicationInfoWrap> apps){
        SQLiteDatabase dataBase=getWritableDatabase();
        dataBase.delete(TABLE_NAME,null,null);
        dataBase.beginTransaction();       //????????????????????????
        //????????????????????????
        for (AppListAdapter.ApplicationInfoWrap wrap:apps){
            ContentValues values=new ContentValues();
            values.put(COLUMN_PACKAGE,wrap.applicationInfo.packageName);
            values.put(COLUMN_TYPE,wrap.selection);
            dataBase.insert(TABLE_NAME,null,values);
        }
        dataBase.setTransactionSuccessful();       //????????????????????????????????????????????????????????????
        dataBase.endTransaction();       //????????????
        dataBase.close();
    }

    public Map<String ,Integer> getSelections(){
        Map<String,Integer> selections=new HashMap<>();
        SQLiteDatabase database=getReadableDatabase();
        Cursor cursor=database.query(TABLE_NAME,null,null,null,null,null,null);
        if (cursor!=null && cursor.moveToFirst()){
            int packageIndex = cursor.getColumnIndex(COLUMN_PACKAGE);
            int typeIndex = cursor.getColumnIndex(COLUMN_TYPE);
            do {
                selections.put(cursor.getString(packageIndex),cursor.getInt(typeIndex));
            }
            while (cursor.moveToNext());
        }
        database.close();
        return selections;
    }


    public void deleteAll(){
        SQLiteDatabase database=getWritableDatabase();
        database.delete(TABLE_NAME,null,null);
        database.close();
    }

    private List<AppListAdapter.ApplicationInfoWrap> mCanOpenApplicationInfos;
    private List<AppListAdapter.ApplicationInfoWrap> mSelectedApplicationInfos;

    //???????????????
    private void queryFilterAppInfo() {

        final PackageManager pm = mContext.getPackageManager();
        // ???????????????????????????????????????
        List<ApplicationInfo> appInfos = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);// GET_UNINSTALLED_PACKAGES??????????????????????????????????????????


        List<AppListAdapter.ApplicationInfoWrap> applicationInfos = new ArrayList<>();
        List<AppListAdapter.ApplicationInfoWrap> allApp = new ArrayList<>();

        // ?????????????????????CATEGORY_LAUNCHER???????????????Intent
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // ??????getPackageManager()???queryIntentActivities????????????,????????????????????????app???packageName
        List<ResolveInfo> resolveinfoList = pm.queryIntentActivities(resolveIntent, 0);

        Set<String> allowPackages = new HashSet();
        for (ResolveInfo resolveInfo : resolveinfoList) {
            allowPackages.add(resolveInfo.activityInfo.packageName);
        }
        for (ApplicationInfo app : appInfos) {
//            if((app.flags & ApplicationInfo.FLAG_SYSTEM) <= 0)//??????????????????
//            {
//                applicationInfos.add(app);
//            }
//            if(app.uid > 10000){
//                applicationInfos.add(app);
//            }


            AppListAdapter.ApplicationInfoWrap wrap = new AppListAdapter.ApplicationInfoWrap();
            wrap.applicationInfo = app;
            if (allowPackages.contains(app.packageName)) {
                applicationInfos.add(wrap);
            }
            allApp.add(wrap);
        }
        Collections.sort(applicationInfos, new Comparator<AppListAdapter.ApplicationInfoWrap>() {//????????????????????????????????????

            @Override
            public int compare(AppListAdapter.ApplicationInfoWrap lhs, AppListAdapter.ApplicationInfoWrap rhs) {
                // TODO ???????????????????????????
                return lhs.applicationInfo.loadLabel(pm).toString().compareToIgnoreCase(rhs.applicationInfo.loadLabel(pm).toString());
            }
        });
        mCanOpenApplicationInfos = applicationInfos;

    }

    private void querySelectedApp() {
        mSelectedApplicationInfos = new ArrayList<>();
        Set<String> selectedPackageNames = new HashSet<>();
        int size = SPHelper.getInt(ConstantUtil.WHITE_LIST_COUNT, 0);
        for (int i = 0; i < size; i++) {
            String packageName = SPHelper.getString(ConstantUtil.WHITE_LIST + "_" + i, "");
            selectedPackageNames.add(packageName);
        }
        for (AppListAdapter.ApplicationInfoWrap app : mCanOpenApplicationInfos) {
            String packageName = app.applicationInfo.packageName;
            if (selectedPackageNames.contains(packageName)) {
                app.isSelected = true;
                mSelectedApplicationInfos.add(app);
            }
        }
    }



}
