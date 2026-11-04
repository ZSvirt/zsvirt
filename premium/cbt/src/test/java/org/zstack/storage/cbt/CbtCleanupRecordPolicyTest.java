package org.zstack.storage.cbt;

import org.junit.Test;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.cbt.VolumeCbtBackupRecordVO;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CbtCleanupRecordPolicyTest {
    @Test
    public void failedCleanupUsesOnlyPendingRecordsAndKeepsAttemptsSeparate() {
        VolumeCbtBackupRecordVO history = record(1, "volume-1", null);
        VolumeCbtBackupRecordVO oldAttempt = record(2, "volume-1", "attempt-1");
        VolumeCbtBackupRecordVO currentAttemptVolume1 = record(3, "volume-1", "attempt-2");
        VolumeCbtBackupRecordVO currentAttemptVolume2 = record(4, "volume-2", "attempt-2");

        List<List<VolumeCbtBackupRecordVO>> attempts = CbtCleanupRecordPolicy.rollbackAttempts(
                Arrays.asList(history, oldAttempt, currentAttemptVolume1, currentAttemptVolume2));

        assertEquals(2, attempts.size());
        assertEquals(Arrays.asList(currentAttemptVolume1, currentAttemptVolume2), attempts.get(0));
        assertEquals(Arrays.asList(oldAttempt), attempts.get(1));
        assertTrue(CbtCleanupRecordPolicy.rollbackAttempts(Arrays.asList(history)).isEmpty());
    }

    @Test
    public void normalCleanupUsesLatestSuccessfulRecordPerVolume() {
        VolumeCbtBackupRecordVO oldHistory = record(1, "volume-1", null);
        VolumeCbtBackupRecordVO pending = record(2, "volume-1", "attempt-1");
        VolumeCbtBackupRecordVO latestHistory = record(3, "volume-1", null);
        VolumeCbtBackupRecordVO otherVolume = record(4, "volume-2", null);

        List<VolumeCbtBackupRecordVO> records = CbtCleanupRecordPolicy.stopRecords(
                Arrays.asList(oldHistory, pending, latestHistory, otherVolume));

        assertEquals(Arrays.asList(latestHistory, otherVolume), records);
    }

    @Test
    public void deferredCleanupUsesTimeBasedGc() {
        assertTrue(TimeBasedGarbageCollector.class.isAssignableFrom(CbtTaskCleanupGC.class));
        assertTrue(TimeBasedGarbageCollector.class.isAssignableFrom(UnexportNbdVolumesGC.class));
    }

    @Test
    public void scratchVolumesAreRetainedUntilPendingRollbackFinishes() {
        VolumeCbtBackupRecordVO history = record(1, "volume-1", null);
        VolumeCbtBackupRecordVO pending = record(2, "volume-1", "attempt-1");

        assertFalse(CbtCleanupRecordPolicy.shouldRetainScratchVolumes(Arrays.asList(history)));
        assertTrue(CbtCleanupRecordPolicy.shouldRetainScratchVolumes(Arrays.asList(history, pending)));
    }

    private VolumeCbtBackupRecordVO record(long id, String volumeUuid, String cleanupAttemptUuid) {
        VolumeCbtBackupRecordVO record = new VolumeCbtBackupRecordVO();
        record.setId(id);
        record.setVolumeUuid(volumeUuid);
        record.setCleanupAttemptUuid(cleanupAttemptUuid);
        return record;
    }
}
