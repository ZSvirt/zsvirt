package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.core.db.Q;
import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.local.LocalStorageConstants;

import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 2019-04-17.
 */
public class MdevDeviceHostChangeStateExtension implements HostChangeStateExtensionPoint {
    @Override
    public void preChangeHostState(HostInventory inventory, HostStateEvent event, HostState nextState) throws HostException {
        if (event.equals(HostStateEvent.preMaintain) && nextState.equals(HostState.PreMaintenance)) {
            List<String> vmUuids = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.uuid)
                    .eq(VmInstanceVO_.hostUuid, inventory.getUuid())
                    .eq(VmInstanceVO_.type, ConfigurationConstant.USER_VM_INSTANCE_OFFERING_TYPE)
                    .listValues();
            if (vmUuids.isEmpty()) {
                return;
            }

            List<String> psUuids = Q.New(VolumeVO.class)
                    .select(VolumeVO_.primaryStorageUuid)
                    .in(VolumeVO_.vmInstanceUuid, vmUuids)
                    .listValues();
            if (psUuids.isEmpty()) {
                return;
            }

            List<String> hasMdevVmUuids = Q.New(MdevDeviceVO.class)
                    .select(MdevDeviceVO_.vmInstanceUuid)
                    .in(MdevDeviceVO_.vmInstanceUuid, vmUuids)
                    .listValues();

            boolean onShareStorage = Q.New(PrimaryStorageVO.class)
                    .in(PrimaryStorageVO_.uuid, psUuids)
                    .notIn(PrimaryStorageVO_.type, Collections.singleton(LocalStorageConstants.LOCAL_STORAGE_TYPE))
                    .isExists();

            if (!hasMdevVmUuids.isEmpty() && onShareStorage) {
                throw new OperationFailureException(operr("The host [%s] has failed to enter the maintenance," +
                                " because vm[%s] has mdev devices attached and cannot migrate automatically",
                        inventory.getUuid(), hasMdevVmUuids.toString()));
            }
        }
    }

    @Override
    public void beforeChangeHostState(HostInventory inventory, HostStateEvent event, HostState nextState) {
        // do nothing
    }

    @Override
    public void afterChangeHostState(HostInventory inventory, HostStateEvent event, HostState previousState) {
        // do nothing
    }
}