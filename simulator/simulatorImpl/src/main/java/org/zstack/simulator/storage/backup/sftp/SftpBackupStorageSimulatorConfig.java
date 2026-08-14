package org.zstack.simulator.storage.backup.sftp;

import org.zstack.storage.backup.sftp.SftpBackupStorageCommands;
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands.GetImageSizeCmd;
import org.zstack.utils.data.SizeUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SftpBackupStorageSimulatorConfig {
    public volatile boolean connectSuccess = true;
    public volatile long totalCapacity = SizeUnit.GIGABYTE.toByte(1000);
    public volatile long usedCapacity;
    public volatile long availableCapacity = SizeUnit.GIGABYTE.toByte(1000);
    public volatile boolean downloadSuccess1 = true;
    public volatile boolean downloadSuccess2 = true;
    public Map<String, Long> imageSizes = new HashMap<String, Long>();
    public Map<String, Long> imageActualSizes = new HashMap<String, Long>();
    public volatile String imageMd5sum;
    public volatile boolean deleteSuccess = true;
    public volatile boolean pingSuccess = true;
    public volatile boolean pingException = false;
    public volatile String bsUuid;
    public volatile boolean getSshkeySuccess = true;
    public volatile boolean getSshkeyException = false;
    public volatile List<SftpBackupStorageCommands.DeleteCmd> deleteCmds = new ArrayList<SftpBackupStorageCommands.DeleteCmd>();
    public List<GetImageSizeCmd> getImageSizeCmds = new ArrayList<GetImageSizeCmd>();
    public Map<String, Long> getImageSizeCmdActualSize = new HashMap<String, Long>();
    public Map<String, Long> getImageSizeCmdSize = new HashMap<String, Long>();
}
