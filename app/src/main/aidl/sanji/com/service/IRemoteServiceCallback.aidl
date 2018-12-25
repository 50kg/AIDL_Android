// IRemoteServiceCallback.aidl
package sanji.com.service;

/**
 * 从服务端回调给客户端的方法
 */
oneway interface IRemoteServiceCallback {
    //回调的url
    void findUrl(String findUrl);
    //网页加载完成
    void webViewPageFinished();
}
