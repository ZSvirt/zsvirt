package org.zstack.storage.backup.imagestore;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;

public class ImageStoreAgentDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_imagestorebackupstorage")
    private final String packageName = ImageStoreBackupStorageGlobalProperty.AGENT_PACKAGE_NAME;
    @SerializedName("client")
    private String client;
    @SerializedName("fs_rootpath")
    private String fsRootPath;
    @SerializedName("max_capacity")
    private String maxCapacity;
    @SerializedName("skip_packages")
    private String skipPackages;
    @SerializedName("new_add")
    private String newAdd;

    public String getNewAdd() {
        return newAdd;
    }

    public void setNewAdd(String newAdd) {
        this.newAdd = newAdd;
    }

    public String getFsRootPath() {
        return fsRootPath;
    }

    public void setFsRootPath(String fsRootPath) {
        this.fsRootPath = fsRootPath;
    }

    public String getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(String maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getSkipPackages() {
        return skipPackages;
    }

    public void setSkipPackages(String skipPackages) {
        this.skipPackages = skipPackages;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }
}
