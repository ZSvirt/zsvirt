package org.zstack.storage.memorySnapshot;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataGroupVO;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataGroupVO_;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.header.volume.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * @author hanyu.liang
 */
public class MemorySnapshotGroupValidator implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(MemorySnapshotGroupValidator.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachDataVolumeToVmMsg) {
            validate((APIAttachDataVolumeToVmMsg) msg);
        } else if (msg instanceof APIDetachDataVolumeFromVmMsg) {
            validate((APIDetachDataVolumeFromVmMsg) msg);
        } else if (msg instanceof APIDeleteDataVolumeMsg) {
            validate((APIDeleteDataVolumeMsg) msg);
        }
        return msg;
    }

    private void validate(APIDeleteDataVolumeMsg msg) {
        String volumeVmUuid = Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid).eq(VolumeVO_.uuid, msg.getUuid()).findValue();
        if (StringUtils.isEmpty(volumeVmUuid)) {
            return;
        }
        if (isVmVolumeReferencedByMemorySnapshot(msg.getVolumeUuid(), volumeVmUuid)) {
            throw new ApiMessageInterceptionException(operr("the vm where the data volume [%s] is located has a memory snapshot, can't delete",
                    msg.getVolumeUuid()));
        }
    }

    private void validate(APIAttachDataVolumeToVmMsg msg) {
        if (skipAttachVolumeMemorySnapshotGroupValidate(msg.getVolumeUuid(), msg.getVmUuid())) {
            // if volume not referenced by memory snapshot group, delete the volume's device address
            vidm.deleteVmResourceMetadata(msg.getVolumeUuid());
            return;
        }

        // if volume not referenced by memory snapshot group, delete the volume's device address
        vidm.deleteVmResourceMetadata(msg.getVolumeUuid());
    }

    private void validate(APIDetachDataVolumeFromVmMsg msg) {
    }

    public boolean isVmVolumeReferencedByMemorySnapshot(String volumeUuid, String vmUuid) {
        String volumeVmUuid = StringUtils.isEmpty(vmUuid) ? Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid).eq(VolumeVO_.uuid, volumeUuid).findValue() : vmUuid;
        List<String> vmVolumeUuids = Q.New(VolumeVO.class).select(VolumeVO_.uuid)
                .eq(VolumeVO_.vmInstanceUuid, volumeVmUuid)
                .eq(VolumeVO_.status, VolumeStatus.Ready)
                .notEq(VolumeVO_.type, VolumeType.Memory).listValues();
        if (vmVolumeUuids.isEmpty()) {
            return false;
        }

        List<String> volumeSnapshotGroupUuids = Q.New(VolumeSnapshotGroupRefVO.class)
                .select(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid)
                .in(VolumeSnapshotGroupRefVO_.volumeUuid, vmVolumeUuids)
                .listValues();

        if (volumeSnapshotGroupUuids.isEmpty()) {
            return false;
        }

        return Q.New(VmInstanceResourceMetadataGroupVO.class)
                .eq(VmInstanceResourceMetadataGroupVO_.vmInstanceUuid, volumeVmUuid)
                .in(VmInstanceResourceMetadataGroupVO_.resourceUuid, volumeSnapshotGroupUuids)
                .isExists();
    }

    public boolean skipAttachVolumeMemorySnapshotGroupValidate(String volumeUuid, String vmUuid) {
        List<String> groupsForVm = Q.New(VolumeSnapshotGroupVO.class)
                .eq(VolumeSnapshotGroupVO_.vmInstanceUuid, vmUuid)
                .select(VolumeSnapshotGroupVO_.uuid)
                .listValues();

        if (groupsForVm.isEmpty()) {
            return true;
        }

        groupsForVm = groupsForVm.stream()
                .filter(groupUuid -> Q.New(VmInstanceResourceMetadataGroupVO.class)
                        .eq(VmInstanceResourceMetadataGroupVO_.resourceUuid, groupUuid)
                        .isExists())
                .collect(Collectors.toList());

        if (groupsForVm.isEmpty()) {
            return true;
        }

        return Q.New(VolumeSnapshotGroupRefVO.class)
                .in(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, groupsForVm)
                .eq(VolumeSnapshotGroupRefVO_.volumeUuid, volumeUuid)
                .isExists();
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
                APIAttachDataVolumeToVmMsg.class,
                APIDetachDataVolumeFromVmMsg.class,
                APIDeleteDataVolumeMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
