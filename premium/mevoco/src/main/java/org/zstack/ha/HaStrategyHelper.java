package org.zstack.ha;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.upgrade.UpgradeGlobalConfig;
import org.zstack.header.volume.VolumeType;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.HashMap;
import java.util.Map;

public class HaStrategyHelper {
    private static final CLogger logger = Utils.getLogger(HaStrategyHelper.class);

    HaStrategyHelper(String primaryStorageType, Boolean storageConsistencySupported) {
        consistencyOffered.put(primaryStorageType, storageConsistencySupported);
    }

    private final static Map<String, Boolean> consistencyOffered = new HashMap<>();

    public static SelfFencerStrategy getVmHaFencerStrategy(String vmInstanceUuid) {
        ResourceConfigFacade rcf = Platform.getComponentLoader().getComponent(ResourceConfigFacade.class);
        if (SelfFencerStrategy.Force == SelfFencerStrategy.valueOf(rcf.getResourceConfigValue(HaGlobalConfig.VM_HA_STRATEGY, vmInstanceUuid, String.class))) {
            // hardcode for ZSTAC-50843
            String fencerStrategy = HaGlobalConfig.SELF_FENCER_STRATEGY.value();
            boolean isVirtualRouterVm = Q.New(VirtualRouterVmVO.class)
                    .eq(VirtualRouterVmVO_.uuid, vmInstanceUuid)
                    .isExists();
            boolean openGrayscaleUpgrade = UpgradeGlobalConfig.GRAYSCALE_UPGRADE.value(Boolean.class);
            if (openGrayscaleUpgrade && isVirtualRouterVm
                    && fencerStrategy.equals(SelfFencerStrategy.Permissive.name())) {
                logger.debug(String.format("vm[%s] fencer strategy return permissive, hardcode for ZSTAC-50843", vmInstanceUuid));
                return SelfFencerStrategy.Permissive;
            }

            return SelfFencerStrategy.Force;
        }

        Tuple tuple = getVmVolumePsUuidAndType(vmInstanceUuid);
        if (tuple == null) {
            logger.debug("No tuple means no primary storage of vm[uuid:%s] can be found return force");
            return SelfFencerStrategy.Force;
        }

        return getPrimaryStorageSelfFencerStrategy((String) tuple.get(0), (String) tuple.get(1));
    }

    private static Tuple getVmVolumePsUuidAndType(String vmUuid) {
        return SQL.New("select distinct ps.uuid, ps.type from PrimaryStorageVO ps, VolumeVO vol" +
                " where vol.vmInstanceUuid = :vmUuid and vol.type = :volumeType and ps.uuid = vol.primaryStorageUuid", Tuple.class)
                .param("vmUuid", vmUuid)
                .param("volumeType", VolumeType.Root)
                .find();
    }

    public static SelfFencerStrategy getPrimaryStorageSelfFencerStrategy(String primaryStorageUuid, String psType) {
        Boolean offered = consistencyOffered.get(psType);
        // if storage consistency is supported, no need to use self fencer strategy always return Force
        if (offered != null && offered) {
            return SelfFencerStrategy.Force;
        }

        ResourceConfigFacade rcf = Platform.getComponentLoader().getComponent(ResourceConfigFacade.class);
        return SelfFencerStrategy.valueOf(rcf.getResourceConfigValue(HaGlobalConfig.SELF_FENCER_STRATEGY, primaryStorageUuid, String.class));
    }
}
