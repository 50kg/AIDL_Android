package sanji.com.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

public class RemoteService extends Service {

    final RemoteCallbackList<IRemoteServiceCallback> mCallbacks = new RemoteCallbackList<IRemoteServiceCallback>();

    private static final int HANDLER_MSG_SUCCESS = 1; //回调url
    private static final int HANDLER_MSG_FINISH = 2; //webview加载完成

    @Override
    public void onCreate() {
    }

    /**
     * onDestroy 不用我多说了吧
     */
    @Override
    public void onDestroy() {
        mCallbacks.kill();
        mHandler.removeMessages(0); //0会把所有任务移除掉
    }

    /**
     * 绑定多个Service
     *
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        if (IRemoteService.class.getName().equals(intent.getAction())) {
            return mBinder;
        }
        if (IService.class.getName().equals(intent.getAction())) {
            return mSecondaryBinder;
        }
        return null;
    }

    /**
     * 注册远程回调
     */
    private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {
        public void registerCallback(IRemoteServiceCallback cb) {
            if (cb != null) mCallbacks.register(cb);
        }

        public void unregisterCallback(IRemoteServiceCallback cb) {
            if (cb != null) mCallbacks.unregister(cb);
        }
    };

    private final IService.Stub mSecondaryBinder = new IService.Stub() {
        @Override
        public int getPid_Client() throws RemoteException {
            return Process.myPid();
        }

        @Override
        public void setUrl_Client(String extraUrl) throws RemoteException {
            //这里是从客户端传入的url，我这里启动了一个Activity，并且无界面显示  假装是个服务，2333333
            Intent snifferIntent = new Intent(getBaseContext(), WebViewActivity.class);
            snifferIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            snifferIntent.putExtra(WebViewActivity.EXTRA_URL, extraUrl);
            getApplication().startActivity(snifferIntent);
        }

        @Override
        public void findUrl_Service(String url) throws RemoteException {
            //url 回调给客户端
            Message msg = mHandler.obtainMessage(HANDLER_MSG_SUCCESS);
            msg.obj = url;
            mHandler.sendMessage(msg);
        }

        @Override
        public void webViewPageFinished_Service() throws RemoteException {
            //页面加载完成
            mHandler.sendEmptyMessage(HANDLER_MSG_FINISH);
        }
    };


    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // 开始广播
            final int N = mCallbacks.beginBroadcast();
            switch (msg.what) {
                case HANDLER_MSG_SUCCESS: {
                    String videoUrl = (String) msg.obj;
                    for (int i = 0; i < N; i++) {
                        try {
                            mCallbacks.getBroadcastItem(i).findUrl(videoUrl);
                        } catch (RemoteException e) {
                        }
                    }
                }
                break;
                case HANDLER_MSG_FINISH:
                    for (int i = 0; i < N; i++) {
                        try {
                            mCallbacks.getBroadcastItem(i).webViewPageFinished();
                        } catch (RemoteException e) {
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }

            //当所有注册的接口都回调完成之后，需要结束广播
            mCallbacks.finishBroadcast();
        }
    };
}
