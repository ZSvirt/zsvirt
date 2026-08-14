package org.zstack.storage.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmMigrationMetric;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.snapshot.group.MemorySnapshotValidatorExtensionPoint;
import org.zstack.header.storageDevice.ScsiLunVO;
import org.zstack.header.storageDevice.ScsiLunVO_;
import org.zstack.header.storageDevice.ScsiLunVmInstanceRefVO;
import org.zstack.header.storageDevice.ScsiLunVmInstanceRefVO_;
import org.zstack.header.vm.APIMigrateVmMsg;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.storage.device.iscsi.*;
import org.zstack.storage.device.nvme.*;
import org.zstack.storage.primary.sharedblock.APIAddSharedBlockGroupPrimaryStorageMsg;
import org.zstack.storage.primary.sharedblock.APIAddSharedBlockToSharedBlockGroupMsg;
import org.zstack.storage.primary.sharedblock.APIUpdateSharedBlockMsg;
import org.zstack.header.storageDevice.*;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Create by weiwang at 2018/8/3
 */
@InterceptorForService("storageDevice")
public class StorageDeviceApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor, MemorySnapshotValidatorExtensionPoint {
    private static final CLogger logger = Utils.getLogger(StorageDeviceApiInterceptor.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddIscsiServerMsg) {
            validate((APIAddIscsiServerMsg) msg);
        } else if (msg instanceof APIAttachIscsiServerToClusterMsg) {
            validate((APIAttachIscsiServerToClusterMsg) msg);
        } else if (msg instanceof APIDetachIscsiServerFromClusterMsg) {
            validate((APIDetachIscsiServerFromClusterMsg) msg);
        } else if (msg instanceof APIDeleteIscsiServerMsg) {
            validate((APIDeleteIscsiServerMsg) msg);
        } else if (msg instanceof APIAddSharedBlockGroupPrimaryStorageMsg) {
            validate((APIAddSharedBlockGroupPrimaryStorageMsg) msg);
        } else if (msg instanceof APIAddSharedBlockToSharedBlockGroupMsg) {
            validate((APIAddSharedBlockToSharedBlockGroupMsg) msg);
        } else if (msg instanceof APIUpdateSharedBlockMsg) {
            validate((APIUpdateSharedBlockMsg) msg);
        } else if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        } else if (msg instanceof APIAttachNvmeServerToClusterMsg) {
            validate((APIAttachNvmeServerToClusterMsg) msg);
        } else if (msg instanceof APIAddNvmeServerMsg) {
            validate((APIAddNvmeServerMsg) msg);
        } else if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        }

        return msg;
    }

    private void validate(APIMigrateVmMsg msg) {
        VmInstanceVO vmvo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        String rootVolumePsType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, vmvo.getRootVolume().getPrimaryStorageUuid())
                .select(PrimaryStorageVO_.type).findValue();

        for (VmMigrationMetric vmMigrationMetric : pluginRgty.getExtensionList(VmMigrationMetric.class)) {
            if (!vmMigrationMetric.isCapable(rootVolumePsType)) {
                continue;
            }
            if (!vmMigrationMetric.isSupportWithSharedBlock() && Q.New(ScsiLunVmInstanceRefVO.class)
                    .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .isExists()) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "do not support migration of vm[uuid:%s] with shared block", msg.getVmInstanceUuid()
                ));
            }
        }
    }

    private void validate(APIAddNvmeServerMsg msg) {
        if (Q.New(NvmeServerVO.class)
                .eq(NvmeServerVO_.ip, msg.getIp())
                .eq(NvmeServerVO_.port, msg.getPort())
                .eq(NvmeServerVO_.transport, msg.getTransport())
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr(
                    "NVMe server[ip: %s, port: %s, transport: %s] already exists", msg.getIp(), msg.getPort(), msg.getTransport()));
        }

        if (!NetworkUtils.isIpv4Address(msg.getIp())) {
            throw new ApiMessageInterceptionException(argerr("NVMe server ip: %s is not valid", msg.getIp()));
        }
    }

    private void validate(APIAttachNvmeServerToClusterMsg msg) {
        if (Q.New(NvmeServerClusterRefVO.class)
                .eq(NvmeServerClusterRefVO_.nvmeServerUuid, msg.getUuid())
                .eq(NvmeServerClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr(
                    "NVMe server[uuid: %s] already attached to cluster[uuid: %s]", msg.getUuid(), msg.getClusterUuid()));
        }
    }

    private void validate(APIUpdateSharedBlockMsg msg) {
        if (msg.getDiskUuid() == null) {
            return;
        }

        List<ScsiLunVO> scsiLunVOS = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.wwid, msg.getDiskUuid()).list();
        if (scsiLunVOS == null || scsiLunVOS.isEmpty()) {
            return;
        }

        for (ScsiLunVO scsiLunVO : scsiLunVOS) {
            ScsiLunVmInstanceRefVO refVO = Q.New(ScsiLunVmInstanceRefVO.class).eq(ScsiLunVmInstanceRefVO_.scsiLunUuid, scsiLunVO.getUuid()).limit(1).find();
            if (refVO != null) {
                throw new ApiMessageInterceptionException(argerr(
                        "scsi lun[wwid: %s] has been attached to vm instance %s", scsiLunVO.getWwid(), refVO.getVmInstanceUuid()));
            }
        }
    }

    public void validate(APIAddIscsiServerMsg msg) {
        if (Q.New(IscsiServerVO.class)
                .eq(IscsiServerVO_.ip, msg.getIp())
                .eq(IscsiServerVO_.port, msg.getPort())
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr(
                    "iSCSI server[ip: %s, port: %s] already exists", msg.getIp(), msg.getPort()));
        }

        if (!NetworkUtils.isIpv4Address(msg.getIp())) {
            throw new ApiMessageInterceptionException(argerr("iSCSI server ip: %s is not valid", msg.getIp()));
        }
    }

    public void validate(APIAttachIscsiServerToClusterMsg msg) {
        if (Q.New(IscsiServerClusterRefVO.class)
                .eq(IscsiServerClusterRefVO_.iscsiServerUuid, msg.getUuid())
                .eq(IscsiServerClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr(
                    "iSCSI server[uuid: %s] already attached to cluster[uuid: %s]", msg.getUuid(), msg.getClusterUuid()));
        }
    }

    public void validate(APIDetachIscsiServerFromClusterMsg msg) {
        if (!Q.New(IscsiServerClusterRefVO.class)
                .eq(IscsiServerClusterRefVO_.iscsiServerUuid, msg.getUuid())
                .eq(IscsiServerClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr(
                    "iSCSI server[uuid: %s] not attached to cluster[uuid: %s]", msg.getUuid(), msg.getClusterUuid()));
        }
    }

    public void validate(APIDeleteIscsiServerMsg msg) {
        String clusterUuid = Q.New(IscsiServerClusterRefVO.class)
                .select(IscsiServerClusterRefVO_.clusterUuid)
                .eq(IscsiServerClusterRefVO_.iscsiServerUuid, msg.getUuid())
                .limit(1).findValue();
        if (clusterUuid != null) {
            throw new ApiMessageInterceptionException(argerr(
                    "iSCSI server[uuid: %s] still attached to cluster[uuid: %s]", msg.getUuid(), clusterUuid));
        }
    }

    public void validate(APIAddSharedBlockGroupPrimaryStorageMsg msg) {
        List<ScsiLunVO> scsiLunVOS = Q.New(ScsiLunVO.class).in(ScsiLunVO_.wwid, msg.getDiskUuids()).list();
        if (scsiLunVOS == null || scsiLunVOS.isEmpty()) {
            return;
        }

        for (ScsiLunVO scsiLunVO : scsiLunVOS) {
            ScsiLunVmInstanceRefVO refVO = Q.New(ScsiLunVmInstanceRefVO.class).eq(ScsiLunVmInstanceRefVO_.scsiLunUuid, scsiLunVO.getUuid()).limit(1).find();
            if (refVO != null) {
                throw new ApiMessageInterceptionException(argerr(
                        "scsi lun[wwid: %s] has been attached to vm instance %s", scsiLunVO.getWwid(), refVO.getVmInstanceUuid()));
            }
        }
    }

    public void validate(APIAddSharedBlockToSharedBlockGroupMsg msg) {
        List<ScsiLunVO> scsiLunVOS = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.wwid, msg.getDiskUuid()).list();
        if (scsiLunVOS == null || scsiLunVOS.isEmpty()) {
            return;
        }

        for (ScsiLunVO scsiLunVO : scsiLunVOS) {
            ScsiLunVmInstanceRefVO refVO = Q.New(ScsiLunVmInstanceRefVO.class).eq(ScsiLunVmInstanceRefVO_.scsiLunUuid, scsiLunVO.getUuid()).limit(1).find();
            if (refVO != null) {
                throw new ApiMessageInterceptionException(argerr(
                        "scsi lun[wwid: %s] has been attached to vm instance %s", scsiLunVO.getWwid(), refVO.getVmInstanceUuid()));
            }
        }
    }

    public void validate(APICreateVmInstanceMsg msg) {
        if (CollectionUtils.isEmpty(msg.getDiskAOs())) {
            return;
        }

        if (msg.getHostUuid() == null) {
            return;
        }

        List<String> lunUuids = msg.getDiskAOs().stream()
                .filter(diskAO -> Objects.equals(diskAO.getSourceType(), LunVO.class.getSimpleName()))
                .map(DiskAO::getSourceUuid).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(lunUuids)) {
            return;
        }

        List<String> attachedHostLunUuids = Q.New(ScsiLunHostRefVO.class).eq(ScsiLunHostRefVO_.hostUuid, msg.getHostUuid())
                .in(ScsiLunHostRefVO_.scsiLunUuid, lunUuids).select(ScsiLunHostRefVO_.scsiLunUuid).listValues();

        lunUuids.removeAll(attachedHostLunUuids);
        if (!lunUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("scisLun[uuids:%s] are not attach to the cluster of host[uuid:%s]",
                    lunUuids.toString(), msg.getHostUuid()));
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APIAddSharedBlockGroupPrimaryStorageMsg.class,
                APIAddSharedBlockToSharedBlockGroupMsg.class,
                APIMigrateVmMsg.class,
                APICreateVmInstanceMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public ErrorCode checkVmWhereMemorySnapshotExistExternalDevices(String VmInstanceUuid) {
        if (Q.New(ScsiLunVmInstanceRefVO.class)
                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, VmInstanceUuid).isExists()) {
            return argerr("please umount all block devices of the vm[%s] and try again", VmInstanceUuid);
        }

        return null;
    }
}
