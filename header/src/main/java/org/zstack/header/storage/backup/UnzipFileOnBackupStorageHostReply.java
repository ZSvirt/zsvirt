package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;

import java.util.Map;

public class UnzipFileOnBackupStorageHostReply extends MessageReply {
    private String unzipInstallPath;
    private Map<String, Long> fileSizes;

    public String getUnzipInstallPath() {
        return unzipInstallPath;
    }

    public void setUnzipInstallPath(String unzipInstallPath) {
        this.unzipInstallPath = unzipInstallPath;
    }

    public Map<String, Long> getFileSizes() {
        return fileSizes;
    }

    public void setFileSizes(Map<String, Long> fileSizes) {
        this.fileSizes = fileSizes;
    }
}
