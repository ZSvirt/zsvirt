package org.zstack.storage.primary.local;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by frank on 7/1/2015.
 */
@GlobalPropertyDefinition
public class LocalStorageGlobalProperty {
    @GlobalProperty(name="LocalStorage.kvm.SftpDownloadBitsCmd.timeout", defaultValue = "3600")
    public static int KVM_SftpDownloadBitsCmd_TIMEOUT;
    @GlobalProperty(name="LocalStorage.kvm.SftpUploadBitsCmd.timeout", defaultValue = "3600")
    public static int KVM_SftpUploadBitsCmd_TIMEOUT;
}
