package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.BackupStorageMessage;

import java.util.List;

public class CancelExportNbdImagesMsg extends NeedReplyMessage implements BackupStorageMessage {
    private List<String> imagePaths;
    private List<Integer> ports;
    private String nbdDescription;
    private String backupStorageUuid;


    public List<String> getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = imagePaths;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }

    public String getNbdDescription() {
        return nbdDescription;
    }

    public void setNbdDescription(String nbdDescription) {
        this.nbdDescription = nbdDescription;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
