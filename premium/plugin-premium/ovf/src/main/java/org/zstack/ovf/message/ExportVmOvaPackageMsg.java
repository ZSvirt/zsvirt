package org.zstack.ovf.message;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.ovf.api.APIExportVmOvaPackageMsg;

/**
 * Created by Qi Le on 2022/5/5
 */
public class ExportVmOvaPackageMsg extends NeedReplyMessage {
    private String name;
    private String description;
    private String vmUuid;
    private String backupStorageUuid;

    private String resourceUuid;

    private SessionInventory session;

    public ExportVmOvaPackageMsg() {
    }

    public ExportVmOvaPackageMsg(APIExportVmOvaPackageMsg api) {
        this.name = api.getName();
        this.description = api.getDescription();
        this.vmUuid = api.getVmUuid();
        this.backupStorageUuid = api.getBackupStorageUuid();
        this.resourceUuid = api.getResourceUuid();
        this.session = api.getSession();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }
}
