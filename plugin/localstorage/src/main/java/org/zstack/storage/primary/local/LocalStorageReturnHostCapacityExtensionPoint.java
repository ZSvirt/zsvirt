package org.zstack.storage.primary.local;

/**
 * Created by frank on 10/16/2015.
 */
public interface LocalStorageReturnHostCapacityExtensionPoint {
    void beforeReturnLocalStorageCapacityOnHost(LocalStorageHostCapacityStruct struct);

}
