package org.zstack.appliancevm;


import org.zstack.header.core.NoErrorCompletion;

/**
 * Created by shixin on 2019/06/27.
 */
public interface ApplianceVmSyncConfigToHaGroupExtensionPoint {
    void applianceVmSyncConfigToHa(ApplianceVmInventory inv, String haUuid);
    void applianceVmSyncConfigToHaRollback(ApplianceVmInventory inv, String haUuid);
    void applianceVmSyncConfigAfterAddToHaGroup(ApplianceVmInventory inv, String haUuid, NoErrorCompletion completion);
}
