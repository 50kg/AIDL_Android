// IService.aidl
package sanji.com.service;

/**
 * 客户端调用服务端的方法 写在这里
 */
interface IService {
    /**
     * 获取当前服务的pid
     */
    int getPid_Client();

    /**
     * 设置要嗅探的URL
     */
    void setUrl_Client(String extraUrl);

//============上面：从客户端调用服务器代码===================================
//============下面：从Activity回调到Service===================================
    /**
     * 找到的url
     */
    void findUrl_Service(String videoUrl);

    /**
     * 网页加载完成
     */
    void webViewPageFinished_Service();
}