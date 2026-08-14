package org.zstack.storage.ceph.backup;

import org.zstack.storage.ceph.backup.CephBackupStorageBase.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/28/2015.
 */
public class CephBackupStorageSimulatorConfig {
    public static class CephBackupStorageConfig {
        public long totalCapacity;
        public long availCapacity;
        public String fsid;
        public String name;
    }

    public volatile boolean monInitSuccess = true;
    public List<InitCmd> initCmds = new ArrayList<InitCmd>();
    public Map<String, CephBackupStorageConfig> config = new HashMap<String, CephBackupStorageConfig>();
    public List<DownloadCmd> downloadCmds = new ArrayList<DownloadCmd>();
    public List<DeleteCmd> deleteCmds = new ArrayList<DeleteCmd>();
    public List<CheckImageMetaDataFileExistCmd> checkMetadataFileCmds = new ArrayList<CheckImageMetaDataFileExistCmd>();
    public List<DeleteImageInfoFromMetaDataFileCmd> deleteImageInfoFromMetadataFileCmds = new ArrayList<DeleteImageInfoFromMetaDataFileCmd>();
    public List<DumpImageInfoToMetaDataFileCmd> dumpImageInfoToMetaDataFileCmds = new ArrayList<DumpImageInfoToMetaDataFileCmd>();
    public List<GetImagesMetaDataCmd> getImageInfoToMetaDataFileCmds = new ArrayList<GetImagesMetaDataCmd>();
    public List<PingCmd> pingCmds = new ArrayList<PingCmd>();
    public Map<String, Long> imageSize = new HashMap<String, Long>();
    public Map<String, Long> imageActualSize = new HashMap<String, Long>();

    public List<GetImageSizeCmd> getImageSizeCmds = new ArrayList<GetImageSizeCmd>();
    public Map<String, Long> getImageSizeCmdSize = new HashMap<String, Long>();
    public Map<String, Long> getImageSizeCmdActualSize = new HashMap<String, Long>();

    public Map<String, Boolean> pingCmdSuccess = new HashMap<String, Boolean>();
    public Map<String, PingOperationFailure> pingCmdOperationFailure = new HashMap<>();
    public List<GetFactsCmd> getFactsCmds = new ArrayList<GetFactsCmd>();
    public Map<String, String> getFactsCmdFsid = new HashMap<String, String>();
    public Map<String, String> monAddr = new HashMap<>();
}
