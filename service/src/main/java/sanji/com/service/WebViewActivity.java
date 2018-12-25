package sanji.com.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import sanji.com.service.util.DeviceUtil;

public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "WebViewActivity";

    private WebView webView;
    public static final String EXTRA_URL = "EXTRA_URL";

    private String extraUrl;

    private boolean mIsBound;//是否绑定了service

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideActivity();
        setContentView(R.layout.activity_web_view);
        initExtra();
        initWebView();
    }


    private void hideActivity() {
        moveTaskToBack(true);
        //* 执行此语句后在后台运行
        //* 生命周期：onResume-- > onPause-- > onStop
    }

    @Override
    protected void onStart() {
        super.onStart();
        doBindService();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        hideActivity();
    }

    /**
     * 只是作为功能演示，此WebView未做任何防泄漏处理
     */
    private void initWebView() {
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);//允许使用js
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (mService != null) {
                    try {
                        mService.webViewPageFinished_Service();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if (mService != null) {
                    try {
                        mService.findUrl_Service(url);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        webView.loadUrl(extraUrl);
    }

    private void initExtra() {
        Intent intent = getIntent();
        if (intent != null) {
            //先通过参数获取url  客户端可以通过以下方式直接启动并传值~~我是从RemoteService里传入的
//            Intent intent = new Intent();
//            //
//            intent.setData(Uri.parse("demo://sanji.com.service/MainActivity?url=http://m.baidu.com"));
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
            Uri uri = intent.getData();
            if (uri != null) {
                extraUrl = uri.getQueryParameter("url");
            }

            //如果参数获取url还是空 则通过extra获取参数
            if (TextUtils.isEmpty(extraUrl)) {
                extraUrl = intent.getStringExtra(EXTRA_URL);
            }
        }

    }


    private MyServiceConnection mConnection;
    private IService mService = null;

    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.e(TAG, "onServiceConnected: 绑定");
            mService = IService.Stub.asInterface(service);
            mIsBound = true;
            try {
                //设置死亡代理 意外死亡自动绑定
                service.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //主线程 可以访问UI（也可以在这里进行绑定）
            mService = null;
            mIsBound = false;
        }
    }

    /**
     * 监听Binder是否死亡
     */
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            //子线程
            if (mService == null) {
                return;
            }
            Log.e(TAG, "binderDied: 死亡监听 重新复活绑定service");
            mService.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mService = null;
            mIsBound = false;
            //重新绑定
            doBindService();
        }
    };

    /**
     * bind service
     */
    private void doBindService() {
        Log.e(TAG, "doBindService: 绑定服务");
        //为什么不直接new ServiceConnection mConnection = new ServiceConnection(
        //因为unbind service的时候mSnifferConnection null了 如果重新绑定需要new
        mConnection = new MyServiceConnection();
        Intent intent = new Intent(IService.class.getName());
        intent.setPackage(DeviceUtil.getPackageName(this));//包名
        this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    /**
     * unbind service
     */
    private void doUnbindService() {
        if (mIsBound) {
            Log.e(TAG, "doUnbindService: 解绑服务");
            unbindService(mConnection);
            mConnection = null;
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }
}
