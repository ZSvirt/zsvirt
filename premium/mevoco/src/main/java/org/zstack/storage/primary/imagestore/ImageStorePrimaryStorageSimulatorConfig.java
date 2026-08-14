package org.zstack.storage.primary.imagestore;

import org.zstack.utils.data.SizeUnit;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by david on 8/6/16.
 */
public class ImageStorePrimaryStorageSimulatorConfig {
    public volatile boolean connectSuccess = true;
    public volatile String format = "qcow2";
    public volatile long totalCapacity = SizeUnit.GIGABYTE.toByte(1000);
    public Map<String, Long> imageSizes = new HashMap<String, Long>();
    public Map<String, Long> imageActualSizes = new HashMap<String, Long>();
}
