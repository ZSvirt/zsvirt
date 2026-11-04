package org.zstack.ha;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.core.db.Q;
import org.zstack.ha.entity.HostHaStateVO;
import org.zstack.ha.entity.HostHaStateVO_;
import org.zstack.ha.entity.HaStrategyConditionVO;
import org.zstack.ha.entity.HaStrategyConditionVO_;
import org.zstack.ha.header.HostHaState;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.ha.header.HaConstants;
import org.zstack.ha.header.HaGlobalConfig;
import org.zstack.ha.header.HaStrategyState;
import org.zstack.header.core.MevocoCoreGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: ya.wang
 * @Date: 12/20/24 11:39
 */
public class HaHelper {
    private static final CLogger logger = Utils.getLogger(HaHelper.class);

    public static boolean isVmHaEnabled() {
        return HaGlobalConfig.ALL.value(Boolean.class)
                && !MevocoCoreGlobalConfig.PAUSE_THE_WORLD.value(Boolean.class);
    }

    public static List<String> findExecuteFencers() {
        List<String> fencers = Q.New(HaStrategyConditionVO.class)
                .select(HaStrategyConditionVO_.fencerName)
                .eq(HaStrategyConditionVO_.state, HaStrategyState.Enable)
                .listValues();

        if (CollectionUtils.isEmpty(fencers) || !fencers.contains(HaConstants.HOST_STORAGE_STATE)) {
            fencers.add(HaConstants.HOST_STORAGE_STATE);
        }

        return fencers;
    }

    public static boolean isSelfFencerDone(String vmUuid, String hostUuid) {
        if (hostUuid == null) {
            return true;
        }
        String psUuid = Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .eq(VolumeVO_.type, VolumeType.Root)
                .select(VolumeVO_.primaryStorageUuid)
                .findValue();
        if (psUuid == null) {
            return true;
        }
        HostHaState state = Q.New(HostHaStateVO.class)
                .select(HostHaStateVO_.state)
                .eq(HostHaStateVO_.primaryStorageUuid, psUuid)
                .eq(HostHaStateVO_.hostUuid, hostUuid)
                .findValue();
        if (state != null && state != HostHaState.Done) {
            logger.debug(String.format("skip perform HA vm[uuid:%s] because the primary storage[uuid:%s]'s self-fencer state on host[uuid:%s] is not Done[current:%s]",
                    vmUuid, psUuid, hostUuid, state));
            return false;
        }
        return true;
    }

    // ------------------------------------- Primary Storage ------------------------------------

    public static List<PrimaryStorageInventory> getSelfFencerSupportedPrimaryStorage(HostInventory host) {
        List<PrimaryStorageVO> ps = Q.New(PrimaryStorageVO.class, PrimaryStorageClusterRefVO.class)
            .table0()
                .eq(PrimaryStorageVO_.uuid).table1(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                .selectThisTable()
            .table1()
                .eq(PrimaryStorageClusterRefVO_.clusterUuid, host.getClusterUuid())
            .list();
        if (ps.isEmpty()) {
            return new ArrayList<>();
        }

        List<PrimaryStorageInventory> invs = new ArrayList<>();
        for (PrimaryStorageVO vo : ps) {
            PrimaryStorageType type = PrimaryStorageType.valueOf(vo.getType());
            if (type.isSupportHeartbeatFile()) {
                invs.add(PrimaryStorageInventory.valueOf(vo));
            }
        }

        return invs;
    }
}
