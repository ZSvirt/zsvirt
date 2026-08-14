package org.zstack.storage.cbt;

import org.zstack.header.cbt.CbtTaskVO;
import org.zstack.header.cbt.VolumeCbtBackupInfo;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;

import java.util.List;
import java.util.Map;

public interface CbtBackupHypervisorBackend {
    void takeVolumeCbtBackup(String vmUuid, String hostUuid, List<VolumeCbtBackupInfo> volumeInfos, String bitmapName, ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion);
    void disableVolumeCbtBackup(CbtTaskVO vo, String vmUuid, String hostUuid, Completion completion);
    void getVolumesBitmap(List<VolumeCbtBackupInfo> volumeInfos, String hostUuid, String bitmapName, ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion);
    void exportNbdVolumesForRecovery(String vmUuid, List<String> volumeUuids, ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion);
    void unexportNbdVolumesForRecovery(String vmUuid, List<String> volumeUuids, Completion completion);
    void listExportedVolumesForRecovery(String vmUuid, List<String> volumeUuids, ReturnValueCompletion<Map<String, Boolean>> completion);
}
