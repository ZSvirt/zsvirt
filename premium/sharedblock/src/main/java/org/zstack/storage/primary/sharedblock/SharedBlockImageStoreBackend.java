package org.zstack.storage.primary.sharedblock;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.ResizeVolumeOnPrimaryStorageMsg;
import org.zstack.storage.backup.imagestore.CleanImageMetaOnPrimaryStorageMsg;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

public class SharedBlockImageStoreBackend extends SharedBlockGroupPrimaryStorageBase {
    public SharedBlockImageStoreBackend(PrimaryStorageVO self) {
        super(self);
    }

    public SharedBlockImageStoreBackend() {
    }

    @Override
    public void handleLocalMessage(Message msg) {
        if (msg instanceof CommitVolumeAsImageMsg) {
            handle((CommitVolumeAsImageMsg) msg);
        } else if (msg instanceof SelectBackupStorageMsg) {
            handle((SelectBackupStorageMsg) msg);
        } else if (msg instanceof CommitVolumeAsImageOnPrimaryStorageMsg) {
            handle((CommitVolumeAsImageOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ResizeVolumeOnPrimaryStorageMsg) {
            handle((ResizeVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CleanImageMetaOnPrimaryStorageMsg) {
            handle((CleanImageMetaOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(final ResizeVolumeOnPrimaryStorageMsg msg) {
        String clusterUuid = null;
        if (null != msg.getVolume().getVmInstanceUuid()) {
            clusterUuid = getClusterUuidByVolumeUuid(msg.getVolume().getUuid());
        }

        if (clusterUuid == null) {
            clusterUuid = getClusterUuidByVolumePrimaryStorageUuid(msg.getVolume().getPrimaryStorageUuid());
        }

        if (clusterUuid == null) {
            throw new OperationFailureException(Platform.operr("can not get cluster uuid of volume %s", msg.getVolume().getUuid()));
        }

        SharedBlockHypervisorFactory factory = getHypervisorFactoryByClusterUuid(clusterUuid);
        SharedBlockHypervisorBackend bkd = factory.getHypervisorBackend(self);
        bkd.handleLocalMessage(msg);
    }

    private void handle(final CleanImageMetaOnPrimaryStorageMsg msg) {
        SharedBlockHypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolumeUuid());
        bkd.handleLocalMessage(msg);
    }

    @Transactional(readOnly = true)
    private String getHostUuidByVolumeUuid(String volumeUuid) {
        String sql = "select vm.hostUuid, vm.lastHostUuid from VmInstanceVO vm, VolumeVO vol "
                + "where vol.uuid = :volumeUuid and vol.vmInstanceUuid = vm.uuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("volumeUuid", volumeUuid);
        Tuple t = q.getSingleResult();

        String hostUuid = t.get(0, String.class);
        return hostUuid != null ? hostUuid : t.get(1, String.class);
    }

    @Transactional(readOnly = true)
    private String getClusterUuidByVolumeUuid(String volumeUuid) {
        String clusterUuid = SQL.New("select vm.clusterUuid from VmInstanceVO vm, VolumeVO vol"
                + " where vol.uuid = :volumeUuid and vol.vmInstanceUuid = vm.uuid", String.class)
                .param("volumeUuid", volumeUuid)
                .find();

        if (clusterUuid == null) {
            clusterUuid = getClusterUuidByLastHost(volumeUuid);
        }

        if (clusterUuid == null) {
            throw new OperationFailureException(Platform.operr("can not get cluster uuid of volume %s", volumeUuid));
        }
        return clusterUuid;
    }

    private String getClusterUuidByVolumePrimaryStorageUuid(String psUuid) {
        return SQL.New("select ref.clusterUuid from PrimaryStorageClusterRefVO ref,PrimaryStorageVO ps"
                + " where ref.primaryStorageUuid = :psUuid and ps.status = :state", String.class)
                .param("psUuid", psUuid)
                .param("state", PrimaryStorageStatus.Connected)
                .limit(1)
                .find();
    }

    private void handle(final CommitVolumeAsImageOnPrimaryStorageMsg msg) {
        String clusterUuid = getClusterUuidByVolumePrimaryStorageUuid(msg.getPrimaryStorageUuid());
        if (clusterUuid == null) {
            clusterUuid = getClusterUuidByVolumeUuid(msg.getVolumeUuid());
        }
        SharedBlockHypervisorFactory factory = getHypervisorFactoryByClusterUuid(clusterUuid);
        SharedBlockHypervisorBackend bkd = factory.getHypervisorBackend(self);
        bkd.handleLocalMessage(msg);
    }

    private void handle(final CommitVolumeAsImageMsg msg) {
        String clusterUuid = getClusterUuidByVolumeUuid(msg.getVolumeUuid());
        SharedBlockHypervisorFactory factory = getHypervisorFactoryByClusterUuid(clusterUuid);
        SharedBlockHypervisorBackend bkd = factory.getHypervisorBackend(self);
        bkd.handleLocalMessage(msg);
    }

    private void handle(final SelectBackupStorageMsg msg) {
        String clusterUuid = getClusterUuidByVolumeUuid(msg.getVolumeUuid());
        SharedBlockHypervisorFactory factory = getHypervisorFactoryByClusterUuid(clusterUuid);
        SharedBlockHypervisorBackend bkd = factory.getHypervisorBackend(self);
        bkd.handleLocalMessage(msg);
    }

    private String getClusterUuidByLastHost(String volumeUuid) {
        String lastHostUuid = SQL.New("select vm.lastHostUuid from VmInstanceVO vm, VolumeVO vol"
                + " where vol.uuid = :volumeUuid and vol.vmInstanceUuid = vm.uuid", String.class)
                .param("volumeUuid", volumeUuid)
                .find();
        if (lastHostUuid != null) {
            return dbf.findByUuid(lastHostUuid, HostVO.class).getClusterUuid();
        }
        return null;
    }
}
