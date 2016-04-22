
package com.jacky.launcher.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RadioButton;

import com.jacky.launcher.LauncherApp;
import com.jacky.launcher.R;
import com.jacky.launcher.adapter.MainActivityAdapter;
import com.jacky.launcher.features.LocalServiceFragment;
import com.jacky.launcher.features.app.AppFragment;
import com.jacky.launcher.features.setting.SettingFragment;
import com.jacky.launcher.utils.FileCache;
import com.jacky.launcher.utils.NetWorkUtil;
import com.jacky.launcher.utils.SharedPreferencesUtil;
import com.jacky.uikit.activity.BaseTitleActivity;
import com.jacky.uikit.alarm.ToastAlarm;

import java.util.ArrayList;

public class MainActivity extends BaseTitleActivity implements View.OnClickListener {

    private ViewPager mViewPager;
    private RadioButton localService;
    private RadioButton setting;
    private RadioButton app;
    private SQLiteDatabase mSQLiteDataBase;
    private LauncherApp mClientApp;
    private int currentIndex;
    private static final int PAGE_NUMBER = 3;
    private ArrayList<Fragment> fragments = new ArrayList<>();
    private boolean d = true;// debug
    private SharedPreferencesUtil sp;
    private Context context;
    private FileCache fileCache;
    private String cacheDir;
    private View mViews[];
    private int mCurrentIndex = 0;

    public ViewPager.OnPageChangeListener pageListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageSelected(int position) {
            mViewPager.setCurrentItem(position);
            switch (position) {
                case 0:
                    currentIndex = 0;
                    localService.setSelected(true);
                    setting.setSelected(false);
                    app.setSelected(false);
                    break;
                case 1:
                    currentIndex = 1;
                    localService.setSelected(false);
                    setting.setSelected(true);
                    app.setSelected(false);
                    break;
                case 2:
                    currentIndex = 2;
                    localService.setSelected(false);
                    setting.setSelected(false);
                    app.setSelected(true);
                    break;
            }
        }
    };

    private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            NetworkInfo currentNetworkInfo = intent
                    .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            if (currentNetworkInfo.isConnected()) {

            } else {
                ToastAlarm.show("Network is not connected");
                LauncherApp.netFlag = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        mClientApp = (LauncherApp) this.getApplication();
        context = this;
        fileCache = new FileCache(context);
        cacheDir = fileCache.getCacheDir();
        sp = SharedPreferencesUtil.getInstance(context);
        initView();
        initData();
        context.registerReceiver(this.mConnReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void initData() {
        openDataBase();
        if (isThereHaveUrlDataInDB()) {
            String data = getUrlDataFromDB();
            initFragment(data);
            getUrlDataFromNetFlow();
        } else {
            getUrlDataFromNetFlow();
        }
    }

    private boolean isThereHaveUrlDataInDB() {
        boolean b = false;
        try {
            String s = getUrlDataFromDB();
            if (s.length() > 0)
                b = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    private void initView() {
        mViewPager = (ViewPager) this.findViewById(R.id.main_viewpager);
        localService = (RadioButton) findViewById(R.id.main_title_local);
        setting = (RadioButton) findViewById(R.id.main_title_setting);
        app = (RadioButton) findViewById(R.id.main_title_app);
        localService.setSelected(true);
        mViews = new View[]{
                localService, setting, app
        };
        setListener();
    }

    private void setListener() {
        localService.setOnClickListener(this);
        setting.setOnClickListener(this);
        app.setOnClickListener(this);

        localService.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mViewPager.setCurrentItem(0);
                }
            }
        });
        setting.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mViewPager.setCurrentItem(1);
                }
            }
        });
        app.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mViewPager.setCurrentItem(2);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_title_local:
                currentIndex = 0;
                mViewPager.setCurrentItem(0);
                break;
            case R.id.main_title_setting:
                currentIndex = 3;
                mViewPager.setCurrentItem(1);
                break;
            case R.id.main_title_app:
                currentIndex = 4;
                mViewPager.setCurrentItem(2);
                break;
        }
    }

    private void initFragment(String urlData) {
        fragments.clear();
        int count = PAGE_NUMBER;

        FragmentManager manager;
        FragmentTransaction transaction;

        manager = this.getSupportFragmentManager();
        transaction = manager.beginTransaction();

        LocalServiceFragment interactTV = new LocalServiceFragment();
        SettingFragment setting = new SettingFragment();
        AppFragment app = new AppFragment();

        Bundle bundle = new Bundle();
        bundle.putString("url_data", urlData);

        interactTV.setArguments(bundle);

        fragments.add(interactTV);
        fragments.add(setting);
        fragments.add(app);

        transaction.commitAllowingStateLoss();

        MainActivityAdapter mAdapter = new MainActivityAdapter(getSupportFragmentManager(), fragments);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(pageListener);
        mViewPager.setCurrentItem(0);
    }

    private void getUrlDataFromNetFlow() {
        if (NetWorkUtil.isNetWorkConnected(context)) {
            initFragment("");
        } else {
            initFragment("");
        }
    }

    private String getUrlDataFromDB() {
        Cursor cursor = mSQLiteDataBase.rawQuery("SELECT url_data FROM my_url_data", null);
        cursor.moveToLast();
        // String s = cursor.getString(2);
        return cursor.getString(cursor.getColumnIndex("url_data"));
    }

    private void openDataBase() {
        mSQLiteDataBase = this.openOrCreateDatabase("myapp.db",
                MODE_PRIVATE, null);
        String createTable = "create table if not exists my_url_data (_id INTEGER PRIMARY KEY,url_data TEXT);";
        mSQLiteDataBase.execSQL(createTable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSQLiteDataBase.close();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean focusFlag = false;
        for (View v : mViews) {
            if (v.isFocused()) {
                focusFlag = true;
            }
        }
        if (focusFlag) {
            if (KeyEvent.KEYCODE_DPAD_LEFT == keyCode) {
                if (mCurrentIndex > 0) {
                    mViews[--mCurrentIndex].requestFocus();
                }
                return true;
            } else if (KeyEvent.KEYCODE_DPAD_RIGHT == keyCode) {
                if (mCurrentIndex < 2) {
                    mViews[++mCurrentIndex].requestFocus();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mConnReceiver);
    }
}