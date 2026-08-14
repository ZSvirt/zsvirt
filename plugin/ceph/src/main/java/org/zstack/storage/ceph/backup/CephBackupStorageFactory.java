package org.zstack.storage.ceph.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.ceph.*;
import org.zstack.tag.SystemTagCreator;

import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
public class CephBackupStorageFactory implements BackupStorageFactory, CephCapacityUpdateExtensionPoint, Component {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AnsibleFacade asf;

    public static final BackupStorageType type = new BackupStorageType(CephConstants.CEPH_BACKUP_STORAGE_TYPE);

    static {
        type.setOrder(899);
    }

    void init() {
        type.setPrimaryStorageFinder(new BackupStorageFindRelatedPrimaryStorage() {
            @Override
            @Transactional(readOnly = true)
            public List<String> findRelatedPrimaryStorage(String backupStorageUuid) {
                String sql = "select p.uuid from CephPrimaryStorageVO p, CephBackupStorageVO b where b.fsid = p.fsid" +
                        " and b.uuid = :buuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("buuid", backupStorageUuid);
                return q.getResultList();
            }
        });
    }

    @Override
    public BackupStorageType getBackupStorageType() {
        return type;
    }

    @Override
    @Transactional
    public BackupStorageInventory createBackupStorage(final BackupStorageVO vo, APIAddBackupStorageMsg msg) {
        APIAddCephBackupStorageMsg cmsg = (APIAddCephBackupStorageMsg)msg;

        CephBackupStorageVO cvo = new CephBackupStorageVO(vo);
        cvo.setType(CephConstants.CEPH_BACKUP_STORAGE_TYPE);
        String poolName = cmsg.getPoolName() == null ? String.format("bak-t-%s", vo.getUuid()) : cmsg.getPoolName();
        cvo.setPoolName(poolName);

        dbf.getEntityManager().persist(cvo);

        if (cmsg.getPoolName() != null) {
            SystemTagCreator creator = CephSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }

        for (String url : cmsg.getMonUrls()) {
            CephBackupStorageMonVO monvo = new CephBackupStorageMonVO();
            MonUri uri = new MonUri(url);
            monvo.setUuid(Platform.getUuid());
            monvo.setStatus(MonStatus.Connecting);
            monvo.setHostname(uri.getHostname());
            monvo.setMonAddr(monvo.getHostname());
            monvo.setMonPort(uri.getMonPort());
            monvo.setSshPort(uri.getSshPort());
            monvo.setSshUsername(uri.getSshUsername());
            monvo.setSshPassword(uri.getSshPassword());
            monvo.setBackupStorageUuid(cvo.getUuid());
            dbf.getEntityManager().persist(monvo);
        }

        return BackupStorageInventory.valueOf(cvo);
    }

    @Override
    public BackupStorage getBackupStorage(BackupStorageVO vo) {
        CephBackupStorageVO cvo = dbf.findByUuid(vo.getUuid(), CephBackupStorageVO.class);
        return new CephBackupStorageBase(cvo);
    }

    @Override
    public BackupStorageInventory reload(String uuid) {
        return CephBackupStorageInventory.valueOf(dbf.findByUuid(uuid, CephBackupStorageVO.class));
    }

    @Override
    @Transactional
    public void update(CephCapacity cephCapacity) {
        String fsid = cephCapacity.getFsid();
        long total = cephCapacity.getTotalCapacity();
        long avail = cephCapacity.getAvailableCapacity();
        List<CephPoolCapacity> poolCapacities = cephCapacity.getPoolCapacities();
        boolean enterpriseCeph = cephCapacity.isEnterpriseCeph();

        String sql = "select c from CephBackupStorageVO c where c.fsid = :fsid";
        TypedQuery<CephBackupStorageVO> q = dbf.getEntityManager().createQuery(sql, CephBackupStorageVO.class);
        q.setParameter("fsid", fsid);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        try {
            CephBackupStorageVO vo = q.getSingleResult();
            vo.setTotalCapacity(total);
            vo.setAvailableCapacity(avail);

            if (poolCapacities != null && poolCapacities.stream().anyMatch((e) -> vo.getPoolName().equals(e.getName()))) {
                CephPoolCapacity poolCapacity = poolCapacities.stream()
                        .filter(e -> vo.getPoolName().equals(e.getName()))
                        .findAny().get();
                if (poolCapacity.getRelatedOsdCapacity() != null) {
                    total = 0;
                    avail = 0;
                    for (String osdName : poolCapacity.getRelatedOsdCapacity().keySet()) {
                        total += poolCapacity.getRelatedOsdCapacity().get(osdName).getSize() * poolCapacity.getDiskUtilization();
                        avail += poolCapacity.getRelatedOsdCapacity().get(osdName).getAvailableCapacity() * poolCapacity.getDiskUtilization();
                    }
                    vo.setTotalCapacity(total);
                    vo.setAvailableCapacity(avail);
                } else {
                    vo.setTotalCapacity(poolCapacity.getTotalCapacity());
                    vo.setAvailableCapacity(poolCapacity.getAvailableCapacity());
                }
                vo.setPoolAvailableCapacity(poolCapacity.getAvailableCapacity());
                vo.setPoolReplicatedSize(poolCapacity.getReplicatedSize());
                vo.setPoolUsedCapacity(poolCapacity.getUsedCapacity());
                vo.setPoolSecurityPolicy(poolCapacity.getSecurityPolicy());
                vo.setPoolDiskUtilization(poolCapacity.getDiskUtilization());
            }

            dbf.getEntityManager().merge(vo);
        } catch (NoResultException|EmptyResultDataAccessException e) {
            return;
        }
    }

    @Override
    public boolean start() {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            asf.deployModule(CephGlobalProperty.BACKUP_STORAGE_MODULE_PATH, CephGlobalProperty.BACKUP_STORAGE_PLAYBOOK_NAME);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
