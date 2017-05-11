package com.fenqile.licai.base;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mobstat.StatService;
import com.fenqile.licai.R;
import com.fenqile.licai.broadcast.ScreenBroadcastReceiver;
import com.fenqile.licai.config.Constants;
import com.fenqile.licai.config.IntentKey;
import com.fenqile.licai.db.DBSQLiteOpenHelper;
import com.fenqile.licai.manager.AccountManager;
import com.fenqile.licai.manager.GlobalInfoManager;
import com.fenqile.licai.nativeh5.NativeH5Util;
import com.fenqile.licai.redpoint.client.GetUnReadMsgCallback;
import com.fenqile.licai.redpoint.client.GetUnReadMsgClient;
import com.fenqile.licai.redpoint.client.SetRedPointReadCallback;
import com.fenqile.licai.redpoint.client.SetRedPointReadClient;
import com.fenqile.licai.redpoint.model.RedPointConfig;
import com.fenqile.licai.ui.gesture.UnlockGestureActivity;
import com.fenqile.licai.ui.login.LoginActivity;
import com.fenqile.licai.ui.main.MainActivity;
import com.fenqile.licai.ui.main.PageLevel;
import com.fenqile.licai.ui.splash.SplashActivity;
import com.fenqile.licai.umeng.EventStatistics;
import com.fenqile.licai.util.HttpUtils;
import com.fenqile.licai.view.webview.WebViewActivity;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Activity基类
 * Created by roro on 2015/9/30.
 */
public class BaseActivity extends AppCompatActivity implements AccountManager.OnUserInfoChangedListener {
    public static LinkedList<BaseActivity> mStackList = new LinkedList<>();
    private ProgressDialog mProgressDialog;
    private TextView tvMsg;
    private View mProgressView;
    private ScreenBroadcastReceiver mScreenreceiver;
	
    private ProgressDialog mLoadingDialog;
	private ProgressDialog mLoadingDialogTwo;
    private View mLoadingView;
    //
    public static boolean IS_LOCK_ON = false;//true：手机锁屏了/需要打开解锁界面
    public static boolean OPEN_OR_COLSE = false;
    //应用是否在前台运行
    public static boolean isActive = true;
    public static int count = 0;//打开Activity的数量
    private PullToRefreshBase pulltoRefresh;

    private DBSQLiteOpenHelper db;
    private SQLiteDatabase sdb;
    //小红点配置文件
    private static List<RedPointConfig.ItemArr> mItemArrs = GlobalInfoManager.get().getRedPointConfig().getItemArr();
    //DotId的集合
    private List<RedPointConfig.ItemArr.DotModel> mDotModelArrs;


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    /**
     * 设置刷新的时候可以进行滚动
     */
    private void trySetupPullToRefresh() {
        View view = findViewById(R.id.pull_to_refresh);
        if (view instanceof PullToRefreshBase) {
            pulltoRefresh = (PullToRefreshBase) view;
            pulltoRefresh.setScrollingWhileRefreshingEnabled(true);
        }
    }

    /**
     * 刷新完成
     */
    public final void onRefreshComplete() {
        if (pulltoRefresh != null) {
            pulltoRefresh.onRefreshComplete();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStackList.add(this);
        //注册解锁界面是否启动的光比接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenreceiver = new ScreenBroadcastReceiver();
        registerReceiver(mScreenreceiver, filter);
        registerForUserInfo();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /**
     * 从栈中移除某个activity
     * @param mActivity
     */
    protected void RemoveActivity(Activity mActivity) {
        mStackList.remove(mActivity);
    }

    /**
     * 判断某个Activity是否在栈中
     *
     * @param activityName
     * @return
     */
    protected boolean hasActivity(String activityName) {
        for (Activity activity : mStackList) {
            if (activity.getClass().getSimpleName().equals(activityName)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 页面大于三级的时候在右上角添加关闭按钮
     */
    protected void checkPageDepth() {
        //有几个不同的WebView TODO // FIXME: 2016/2/2
        View tv = findViewById(R.id.mTvCommonHeaderRight);
        if (tv == null) tv = findViewById(R.id.tv_right);
        if (tv != null
                && tv instanceof TextView
                && TextUtils.isEmpty(((TextView) tv).getText())
                && PageLevel.needAddCloseButton
                && checkIfNeedClose()
                ) {
            tv.setVisibility(View.VISIBLE);
            ((TextView) tv).setText("关闭");
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    killUntilRoot();
                }
            });
        }
    }

    /**
     * 是否需要右上角的关闭按钮
     * @return
     */
    public boolean checkIfNeedClose() {
        int counter = 1;//MainActivity
        for (int i = mStackList.size() - 1; i >= 0; i--) {
            BaseActivity activity = mStackList.get(i);
            if (!(activity instanceof MainActivity)) {
                counter++;
            } else {
                break;
            }
        }
        return counter > 3;
    }

    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration config = new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config, res.getDisplayMetrics());
        return res;
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(JuziApplication.getInstance().getApplication().getBaseContext());
        StatService.onResume(this);
        updateIconFont(1800L);
        updateNativeH5Config(1800L);
        updateRedPointConfig(1800L);
        count++;
        if (isAppOnForeground()) {
            isActive = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(JuziApplication.getInstance().getApplication().getBaseContext());
        StatService.onPause(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        count--;
        saveSysTime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStackList.remove(this);
        //关闭广播接收器
        unregisterReceiver(mScreenreceiver);
        unregisterForUserInfo();
        if (isAppOnForeground()) {
            isActive = false;
        }
        JuziApplication.getInstance().ssoDialog = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (IS_LOCK_ON) {
            startLock();
        }
    }

    /***
     * app在后台的时间超过指定时间，更新 RedPointConfig配置文件
     * @param timeInterval
     */
    public void updateRedPointConfig(long timeInterval){
        if (count == 0 && !(this instanceof SplashActivity)) {//app唤醒
            SharedPreferences preferencesAppTime = getSharedPreferences(Constants.RED_POINT_SP, MODE_PRIVATE);
            long lastSysTime = preferencesAppTime.getLong(Constants.RED_POINT_KEY_TIME, 0L);
            long currentSysTime = System.currentTimeMillis();
            if (currentSysTime - lastSysTime > timeInterval * 1000) {//app在后台的时间超过指定时间，更新Typeface，从服务器获取ttf文件
                //RedPointUtil.updateRedPointConfig(this);
            }
        }

    }

    /**
     * app在后台运行时间超过指定时间，更新Typeface，从服务器获取ttf文件
     */
    private void updateIconFont(long timeInterval) {
        if (count == 0 && !(this instanceof SplashActivity)) {//app唤醒
            SharedPreferences preferencesAppTime = getSharedPreferences(Constants.ICON_FONT_SP, MODE_PRIVATE);
            long lastSysTime = preferencesAppTime.getLong(Constants.ICON_FONT_KEY_TIME, 0L);
            long currentSysTime = System.currentTimeMillis();
            if (currentSysTime - lastSysTime > timeInterval * 1000) {//app在后台的时间超过指定时间，更新Typeface，从服务器获取ttf文件
                GlobalInfoManager.get().updateIconFontFile(this);
            }
        }
    }

    /**
     * app在后台的时间超过指定时间，更新 OAConfig, ActionPairing 配置文件
     */
    private void updateNativeH5Config(long timeInterval) {
        if (count == 0 && !(this instanceof SplashActivity)) {//app唤醒
            SharedPreferences preferences = getSharedPreferences(Constants.NATIVE_H5_SP, MODE_PRIVATE);
            long lastSysTime = preferences.getLong(Constants.NATIVE_H5_SP_KEY_TIME, 0L);
            long currentSysTime = System.currentTimeMillis();
            if (currentSysTime - lastSysTime > timeInterval * 1000) {//app在后台的时间超过指定时间，更新配置文件
                NativeH5Util.updateNativeH5Config(this);
            }
        }
    }

    /**
     * app退出或者回到后台, 保存当前时间, 用于更新字体图片ttf文件
     */
    private void saveSysTime() {
        if (count == 0) {//app退出或者回到后台, 保存当前时间
            //ttf文件 SharedPreferences
            SharedPreferences preferencesAppTime = getSharedPreferences(Constants.ICON_FONT_SP, MODE_PRIVATE);
            long currentSysTime = System.currentTimeMillis();
            SharedPreferences.Editor editor = preferencesAppTime.edit();
            editor.putLong(Constants.ICON_FONT_KEY_TIME, currentSysTime);
            editor.apply();

            //NativeH5 SharedPreferences
            SharedPreferences preferencesNativeH5 = getSharedPreferences(Constants.NATIVE_H5_SP, MODE_PRIVATE);
            SharedPreferences.Editor editorNativeH5 = preferencesNativeH5.edit();
            editorNativeH5.putLong(Constants.NATIVE_H5_SP_KEY_TIME, currentSysTime);
            editorNativeH5.apply();
        }
    }

    // 得到顶部分的Activity
    public static BaseActivity GetTopActivity() {
        if (mStackList.size() <= 0)
            return null;
        return mStackList.get(mStackList.size() - 1);
    }

    /**
     * 检查是否具有网络连接
     * @return
     */
    public boolean isNetWorks() {
        if (HttpUtils.isConnected(this)) {
            return true;
        } else {
            toastShort("请检查您的网络连接");
            return false;
        }
    }


    /**
     * 查询数据库中的小红点
     *
     * @param strClickId
     * @return
     */
    public int checkDotIdNum(String strClickId) {
        //创建数据库
        db = new DBSQLiteOpenHelper(JuziApplication.getInstance().getApplication().getBaseContext());
        //调用数据库
        sdb = db.getReadableDatabase();
        StringBuffer strSql = new StringBuffer();
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < mItemArrs.size(); i++) {
            if (mItemArrs.get(i).getItemID().equals(strClickId)) {
                mDotModelArrs = mItemArrs.get(i).getDotModel();
            }
        }
        for (int k = 0; k < mDotModelArrs.size(); k++) {
            list.add(mDotModelArrs.get(k).getDotID());
        }
        for (int m = 0; m < list.size(); m++) {
            if (m == list.size() - 1) {
                strSql.append("'" + list.get(m) + "'");
            } else {
                strSql.append("'" + list.get(m) + "',");
            }
        }
        //查询表格
        Cursor cursor = sdb.rawQuery("select  count(0)  as num  from  un_red_point  where pushRedId in(" + strSql + ")", null);
        cursor.moveToFirst();
        int num = cursor.getInt(cursor.getColumnIndex("num"));
        return num;
    }

    /**
     * 查询数据库中的小红点
     *
     * @param strClickId
     * @return
     */
    public StringBuffer checkDotIdArr(String strClickId) {
        //创建数据库
        db = new DBSQLiteOpenHelper(JuziApplication.getInstance().getApplication().getBaseContext());
        //调用数据库
        sdb = db.getReadableDatabase();
        StringBuffer strSql = new StringBuffer();
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < mItemArrs.size(); i++) {
            if (mItemArrs.get(i).getItemID().equals(strClickId)) {
                mDotModelArrs = mItemArrs.get(i).getDotModel();
            }
        }

        for (int k = 0; k < mDotModelArrs.size(); k++) {
            list.add(mDotModelArrs.get(k).getDotID());
        }

        for (int m = 0; m < list.size(); m++) {
            if (m == list.size() - 1) {
                strSql.append("'" + list.get(m) + "'");
            } else {
                strSql.append("'" + list.get(m) + "',");
            }
        }
        return strSql;
    }

    /***
     * 设置小红点已读
     */
    public void setRedPointRead(TextView mTextView,String str) {
        //影藏小红点
        if(mTextView!=null){
            mTextView.setVisibility(View.GONE);
        }
        StringBuffer strSql=checkDotIdArr(str);
        //创建数据库
        db = new DBSQLiteOpenHelper(JuziApplication.getInstance().getApplication().getBaseContext());
        //调用数据库
        sdb = db.getReadableDatabase();

        List<String> mList = new ArrayList<>();
        /*Cursor cursor = sdb.rawQuery("select msgId from UnReadMsgEntity where pushRedNo = '1-1-7'", null);*/
        Cursor cursor = sdb.rawQuery("select msgId from un_red_point where pushRedId in(" + strSql + ")", null);
        while (cursor.moveToNext()) {
            cursor.getColumnIndex("msgId");
            int nameColumnIndex = cursor.getColumnIndex("msgId");
            String name = cursor.getString(nameColumnIndex);
            mList.add(name);
        }
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < mList.size(); i++) {
            jsonArray.put(mList.get(i));
        }
        //通知服务器消息已读
        new SetRedPointReadClient().execute(new SetRedPointReadCallback(), IntentKey.MSG_ID, jsonArray);

        //删除数据中pushClickId等于11205的数据
        sdb.execSQL("delete from  un_red_point  where  pushRedId in(" + strSql + ")");
    }

    /**
     * ------------------BaseAcitivity 方法 用于短Toast-----------------------</br>
     *
     * @param str 需要Toast的字符串
     */
    public void toastShort(String str) {
        if (!TextUtils.isEmpty(str)) {
            Toast toast = Toast.makeText(JuziApplication.getInstance().getApplication(), str, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    /**
     * ------------------BaseAcitivity 方法 用于长Toast-----------------------</br>
     *
     * @param str 需要Toast的字符串
     */
    public void toastLong(String str) {
        if (!TextUtils.isEmpty(str)) {
            Toast toast = Toast.makeText(JuziApplication.getInstance().getApplication(), str, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    /**
     * 展示进度条
     * @param msg
     * @param cancelable
     */
    public void showProgress(String msg, boolean cancelable) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this, R.style.ProgressDialog);
            mProgressView = getLayoutInflater().inflate(R.layout.progress_layout, null);
            tvMsg = (TextView) mProgressView.findViewById(R.id.tv_progress_message);
        }
        mProgressDialog.setCancelable(cancelable);
        if (tvMsg != null && !TextUtils.isEmpty(msg)) {
            tvMsg.setText(msg);
        }
        mProgressDialog.show();
        if (mProgressView != null) {
            mProgressDialog.setContentView(mProgressView);
        }
    }

    public void showProgress(String msg) {
        showProgress(msg, true);
    }

    //Progress
    public void showLoadingDialog(boolean cancelable) {
        if (mLoadingDialog == null) {
            mLoadingDialog = new ProgressDialog(this, R.style.LoadingDialog);
            mLoadingView = LayoutInflater.from(this).inflate(R.layout.layout_loading_view, null);
        }
        mLoadingDialog.setCancelable(cancelable);
        mLoadingDialog.setCanceledOnTouchOutside(false);

        mLoadingDialog.show();
        if (mLoadingView != null) {
            mLoadingDialog.setContentView(mLoadingView);
        }
    }


    public void hideProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public void hideLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }
    /**
     * @param key
     * @return 判断上一页面是否有String传值
     */
    protected String getStringByKey(String key) {
        if (getIntent().getExtras() != null
                && getIntent().getExtras().getString(key) != null)
            return getIntent().getExtras().getString(key);
        return "";
    }

    /**
     * @param key
     * @return 判断上一页面是否有传Boolean值
     */
    protected boolean getBooleanByKey(String key) {
        if (getIntent().getExtras() != null) {
            return getIntent().getExtras().getBoolean(key);
        }
        return false;
    }

    /**
     * when back button double clicked ,exit!
     */
    private boolean isExit = false;

    protected void exit() {
        if (!isExit) {
            isExit = true;
            toastShort(getString(R.string.press_again_to_exit));
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                }
            };
            timer.schedule(timerTask, 2000);
        } else {
            this.finish();
            overridePendingTransition(R.anim.fade_in_center,
                    R.anim.fade_out_center);
            EXIT();
        }
    }

    /***
     * 拉取未读红点消息
     */
    public void GetUnReadRedPoint(){
        if (AccountManager.getInstance().isLogin()) {
            //拉取未读的消息数据
            new GetUnReadMsgClient().execute(new GetUnReadMsgCallback());
        }
    }

    /**
     * 启动解锁界面
     */
    public void startLock() {
        SharedPreferences sharedPreferences = JuziApplication.getInstance().getApplication().getSharedPreferences(
                "LockGesture", MODE_PRIVATE);
        String strLock = sharedPreferences.getString(AccountManager.getInstance().getUserInfo().getUid(), "");
      /*  if (!AccountManager.getInstance().isLogin() || this instanceof SetLockGestureActivity || hasActivity(UnlockGestureActivity.class.getSimpleName())) {
            return;
        }*/
        if (AccountManager.getInstance().isLogin() && (!TextUtils.isEmpty(strLock)) && !(this instanceof UnlockGestureActivity)) {
            startActivityForResult(new Intent(this, UnlockGestureActivity.class), IntentKey.REQUEST_LOCK);
        }
    }

    /**
     * 启动登陆界面
     **/
    public void doNotLoginEvent() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, IntentKey.SKIPLOGIN);
    }

    /**
     * 启动登陆界面,登录完成之后跳转首页
     **/
    public void doSSOLoginEvent() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, IntentKey.SSOLOGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IntentKey.SSOLOGIN) {
//            setResult(RESULT_OK);
            killUntilRoot();
            finish();
            //
            JuziApplication.getInstance().goMainFirst(this);
        }
    }

    /**
     * 退出程序
     */
    public static void EXIT() {
        BaseActivity.IS_LOCK_ON = true;
       /* DownloadUtils.stop();*/
        ArrayList<Activity> tempList = new ArrayList<Activity>();
        for (final Activity activity : mStackList) {
            tempList.add(activity);
        }
        for (final Activity activity : tempList) {
            if (activity != null) {
                activity.finish();
            }
        }
        System.exit(0);
    }

    /**
     * @param target_url WebView载入的页面地址
     * @param isRedirect 是否重定跟用户相关，需要重定向；例如我的账单等.</br>注册协议等与用户session无关 不需重定
     */

    public void startWebView(String target_url, boolean isRedirect, String title) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(IntentKey.URL_LINK, target_url);
        intent.putExtra(IntentKey.IS_REDIRECT, isRedirect);
        intent.putExtra(IntentKey.TITLE, title);
        startActivity(intent);
    }

    public void startWebView(String target_url, boolean isRedirect) {
        startWebView(target_url, isRedirect, "");
    }

    public void startWebView(String target_url, boolean isRedirect, int requestCode) {
        startWebViewForResult(target_url, isRedirect, "", requestCode);
    }

    protected void startWebViewForResult(String target_url, boolean isRedirect, String title, int requestCode) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(IntentKey.URL_LINK, target_url);
        intent.putExtra(IntentKey.IS_REDIRECT, isRedirect);
        intent.putExtra(IntentKey.TITLE, title);
        startActivityForResult(intent, requestCode);
    }

    /**
     * 新手信息，可买天数需要截取数字变颜色
     * <p/>
     * 截取数字  可买天数30天，剩余<span>30</span>天
     *
     * @param textView
     * @param text
     */
    public void setLimitText(TextView textView, String text) {
        if (textView == null || text == null) return;
        try {
            Pattern pattern = Pattern.compile("<span>(.*)</span>");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String spanDays = String.format("<font color = '#999999'>%s</font>", matcher.group());
                String[] split = pattern.split(text);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < split.length; i++) {
                    if (i == 0) {
                        sb.append(split[i]).append(spanDays);
                    } else {
                        sb.append(split[i]);
                    }
                }
                textView.setText(Html.fromHtml(sb.toString()));
                return;
            }
        } catch (Exception e) {
            textView.setText(text);
            return;
        }
        textView.setText(text);
    }


    /**
     * 友盟事件统计
     *
     * @param eventId
     */
    public void statistics(String eventId) {
        EventStatistics.Statistics(this, eventId);
    }

    /**
     * 注册用户信息改变监听
     */
    private void registerForUserInfo() {
        boolean needRegister = this instanceof MainActivity;
        if (needRegister) {
            AccountManager.getInstance().registerOnUserInfoChangedListener(this);
        }
    }

    /**
     * 注销用户信息改变监听
     */
    private void unregisterForUserInfo() {
        boolean registered = this instanceof MainActivity;
        if (registered) {
            AccountManager.getInstance().unregisterOnUserInfoChangedListener(this);
        }
    }

    @Override
    public void onLogin() {

    }

    @Override
    public void onLogout(boolean normal) {

    }

    /**
     * 调用android自带的照相机
     * @param file
     */
    public void startCameraAty(File file) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        intent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
        startActivityForResult(intent, IntentKey.REQUEST_CAMERA);
    }

    /**
     * 调用android相册,选择照片
     */
    public void startAlbumAty() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(Intent.createChooser(intent, "选择图片"),
                IntentKey.REQUEST_ALBUM);
    }

    public void initPhoto(File tempFile) {
        if (!tempFile.getParentFile().exists()) {
            tempFile.getParentFile().mkdirs();
        }
        if (!tempFile.exists()) {
            try {
                tempFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 程序是否在前台运行
     *
     * @return
     */
    public boolean isAppOnForeground() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = getApplicationContext().getPackageName();

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        if (appProcesses == null)
            return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName.equals(packageName)
                    && appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    /**
     * finish所有栈中的activity
     */
    public void killUntilRoot() {
        for (BaseActivity activity : mStackList) {
            if (!(activity == null
                    || activity instanceof MainActivity)) {
                activity.finish();
                overridePendingTransition(0, 0);
            }
        }
    }

    /**
     * finish所有栈中的activity,排除一些特定的activity
     */
    public void killUntilRoot(BaseActivity skip) {
        for (BaseActivity activity : mStackList) {
            if (!(activity == null
                    || activity instanceof MainActivity
                    || skip == activity)) {
                activity.finish();
                overridePendingTransition(0, 0);
            }
        }
    }
}
