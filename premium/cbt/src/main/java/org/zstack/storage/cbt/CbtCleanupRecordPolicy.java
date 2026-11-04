package org.zstack.storage.cbt;

import org.zstack.header.cbt.VolumeCbtBackupRecordVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CbtCleanupRecordPolicy {
    private CbtCleanupRecordPolicy() {
    }

    static List<List<VolumeCbtBackupRecordVO>> rollbackAttempts(List<VolumeCbtBackupRecordVO> records) {
        List<VolumeCbtBackupRecordVO> pendingRecords = new ArrayList<>();
        for (VolumeCbtBackupRecordVO record : records) {
            if (record.getCleanupAttemptUuid() != null) {
                pendingRecords.add(record);
            }
        }
        pendingRecords.sort(Comparator.comparingLong(VolumeCbtBackupRecordVO::getId).reversed());

        Map<String, List<VolumeCbtBackupRecordVO>> attempts = new LinkedHashMap<>();
        for (VolumeCbtBackupRecordVO record : pendingRecords) {
            attempts.computeIfAbsent(record.getCleanupAttemptUuid(), ignored -> new ArrayList<>()).add(record);
        }

        List<List<VolumeCbtBackupRecordVO>> result = new ArrayList<>(attempts.values());
        for (List<VolumeCbtBackupRecordVO> attempt : result) {
            attempt.sort(Comparator.comparingLong(VolumeCbtBackupRecordVO::getId));
        }
        return result;
    }

    static boolean shouldRetainScratchVolumes(List<VolumeCbtBackupRecordVO> records) {
        return records.stream().anyMatch(record -> record.getCleanupAttemptUuid() != null);
    }

    static List<VolumeCbtBackupRecordVO> stopRecords(List<VolumeCbtBackupRecordVO> records) {
        List<VolumeCbtBackupRecordVO> successfulRecords = new ArrayList<>();
        for (VolumeCbtBackupRecordVO record : records) {
            if (record.getCleanupAttemptUuid() == null) {
                successfulRecords.add(record);
            }
        }
        successfulRecords.sort(Comparator.comparingLong(VolumeCbtBackupRecordVO::getId).reversed());

        Map<String, VolumeCbtBackupRecordVO> latestByVolume = new LinkedHashMap<>();
        for (VolumeCbtBackupRecordVO record : successfulRecords) {
            latestByVolume.putIfAbsent(record.getVolumeUuid(), record);
        }

        List<VolumeCbtBackupRecordVO> result = new ArrayList<>(latestByVolume.values());
        result.sort(Comparator.comparingLong(VolumeCbtBackupRecordVO::getId));
        return result;
    }
}
