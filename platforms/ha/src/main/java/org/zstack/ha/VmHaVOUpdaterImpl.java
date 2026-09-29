package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.ha.entity.VmHaVO;
import org.zstack.ha.entity.VmHaVO_;
import org.zstack.ha.fencers.KvmFencer;
import org.zstack.ha.header.HaSystemTags;
import org.zstack.ha.header.VmHaLevel;
import org.zstack.ha.spi.VmHaVOUpdater;
import org.zstack.ha.spi.VmInstanceHaLevelExtensionPoint;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.i18n;
import static org.zstack.ha.header.HaGlobalConfig.HOST_CHECK_INTERVAL;
import static org.zstack.ha.header.HaGlobalConfig.HOST_CHECK_MAX_ATTEMPTS;
import static org.zstack.ha.header.HaGlobalConfig.STORAGE_CHECKER_TIMEOUT;
import static org.zstack.ha.header.HaSystemTags.*;
import static org.zstack.ha.header.VmHaLevel.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.findOneOrNull;

public class VmHaVOUpdaterImpl implements VmHaVOUpdater {
    private static final CLogger logger = Utils.getLogger(VmHaVOUpdaterImpl.class);

    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private TimeHelper timeHelper;
    @Autowired
    private List<KvmFencer> fencers = new ArrayList<>();

    @Override
    public void update(VmHAExecutorParameters parameters) {
        VmHaLevel updateTo = parameters.getHaLevelTo();

        if (updateTo == null) {
            if (parameters.isInhibitHA()) {
                inhibitHATemporarily(parameters);
            } else {
                clearInhibitHABlocking(parameters.getVmUuid(), parameters.getInhibitHAReason());
            }
            return;
        }

        if (updateTo.isEnabled()) {
            enableVmHa(updateTo, parameters);
        } else {
            disableVmHa(parameters);
        }
    }

    @Override
    public void updateFence(VmHAFenceParameters parameters) {
        if (parameters.isClear()) {
            clearFence(parameters.getVmUuid());
            return;
        }

        SQL.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, parameters.getVmUuid())
                .set(VmHaVO_.lastFencerName, parameters.getFencerName())
                .set(VmHaVO_.lastFenceHostUuid, parameters.getHostUuid())
                .set(VmHaVO_.lastFenceTime, new Timestamp(timeHelper.getCurrentTimeMillis()))
                .update();
    }

    @Override
    public String findVmFencedReason(String vmUuid) {
        VmHaVO vo = Q.New(VmHaVO.class).eq(VmHaVO_.uuid, vmUuid).find();
        if (vo == null || vo.getLastFencerName() == null || vo.getLastFenceTime() == null) {
            return null;
        }

        String relatedFencer = vo.getLastFencerName();
        long fenceTime = vo.getLastFenceTime().getTime();
        final long durationMillis = defaultFencedReasonMaxDurationMillis();
        if (timeHelper.getCurrentTimeMillis() - fenceTime > durationMillis) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("lastFenceTime = %d, durationMillis = %d, clear expired fence",
                        fenceTime, durationMillis));
            }
            clearFence(vmUuid);
            return null;
        }

        KvmFencer fencer = findOneOrNull(fencers, f -> f.getName().equals(relatedFencer));
        if (fencer == null) {
            return i18n("VM fenced by %s", relatedFencer);
        }
        return fencer.vmFencedReason(relatedFencer);
    }

    private long defaultFencedReasonMaxDurationMillis() {
        int timeoutSeconds = STORAGE_CHECKER_TIMEOUT.value(Integer.class);
        long intervalSeconds = HOST_CHECK_INTERVAL.value(Long.class);
        int attempts = HOST_CHECK_MAX_ATTEMPTS.value(Integer.class);
        return (timeoutSeconds + intervalSeconds) * 1000 * attempts * 2;
    }

    private void clearFence(String vmUuid) {
        SQL.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, vmUuid)
                .set(VmHaVO_.lastFencerName, null)
                .set(VmHaVO_.lastFenceHostUuid, null)
                .set(VmHaVO_.lastFenceTime, null)
                .update();
    }

    @SuppressWarnings("unchecked")
    private void enableVmHa(VmHaLevel updateTo, VmHAExecutorParameters parameters) {
        String vmUuid = parameters.getVmUuid();
        String originTag = HA.getTokenByResourceUuid(vmUuid, HaSystemTags.HA_TOKEN);
        final VmHaLevel originLevel;

        if (originTag == null) {
            SystemTagCreator creator = HA.newSystemTagCreator(vmUuid);
            creator.setTagByTokens(map(e(HaSystemTags.HA_TOKEN, None)));
            creator.create();
            originLevel = None;
        } else {
            originLevel = VmHaLevel.valueOf(originTag);
        }

        if (originLevel == updateTo) {
            return;
        }

        if (parameters.isInhibitHA()) {
            addInhibitHAWithoutChecking(vmUuid, parameters.getInhibitHAReason());
        } else {
            clearInhibitHABlocking(vmUuid, "HA level set to " + updateTo);
        }

        HA.update(vmUuid, HA.instantiateTag(map(e(HA_TOKEN, updateTo))));

        CollectionUtils.safeForEach(
                pluginRegistry.getExtensionList(VmInstanceHaLevelExtensionPoint.class),
                ext -> ext.afterSetVmInstanceHaLevel(vmUuid, originLevel, updateTo));
    }

    @SuppressWarnings("unchecked")
    private void disableVmHa(VmHAExecutorParameters parameters) {
        String vmUuid = parameters.getVmUuid();
        String originTag = HA.getTokenByResourceUuid(vmUuid, HaSystemTags.HA_TOKEN);
        final VmHaLevel originLevel;

        if (originTag == null) {
            SystemTagCreator creator = HA.newSystemTagCreator(vmUuid);
            creator.setTagByTokens(map(e(HaSystemTags.HA_TOKEN, None)));
            creator.create();
            originLevel = None;
        } else {
            originLevel = VmHaLevel.valueOf(originTag);
        }

        if (originLevel == None) {
            return;
        }

        clearInhibitHABlocking(vmUuid, "HA level set to None");
        HA.update(vmUuid, HA.instantiateTag(map(e(HA_TOKEN, None))));

        CollectionUtils.safeForEach(
                pluginRegistry.getExtensionList(VmInstanceHaLevelExtensionPoint.class),
                ext -> ext.afterSetVmInstanceHaLevel(vmUuid, originLevel, None));
    }

    private void inhibitHATemporarily(VmHAExecutorParameters parameters) {
        String originTag = HA.getTokenByResourceUuid(parameters.getVmUuid(), HaSystemTags.HA_TOKEN);
        boolean haEnabled = originTag != null && !VmHaLevel.None.toString().equals(originTag);

        if (!haEnabled) {
            return;
        }

        addInhibitHAWithoutChecking(parameters.getVmUuid(), parameters.getInhibitHAReason());
    }

    private void addInhibitHAWithoutChecking(String vmUuid, String reason) {
        SystemTagCreator creator = INHIBIT_HA.newSystemTagCreator(vmUuid);
        creator.inherent = false;
        creator.recreate = true;
        creator.create();

        SQL.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, vmUuid)
                .set(VmHaVO_.inhibitionReason, reason)
                .set(VmHaVO_.inhibitionTime, new Timestamp(timeHelper.getCurrentTimeMillis()))
                .update();

        logger.info(String.format("create inhibit-HA tag to VM[uuid:%s]: %s", vmUuid, reason));
    }

    private void clearInhibitHABlocking(String vmUuid, String reason) {
        final boolean success = INHIBIT_HA.delete(vmUuid);

        SQL.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, vmUuid)
                .set(VmHaVO_.inhibitionReason, null)
                .update();

        if (success) {
            logger.info(String.format("delete inhibit-HA tag to VM[uuid:%s]: %s", vmUuid, reason));
        }
    }
}
