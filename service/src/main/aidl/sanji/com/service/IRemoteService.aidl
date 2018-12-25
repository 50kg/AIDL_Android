// IRemoteService.aidl
package sanji.com.service;

import sanji.com.service.IRemoteServiceCallback;

// 客户端与服务端的注册回调

interface IRemoteService {
    /**
     * Often you want to allow a service to call back to its clients.
     * This shows how to do so, by registering a callback interface with
     * the service.
     */
    void registerCallback(IRemoteServiceCallback cb);

    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(IRemoteServiceCallback cb);
}