package org.zstack.storage.backup.imagestore;

import org.zstack.utils.data.SizeUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by miao on 16-7-13.
 */
public class ImageStoreBackupStorageSimulatorConfig {
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
    public List<ImageStoreBackupStorageCommands.DeleteCmd> deleteCmds =
            new ArrayList<ImageStoreBackupStorageCommands.DeleteCmd>();
    public List<ImageStoreBackupStorageCommands.GetImageInfoCmd> getImageInfoCmds =
            new ArrayList<ImageStoreBackupStorageCommands.GetImageInfoCmd>();
    public Map<String, Long> getImageSizeCmdActualSize = new HashMap<String, Long>();
    public Map<String, Long> getImageSizeCmdSize = new HashMap<String, Long>();
}
