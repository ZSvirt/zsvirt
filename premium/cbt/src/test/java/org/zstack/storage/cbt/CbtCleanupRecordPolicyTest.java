package org.zstack.storage.cbt;

import org.junit.Test;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.cbt.VolumeCbtBackupRecordVO;
import org.zstack.header.volume.VolumeVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CbtCleanupRecordPolicyTest {
    @Test
    public void cephCleanupMatchesPathsWithoutHostMonOrSecretLookup() {
        VolumeCbtBackupRecordVO active = record(1, "source", null);
        active.setTarget("ceph://pool/scratch");
        VolumeCbtBackupRecordVO stale = record(2, "detached", null);
        stale.setTarget("ceph://pool/deleted-scratch");

        assertEquals(Collections.singletonList(active), CbtCleanupRecordPolicy.stopRecordsForVolumes(
                Arrays.asList(active, stale), Collections.singletonList(scratch("ceph://pool/scratch"))));
    }

    @Test
    public void sharedBlockAndBlockCleanupKeepDevicePathConversions() {
        VolumeCbtBackupRecordVO sharedBlock = record(1, "source-1", null);
        sharedBlock.setTarget("/dev/vg/scratch");
        VolumeCbtBackupRecordVO block = record(2, "source-2", null);
        block.setTarget("/dev/disk/by-id/wwn-0x1234");
        VolumeCbtBackupRecordVO stale = record(3, "detached", null);
        stale.setTarget("/dev/vg/deleted-scratch");

        assertEquals(Arrays.asList(sharedBlock, block), CbtCleanupRecordPolicy.stopRecordsForVolumes(
                Arrays.asList(stale, block, sharedBlock),
                Arrays.asList(scratch("sharedblock://vg/scratch"), scratch("block://1234"))));
    }

    @Test
    public void filteringKeepsLatestActiveRecordAndExcludesPendingRollback() {
        VolumeCbtBackupRecordVO old = record(1, "source", null);
        VolumeCbtBackupRecordVO active = record(2, "source", null);
        VolumeCbtBackupRecordVO pending = record(3, "source", "attempt");
        VolumeCbtBackupRecordVO deleted = record(4, "source", null);
        old.setTarget("/localStorage/scratch.qcow2");
        active.setTarget(old.getTarget());
        pending.setTarget(old.getTarget());
        deleted.setTarget("/localStorage/deleted.qcow2");

        List<VolumeCbtBackupRecordVO> records = Arrays.asList(old, active, pending, deleted);
        assertEquals(Collections.singletonList(active), CbtCleanupRecordPolicy.stopRecordsForVolumes(
                records, Collections.singletonList(scratch(active.getTarget()))));
        assertTrue(CbtCleanupRecordPolicy.stopRecordsForVolumes(records,
                Arrays.asList(scratch(null), scratch("/unrelated"))).isEmpty());
        assertTrue(CbtCleanupRecordPolicy.stopRecordsForVolumes(records, Collections.emptyList()).isEmpty());
        assertEquals(4, records.size());
    }

    private VolumeVO scratch(String path) {
        VolumeVO volume = new VolumeVO();
        volume.setInstallPath(path);
        return volume;
    }

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
