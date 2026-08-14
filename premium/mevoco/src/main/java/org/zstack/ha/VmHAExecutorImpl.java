package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SQL;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;

import static org.zstack.ha.HaSystemTags.*;
import static org.zstack.ha.VmHaLevel.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class VmHAExecutorImpl implements VmHAExecutor {
    private static final CLogger logger = Utils.getLogger(VmHAExecutorImpl.class);

    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private TimeHelper timeHelper;

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
