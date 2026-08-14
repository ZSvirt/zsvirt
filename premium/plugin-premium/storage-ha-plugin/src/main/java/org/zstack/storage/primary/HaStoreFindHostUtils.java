package org.zstack.storage.primary;


import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.ha.HaGlobalConfig;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;

import java.util.ArrayList;
import java.util.List;

public class HaStoreFindHostUtils {

    public static List<String> findSiblingFromOtherCluster(String clusterUuid, List<String> currentVmUsedCephStorageUuids) {
        if (!HaGlobalConfig.SUPPORT_HA_SLIBING_CROSS_CLUSTERS.value(Boolean.class)) {
            return new ArrayList<>();
        }

        List<L2NetworkClusterRefVO> l2refs = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid, clusterUuid).list();
        for (String psUuid : currentVmUsedCephStorageUuids) {
            for(L2NetworkClusterRefVO l2ref: l2refs) {
                String sql = "select psref.clusterUuid from PrimaryStorageClusterRefVO psref, L2NetworkClusterRefVO l2ref where " +
                        "psref.clusterUuid != :clusterUuid and l2ref.clusterUuid = psref.clusterUuid and l2ref.l2NetworkUuid = :l2Uuid and psref.primaryStorageUuid = :psUuid";
                List<String> clusters = SQL.New(sql)
                        .param("clusterUuid", clusterUuid)
                        .param("l2Uuid", l2ref.getL2NetworkUuid())
                        .param("psUuid", psUuid).list();
                if (clusters.isEmpty()) {
                    continue;
                }
                for (String uuid : clusters) {
                    List<String> res = findSiblingsFromSameCluster(null, uuid);
                    if (res != null && !res.isEmpty()) {
                        return res;
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    public static List<String> findSiblingsFromSameCluster(String hostUuid, String clusterUuid) {
        Q query = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .eq(HostVO_.status, HostStatus.Connected)
                .eq(HostVO_.state, HostState.Enabled)
                .eq(HostVO_.clusterUuid, clusterUuid)
                .limit(3);

        if (hostUuid != null) {
            query.notEq(HostVO_.uuid, hostUuid);
        }

        return query.listValues();
    }
}
