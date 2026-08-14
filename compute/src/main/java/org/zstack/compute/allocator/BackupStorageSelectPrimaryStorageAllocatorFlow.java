package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;

import static org.zstack.core.Platform.*;

/**
 * Created by xing5 on 2016/8/17.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BackupStorageSelectPrimaryStorageAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(BackupStorageSelectPrimaryStorageAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void allocate() {
        if (spec.getRequiredBackupStorageUuid() == null) {
            next();
            return;
        }

        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.type);
        q.add(BackupStorageVO_.uuid, Op.EQ, spec.getRequiredBackupStorageUuid());
        String type = q.findValue();
        BackupStorageType bsType = BackupStorageType.valueOf(type);

        List<String> psUuids = bsType.findRelatedPrimaryStorage(spec.getRequiredBackupStorageUuid());
        if (psUuids == null) {
            List<String> possiblePrimaryStorageTypes = spec.getBackupStoragePrimaryStorageMetrics().get(type);
            if (possiblePrimaryStorageTypes == null) {
                throw new OperationFailureException(inerr(
                        "the image[uuid:%s] is on the backup storage[uuid:%s, type:%s] that doesn't have metrics defined" +
                                " in conf/springConfigXml/HostAllocatorManager.xml. The developer should add its primary storage metrics",
                        spec.getImage().getUuid(), spec.getRequiredBackupStorageUuid(), type
                ));
            }

            List<String> result = findHostsByPrimaryStorageTypes(possiblePrimaryStorageTypes);
            for (HostCandidate candidate : candidates) {
                if (!result.contains(candidate.getUuid())) {
                    reject(candidate, i18m("need to attach backup storage with type %s for image[uuid:%s, name:%s]",
                            possiblePrimaryStorageTypes, spec.getImage().getUuid(), spec.getImage().getName()));
                }
            }
        } else if (!psUuids.isEmpty()) {
            List<String> result = findHostsByPrimaryStorageUuids(psUuids);
            for (HostCandidate candidate : candidates) {
                if (!result.contains(candidate.getUuid())) {
                    reject(candidate, i18m("need to attach backup storage %s for image[uuid:%s, name:%s]",
                            spec.getRequiredBackupStorageUuid(), spec.getImage().getUuid(), spec.getImage().getName()));
                }
            }
        } else {
            throw new OperationFailureException(operr("the backup storage[uuid:%s, type:%s] requires bound" +
                    " primary storage, however, the primary storage has not been added", spec.getRequiredBackupStorageUuid(), bsType));
        }


        next();
    }

    @Transactional(readOnly = true)
    private List<String> findHostsByPrimaryStorageUuids(List<String> psUuids) {
        String sql = "select distinct h.uuid" +
                " from HostVO h, PrimaryStorageClusterRefVO ref" +
                " where ref.clusterUuid = h.clusterUuid" +
                " and ref.primaryStorageUuid in (:psUuids)" +
                " and h.uuid in (:huuids)";

        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("psUuids", psUuids);
        q.setParameter("huuids", allHostUuidList());

        return q.getResultList();
    }

    @Transactional(readOnly = true)
    private List<String> findHostsByPrimaryStorageTypes(List<String> psTypes) {
        String sql = "select distinct h.uuid" +
                " from HostVO h, PrimaryStorageClusterRefVO ref, PrimaryStorageVO ps" +
                " where ref.clusterUuid = h.clusterUuid" +
                " and ref.primaryStorageUuid = ps.uuid" +
                " and ps.type in (:psTypes)" +
                " and h.uuid in (:huuids)";

        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("psTypes", psTypes);
        q.setParameter("huuids", allHostUuidList());

        return q.getResultList();
    }
}
