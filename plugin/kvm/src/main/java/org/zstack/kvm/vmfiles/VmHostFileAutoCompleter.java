package org.zstack.kvm.vmfiles;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.core.db.Q;
import org.zstack.header.vm.APIConvertTemplatedVmInstanceToVmInstanceMsg;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupMsg;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class VmHostFileAutoCompleter implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VmHostFileAutoCompleter.class);

    @Autowired
    private ResourceConfigFacade resourceConfigFacade;

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
                APIConvertTemplatedVmInstanceToVmInstanceMsg.class,
                APICreateVmInstanceFromVolumeSnapshotGroupMsg.class
        );
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIConvertTemplatedVmInstanceToVmInstanceMsg) {
            validate((APIConvertTemplatedVmInstanceToVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotGroupMsg) {
            validate((APICreateVmInstanceFromVolumeSnapshotGroupMsg) msg);
        }
        return msg;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    private void validate(APIConvertTemplatedVmInstanceToVmInstanceMsg msg) {
        if (msg.getResetTpm() != null) {
            return;
        }

        // template VM is also a VM  ->  templateUuid is vmUuid
        String templateUuid = msg.getTemplatedVmInstanceUuid();
        if (templateUuid == null) {
            return;
        }

        Boolean resolved = resourceConfigFacade.getResourceConfigValue(
                VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE, templateUuid, Boolean.class);
        msg.setResetTpm(resolved);
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotGroupMsg msg) {
        if (msg.getResetTpm() != null) {
            return;
        }

        String snapshotGroupUuid = msg.getVolumeSnapshotGroupUuid();
        if (snapshotGroupUuid == null) {
            return;
        }

        String vmUuid = Q.New(VolumeSnapshotGroupVO.class)
                .select(VolumeSnapshotGroupVO_.vmInstanceUuid)
                .eq(VolumeSnapshotGroupVO_.uuid, snapshotGroupUuid)
                .findValue();
        if (vmUuid == null) {
            return;
        }

        Boolean resolved = resourceConfigFacade.getResourceConfigValue(
                VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE, vmUuid, Boolean.class);
        msg.setResetTpm(resolved);
    }
}
