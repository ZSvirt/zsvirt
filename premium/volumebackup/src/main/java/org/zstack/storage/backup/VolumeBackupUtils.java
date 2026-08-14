package org.zstack.storage.backup;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;

public class VolumeBackupUtils {
    private static final CLogger logger = Utils.getLogger(VolumeBackupUtils.class);

    public static List<String> filterDeletableBackupOrOnlyUpdateStatus(Set<String> toDeleteBackupUuids, String volumeUuid, List<String> backupStorageUuids) {
        if (toDeleteBackupUuids.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> deletableBackupUuids = new ArrayList<>();
        for (List<String> backupInSameChain : groupBackupByChain(volumeUuid).values()) {
            if (toDeleteBackupUuids.containsAll(backupInSameChain)) {
                deletableBackupUuids.addAll(backupInSameChain);
            }
        }

        List<String> updateStatusBackupUuids = new ArrayList<>(toDeleteBackupUuids);
        updateStatusBackupUuids.removeAll(deletableBackupUuids);
        if (!updateStatusBackupUuids.isEmpty()) {
            updateBackupStatus(updateStatusBackupUuids, backupStorageUuids, VolumeBackupStatus.Deleted);
        }

        return deletableBackupUuids;
    }

    public static Map<String, List<String>> groupBackupByChain(String volumeUuid) {
        List<String> lastBackupUuids = Q.New(VolumeBackupHistoryVO.class).eq(VolumeBackupHistoryVO_.uuid, volumeUuid)
                .select(VolumeBackupHistoryVO_.lastBackupUuid)
                .listValues();
        List<Tuple> ts = SQL.New("select ref.volumeBackupUuid, ref.installPath" +
                        " from VolumeBackupStorageRefVO ref, VolumeBackupVO backup" +
                        " where backup.volumeUuid = :volUuid" +
                        " and backup.status in :status" +
                        " and ref.volumeBackupUuid = backup.uuid" +
                        " order by backup.createDate desc", Tuple.class)
                .param("status", Arrays.asList(VolumeBackupStatus.Ready, VolumeBackupStatus.Deleted))
                .param("volUuid", volumeUuid)
                .list();

        if (!lastBackupUuids.isEmpty()) {
            ts.sort(Comparator.comparing(t -> lastBackupUuids.contains(t.get(0, String.class)) ? 0 : 1));
        }

        Map<String, List<String>> chains = new HashMap<>();
        ts.forEach(t -> {
            String chainName = getBackupChainName(t.get(1, String.class));
            chains.computeIfAbsent(chainName, k -> new ArrayList<>()).add(t.get(0, String.class));
        });
        return chains;
    }

    public static String getBackupChainName(String installPath) {
        return installPath.replace("zstore://", "").split("/")[0];
    }

    public static void updateBackupStatus(List<String> backupUuids, List<String> backupStorageUuids, VolumeBackupStatus status) {
        List<String> rets = Q.New(VolumeBackupStorageRefVO.class).select(VolumeBackupStorageRefVO_.volumeBackupUuid)
                .in(VolumeBackupStorageRefVO_.volumeBackupUuid, backupUuids)
                .in(VolumeBackupStorageRefVO_.backupStorageUuid, backupStorageUuids)
                .notEq(VolumeBackupVO_.status, status)
                .listValues();
        if (rets.isEmpty()) {
            return;
        }

        Set<String> needUpdateBackupUuids = new HashSet<>(rets);
        SQL.New(VolumeBackupStorageRefVO.class).in(VolumeBackupStorageRefVO_.volumeBackupUuid, needUpdateBackupUuids)
                .in(VolumeBackupStorageRefVO_.backupStorageUuid, backupStorageUuids)
                .set(VolumeBackupVO_.status, status)
                .update();

        logger.debug(String.format("update backup[uuids: %s, backupStorageUuids:%s] status to %s", needUpdateBackupUuids, backupStorageUuids, status));

        Q.New(VolumeBackupStorageRefVO.class).select(VolumeBackupStorageRefVO_.volumeBackupUuid)
                .in(VolumeBackupStorageRefVO_.volumeBackupUuid, needUpdateBackupUuids)
                .notEq(VolumeBackupVO_.status, status)
                .listValues()
                .forEach(needUpdateBackupUuids::remove);
        if (needUpdateBackupUuids.isEmpty()) {
            return;
        }

        SQL.New(VolumeBackupVO.class).in(VolumeBackupVO_.uuid, needUpdateBackupUuids)
                .set(VolumeBackupVO_.status, status)
                .update();
        logger.debug(String.format("update backup[uuids: %s] status to %s", needUpdateBackupUuids, status));
    }
}
