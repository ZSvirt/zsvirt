package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.I18nMessage;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.i18m;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.isEmpty;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageFeatureAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void allocate() {
        if (isEmpty(spec.getRequiredFeatures())) {
            next();
            return;
        }

        List<PrimaryStorageVO> current = aliveVos();
        for (PrimaryStorageFeatureAllocatorExtensionPoint extp : pluginRgty.getExtensionList(PrimaryStorageFeatureAllocatorExtensionPoint.class)) {
            List<PrimaryStorageVO> ret;
            try {
                ret = extp.allocatePrimaryStorage(spec.getRequiredFeatures(), new ArrayList<>(current));
            } catch (OperationFailureException e) {
                rejectAll(e.getErrorCode());
                next();
                return;
            }
            if (ret == null) {
                continue;
            }
            if (ret.isEmpty()) {
                rejectAll(operr("PrimaryStorageFeatureAllocatorFlow[%s] returns zero primary storage candidate", extp.getClass().getName()));
                next();
                return;
            }
            rejectMissing(ret, i18m("excluded by %s", extp.getClass().getName()));
            current = aliveVos();
        }
        next();
    }

    private List<PrimaryStorageVO> aliveVos() {
        List<PrimaryStorageVO> vos = new ArrayList<>();
        for (PrimaryStorageCandidate candidate : candidates) {
            if (candidate.reject == null) {
                vos.add(candidate.primaryStorage);
            }
        }
        return vos;
    }

    private void rejectMissing(List<PrimaryStorageVO> kept, I18nMessage reason) {
        Set<String> keptUuids = new HashSet<>();
        for (PrimaryStorageVO vo : kept) {
            keptUuids.add(vo.getUuid());
        }
        for (PrimaryStorageCandidate candidate : candidates) {
            if (candidate.reject == null && !keptUuids.contains(candidate.getUuid())) {
                reject(candidate, reason);
            }
        }
    }
}
