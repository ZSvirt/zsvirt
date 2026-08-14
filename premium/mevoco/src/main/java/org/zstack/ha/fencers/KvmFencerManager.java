package org.zstack.ha.fencers;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.ha.HaSystemTags;
import org.zstack.ha.SelfFencerKvmBackend;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceStartExtensionPoint;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.i18n;
import static org.zstack.ha.HaGlobalConfig.*;
import static org.zstack.ha.HaSystemTags.VM_FENCED_BY;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.findOneOrNull;

public class KvmFencerManager implements VmInstanceStartExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmFencerManager.class);

    @Autowired
    private List<KvmFencer> fencers = new ArrayList<>();

    public String findVmFencedReasonByTag(String vmUuid) {
        final SystemTagInventory tag = VM_FENCED_BY.getTagInventory(vmUuid);
        if (tag == null) {
            return null;
        }

        String relatedFencer = VM_FENCED_BY.getTokenByTag(tag.getTag(), HaSystemTags.VM_FENCED_TOKEN);
        KvmFencer fencer = findOneOrNull(fencers, f -> f.getName().equals(relatedFencer));

        long tagCreateTime = tag.getCreateDate().getTime();
        final long durationMillis = defaultFencedReasonMaxDurationMillis();
        if (fencer == null) {
            if (System.currentTimeMillis() - tagCreateTime > durationMillis) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("fencerBy.tagCreateTime = %d, durationMillis = %d, delete expired tag",
                            tagCreateTime, durationMillis));
                }
                VM_FENCED_BY.deleteInherentTag(vmUuid);
                return null;
            }
            return i18n("VM fenced by %s", relatedFencer);
        }

        if (System.currentTimeMillis() - tagCreateTime > durationMillis) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("fencerBy.tagCreateTime = %d, durationMillis = %d, delete expired tag",
                        tagCreateTime, durationMillis));
            }
            VM_FENCED_BY.deleteInherentTag(vmUuid);
            return null;
        }

        return fencer.vmFencedReason(tag);
    }

    private long defaultFencedReasonMaxDurationMillis() {
        int timeoutSeconds = STORAGE_CHECKER_TIMEOUT.value(Integer.class);
        long intervalSeconds = HOST_CHECK_INTERVAL.value(Long.class);
        int attempts = HOST_CHECK_MAX_ATTEMPTS.value(Integer.class);
        return (timeoutSeconds + intervalSeconds) * 1000 * attempts * 2;
    }

    @SuppressWarnings("unchecked")
    public void createFencedByTag(SelfFencerKvmBackend.ReportVmSelfFencerTuple tuple) {
        SystemTagCreator creator = VM_FENCED_BY.newSystemTagCreator(tuple.vmUuid);
        creator.setTagByTokens(map(e(HaSystemTags.VM_FENCED_TOKEN, tuple.fencerName)));
        creator.inherent = true;
        creator.recreate = true;
        creator.create();
    }

    @Override
    public String preStartVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {
        // do-nothing
    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {
        VM_FENCED_BY.deleteInherentTag(inv.getUuid());
    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {
        VM_FENCED_BY.deleteInherentTag(inv.getUuid());
    }
}
