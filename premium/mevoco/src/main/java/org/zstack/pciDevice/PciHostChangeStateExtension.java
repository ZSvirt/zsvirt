package org.zstack.pciDevice;

import org.zstack.core.db.Q;
import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vdpa.VmVdpaNicVO;
import org.zstack.header.vdpa.VmVdpaNicVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.local.LocalStorageConstants;

import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * @author camile
 */
public class PciHostChangeStateExtension implements HostChangeStateExtensionPoint {

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

            Boolean onShareStorage = Q.New(PrimaryStorageVO.class)
                    .in(PrimaryStorageVO_.uuid, psUuids)
                    .notIn(PrimaryStorageVO_.type, Collections.singleton(LocalStorageConstants.LOCAL_STORAGE_TYPE))
                    .isExists();

            // except vDPA.
            List<String> vdpaPciUuids = Q.New(VmVdpaNicVO.class)
                    .select(VmVdpaNicVO_.pciDeviceUuid)
                    .in(VmVdpaNicVO_.vmInstanceUuid, vmUuids)
                    .listValues();

            Q query= Q.New(PciDeviceVO.class)
                    .select(PciDeviceVO_.vmInstanceUuid)
                    .in(PciDeviceVO_.vmInstanceUuid, vmUuids);
            if (!vdpaPciUuids.isEmpty()) {
                query = query.notIn(PciDeviceVO_.uuid, vdpaPciUuids);
            }
            List<String> hasPciVmUuids = query.listValues();

            if (!hasPciVmUuids.isEmpty() && onShareStorage) {
                throw new OperationFailureException(operr("The host [%s] has failed to enter the maintenance," +
                                " The vm [%s] cannot migrate automatically because it contains the PCI device",
                        inventory.getUuid(), hasPciVmUuids.toString()));
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