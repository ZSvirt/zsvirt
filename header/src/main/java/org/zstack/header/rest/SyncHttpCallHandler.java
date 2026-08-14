package org.zstack.header.rest;

/**
 * Created by frank on 11/1/2015.
 */
public interface SyncHttpCallHandler<T> extends HttpCallHandler {
    String handleSyncHttpCall(T cmd);
}
