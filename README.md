# AIDL_Android
Android使用AIDL实现IPC通信——客户端服务端双向通信发送回调数据

此Demo演示了

1：Client和Service线程的相互通信。Client启动Service绑定并传值，Service处理数据后回调给客户端。自动绑定、异常死亡后启动服务等。Demo里例子是客户端传入网址，服务端接收并访问，回调 onLoadResource 的所有Url给客户端

2：服务端隐藏桌面图标、切换多任务隐藏预览、无界面显示Activity(可以理解为一个服务，但是是个看不见的Activity)

目录结构：

module app(client)  作为客户端运行

module service 作为服务端运行（AndroidManifest 里service的intent-filter记得添加 ）

 

Client隐试启动Service --》Client 绑定 Service 的RemoteService  --》Client调用Service的远程方法(可传值) --》

RemoteService里接受数据(接收到数据就自行处理了，我又启动了一个隐藏的Activity)--》WebViewActivity 绑定RemoteService

--》处理数据后回调到RemoteService--》RemoteService再回调到Client

 

大概就这个流程

如有不足，请多指教~~~
--------------------- 
作者：sanji2020 
来源：CSDN 
原文：https://blog.csdn.net/sanji2020/article/details/85243118 
版权声明：本文为博主原创文章，转载请附上博文链接！
