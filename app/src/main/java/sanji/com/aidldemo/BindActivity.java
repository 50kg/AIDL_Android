package sanji.com.aidldemo;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import sanji.com.service.IRemoteService;
import sanji.com.service.IRemoteServiceCallback;
import sanji.com.service.IService;

public class BindActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "BindActivity";

    IRemoteService mRemoteService = null;
    IService mSnfferService = null;
    private boolean mIsBound = false; //是否已绑定
    private boolean mIsWhileBind = true;//循环绑定(因为启动Service APP有一定时间，所以每隔一秒循环绑定一次)

    private Button bind;
    private Button diaoyong;
    private Button unbind;
    private EditText etUrl;
    private Button mKillButton;
    private TextView mCallbackText;

    private static final int HANDLER_MSG_SNIFF_SUCCESS = 1; //回调的url
    private static final int HANDLER_MSG_SNIFF_FINISH = 2; //webview加载完成
    public static final String AIDL_PACKAGE_NAME = "sanji.com.service";//AIDL的包名

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bind_activity);
        bind = findViewById(R.id.bind);
        diaoyong = findViewById(R.id.diaoyong);
        unbind = findViewById(R.id.unbind);
        etUrl = findViewById(R.id.etUrl);
        mKillButton = findViewById(R.id.kill);
        mCallbackText = findViewById(R.id.callback);
        bind.setOnClickListener(this);
        diaoyong.setOnClickListener(this);
        unbind.setOnClickListener(this);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_MSG_SNIFF_SUCCESS:
                    String url = (String) msg.obj;
                    Log.e(TAG, url);
                    mCallbackText.setText(mCallbackText.getText().toString() + url+"\n");
                    break;
                case HANDLER_MSG_SNIFF_FINISH:
                    Log.e(TAG, "加载完成");
                    mCallbackText.setText(mCallbackText.getText().toString() + "加载完成");
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    /**
     * 远程回调接口实现
     */
    private IRemoteServiceCallback mCallback = new IRemoteServiceCallback.Stub() {

        @Override
        public void findUrl(String videoUrl) throws RemoteException {
            Message msg = mHandler.obtainMessage(HANDLER_MSG_SNIFF_SUCCESS);
            msg.obj = videoUrl;
            mHandler.sendMessage(msg);
        }

        @Override
        public void webViewPageFinished() throws RemoteException {
            mHandler.sendEmptyMessage(HANDLER_MSG_SNIFF_FINISH);
        }
    };

    /**
     * 远程服务的connecition用于注册回调和解除注册回调
     */
    private ServiceConnection mRemoteConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mRemoteService = IRemoteService.Stub.asInterface(service);
            //一般来说bind不会失败，但是这里要判断是因为可能app还未启动
            if (mRemoteService != null) {
                mKillButton.setEnabled(true);
                mCallbackText.setText("远程服务已连接(绑定)");
                mIsBound = true;

                try {
                    mRemoteService.registerCallback(mCallback);
                } catch (RemoteException e) {
                }
            } else {
                mCallbackText.setText("远程服务连接失败(绑定) null");
                mIsBound = false;
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mRemoteService = null;
            mKillButton.setEnabled(false);
            mCallbackText.setText("远程服务断开连接 onServiceDisconnected");
            mIsBound = false;
            //异常断开的原因可能被杀死了
            startPluginApp();
            //重新绑定
            doBindService();

        }
    };

    private ServiceConnection mSnifferConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mSnfferService = IService.Stub.asInterface(service);
            mKillButton.setEnabled(true);
        }

        public void onServiceDisconnected(ComponentName className) {
            mSnfferService = null;
            mKillButton.setEnabled(false);
        }
    };

    /**
     * 启动插件APP
     */
    private void startPluginApp() {
        try {
            Intent intent = new Intent();
            //?url=http://m.baidu.com
            intent.setData(Uri.parse("demo://sanji.com.service/MainActivity"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            mCallbackText.setText("未安装Service");
        }
    }


    /**
     * bind service
     */
    private void doBindService() {
        mIsWhileBind = true;
        //延时1秒去绑定远程服务，因为某事可能APP还未启动 去绑定则失效
        mHandler.postDelayed(r, 1 * 1000);
    }

    Handler mBindSetviceHandler = new Handler();
    Runnable r = new Runnable() {

        @Override
        public void run() {
            if (!mIsBound) {
                Log.e(TAG, "null 去绑定");
                //启动了 绑定

                //注意这里的Context.BIND_AUTO_CREATE，这意味这如果在绑定的过程中，
                //如果Service由于某种原因被Destroy了，Android还会自动重新启动被绑定的Service。
                // 你可以点击Kill Process 杀死Service看看结果

                Intent intent = new Intent(IRemoteService.class.getName());
                intent.setPackage(AIDL_PACKAGE_NAME);
                BindActivity.this.bindService(intent, mRemoteConnection, Context.BIND_AUTO_CREATE);


                intent = new Intent(IService.class.getName());
                intent.setPackage(AIDL_PACKAGE_NAME);
                BindActivity.this.bindService(intent, mSnifferConnection, Context.BIND_AUTO_CREATE);

                if (mIsWhileBind) {
                    //每隔1s循环执行run方法
                    mBindSetviceHandler.postDelayed(this, 1 * 1000);
                }

            } else {
                Log.e(TAG, "已绑定");
            }
        }
    };

    /**
     * unbind service
     */
    private void doUnbindService() {
        if (mIsBound) {
            if (mRemoteService != null) {
                try {
                    mRemoteService.unregisterCallback(mCallback);
                } catch (RemoteException e) {

                }
            }

            unbindService(mRemoteConnection);
            unbindService(mSnifferConnection);
            mKillButton.setEnabled(false);
            mCallbackText.setText("已解除绑定");
            mIsBound = false;
            mIsWhileBind = false;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bind:
                startPluginApp();
                doBindService();
                break;
            case R.id.diaoyong:
                if (mSnfferService != null) {
                    try {
                        mSnfferService.setUrl_Client(etUrl.getText().toString());
                    } catch (RemoteException e) {

                    }
                }
                break;
            case R.id.unbind:
                //解除绑定
                doUnbindService();
                break;
            case R.id.kill:
                //杀死
                if (mSnfferService != null) {
                    try {
                        //杀死服务后还是可以继续收到消息的。因为绑定的时候用了AUTO 会自动绑定
                        int pid = mSnfferService.getPid_Client();
                        Process.killProcess(pid);
                        mCallbackText.setText("已杀死服务");
                    } catch (RemoteException ex) {
                    }
                }
                break;
        }
    }
}
