package org.zstack.storage.cbt;

import org.zstack.header.HasThreadContext;
import org.zstack.header.agent.AgentCommand;
import org.zstack.header.agent.AgentResponse;
import org.zstack.header.cbt.VolumeCbtBackupInfo;
import org.zstack.header.cbt.VolumeCbtBackupRecordVO;
import org.zstack.kvm.VolumeTO;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class CbtBackupKvmCommands {
    public static final String REST_API_TAKE_VOLUME_CBT_BACKUP_PATH = "/vm/volume/takecbtbackup";
    public static final String REST_API_STOP_VOLUME_CBT_BACKUP_PATH = "/vm/volume/cancelcbtbackup";
    public static final String REST_API_GET_VOLUMES_BITMAPS_PATH = "/vm/volume/getvolumebitmaps";
    public static final String REST_API_EXPORT_NDB_VOLUMES_PATH = "/vm/volume/exportnbdvolumes";
    public static final String REST_API_UNEXPORT_NDB_VOLUMES_PATH = "/vm/volume/unexportnbdvolumes";
    public static final String REST_API_LIST_EXPORTED_VOLUMES_PATH = "/vm/volume/listexportedvolumes";


    public static class DisableVolumeCbtBackupCmd extends AgentCommand {
        public String vmUuid;
        public List<VolumeCbtBackupRecordVO> records;
    }

    public static class TakeVolumeCbtBackupCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String vmUuid;
        public List<VolumeCbtBackupInfo> volumeInfos;
        public String bitmapTimestamp;
        public String portRange;
    }

    public static class TakeVolumeCbtBackupResponse extends AgentResponse {
        public List<VolumeCbtBackupInfo> volumeInfos;
    }

    public static class GetVolumesBitmapCmd extends AgentCommand {
        public List<VolumeCbtBackupInfo> volumeInfos;
        public String bitmapTimestamp;
    }

    public static class GetVolumesBitmapResponse extends AgentResponse {
        public List<VolumeCbtBackupInfo> volumeInfos;
    }

    public static class ExportNbdVolumesCmd extends AgentCommand {
        public List<VolumeCbtBackupInfo> volumeInfos;
        public String portRange;
    }

    public static class ExportNbdVolumesResponse extends AgentResponse {
        public List<VolumeCbtBackupInfo> volumeInfos;
    }

    public static class UnexportNbdVolumesCmd extends AgentCommand {
        public List<VolumeTO> volumes;
    }

    public static class ListExportedVolumesCmd extends AgentCommand {
        public List<VolumeTO> volumes;
    }

    public static class ListExportedVolumesResponse extends AgentResponse {
        public Map<String, Boolean> volumeExportInfos;
    }
}
