package org.zstack.storage.volume;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.expon.ExponConstants;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.APICreateVolumesSnapshotMsg;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;
import org.zstack.header.volume.block.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.mevoco.*;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephSystemTags;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.storage.primary.smp.SMPConstants;
import org.zstack.storage.volume.block.BlockVolumeFactory;
import org.zstack.storage.volume.block.BlockVolumeMessage;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.data.SizeUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.header.volume.MevocoVolumeConstants.*;
import static org.zstack.storage.volume.VolumeMevocoConstants.ALLOW_TAKE_SNAPSHOTS_VM_STATES;

/**
 * Created by mingjian.deng on 16/12/30.
 */
public class VolumeMevocoApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private CloudBus bus;
    @Autowired
    private MevocoManagerImpl mevocoManager;

    private static final List<String> ALLOW_BLOCK_VENDER_TYPES = asList(CephConstants.CEPH_PRIMARY_STORAGE_TYPE, ExponConstants.EXPON_MANUFACTURER);

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APISetVolumeQosMsg) {
            validate((APISetVolumeQosMsg) msg);
        } else if (msg instanceof APIResizeRootVolumeMsg) {
            validate((APIResizeRootVolumeMsg) msg);
        } else if (msg instanceof APIResizeDataVolumeMsg) {
            validate((APIResizeDataVolumeMsg) msg);
        } else if (msg instanceof APICreateVolumesSnapshotMsg) {
            validate((APICreateVolumesSnapshotMsg) msg);
        } else if (msg instanceof APIValidateVolumeSnapshotChainMsg) {
            validate((APIValidateVolumeSnapshotChainMsg) msg);
        } else if ((msg instanceof APISetVolumeIoThreadPinMsg)) {
            validate((APISetVolumeIoThreadPinMsg) msg);
        } else if (msg instanceof APIUpdateBlockVolumeMsg) {
            validate((APIUpdateBlockVolumeMsg) msg);
        } else if (msg instanceof APICreateBlockVolumeMsg) {
            validate((APICreateBlockVolumeMsg) msg);
        } else if (msg instanceof APIGetAccessPathMsg) {
            validate((APIGetAccessPathMsg) msg);
        }
        setServiceId(msg);
        return msg;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(
                APIGetVolumeQosMsg.class,
                APISetVolumeQosMsg.class,
                APIDeleteVolumeQosMsg.class,
                APIResizeRootVolumeMsg.class,
                APIResizeDataVolumeMsg.class,
                APICreateVolumesSnapshotMsg.class,
                APIValidateVolumeSnapshotChainMsg.class,
                APISetVolumeIoThreadPinMsg.class,
                APIGetVolumeIoThreadPinMsg.class,
                APICreateBlockVolumeMsg.class,
                APIGetAccessPathMsg.class,
                APIQueryBlockVolumeMsg.class,
                APIQueryXskyBlockVolumeMsg.class,
                APIUpdateXskyBlockVolumeMsg.class,
                APIUpdateBlockVolumeMsg.class,
                APIQueryExponBlockVolumeMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }

    private void validate(APIGetAccessPathMsg msg) {
        validateIsOrNotCeph(msg.getPrimaryStorageUuid());
        if (!CephSystemTags.THIRDPARTY_PLATFORM.hasTag(msg.getPrimaryStorageUuid())) {
            throw new ApiMessageInterceptionException(argerr("the current primaryStorage %s does not " +
                    "have a third-party token set, and the block volume cannot be " +
                    "created temporarily", msg.getPrimaryStorageUuid()));
        }
    }

    private static void validateIsOrNotCeph(String primaryStorageUuid) {
        String type = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, primaryStorageUuid).findValue();
        if (!type.equals(CephConstants.CEPH_PRIMARY_STORAGE_TYPE)) {
            throw new ApiMessageInterceptionException(argerr("the current primaryStorage %s is not " +
                    "Ceph type, can not get access path", primaryStorageUuid));
        }
    }

    private void validate(APICreateBlockVolumeMsg msg) {
        validateName(msg.getName());
        validateByVender(msg.getName(), msg.getProtocol(), msg.getPrimaryStorageUuid());
        String type = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid())
                .findValue();
        if (CephConstants.CEPH_PRIMARY_STORAGE_TYPE.equals(type)) {
            if (msg.getAccessPathId() == null || msg.getAccessPathIqn() == null) {
                throw new ApiMessageInterceptionException(argerr("Ceph type block volume accessPathId, " +
                        "accessPathIqn cannot be null"));
            }
        }
    }

    private void validateByVender(String name, String protocol, String primaryStorageUuid) {
        String vender = getManufactureByPrimaryStorage(primaryStorageUuid);
        if (vender == null) {
            throw new ApiMessageInterceptionException(argerr("current primary storage type not support block volume, support" +
                    "type has %s", ALLOW_BLOCK_VENDER_TYPES));
        }

        BlockVolumeFactory factory = mevocoManager.getBlockVolumeFactory(vender);
        if (factory == null) {
            throw new ApiMessageInterceptionException(argerr("no block volume factory found for vendor: %s", vender));
        }
        factory.validate(name, protocol, primaryStorageUuid);
    }

    private String getManufactureByPrimaryStorage(String primaryStorageUuid) {
        PrimaryStorageVO vo = dbf.findByUuid(primaryStorageUuid, PrimaryStorageVO.class);
        switch (vo.getType()) {
            case CephConstants.CEPH_PRIMARY_STORAGE_TYPE:
                return CephSystemTags.CEPH_MANUFACTURER.getTokenByResourceUuid(primaryStorageUuid,
                        CephSystemTags.CEPH_MANUFACTURER_TOKEN);
            case PrimaryStorageConstant.EXTERNAL_PRIMARY_STORAGE_TYPE:
                return ExponConstants.EXPON_MANUFACTURER;
        }

        return null;
    }

        private void validateName(String name) {
        if (!VolumeUtils.validateString(name)) {
            throw new ApiMessageInterceptionException(argerr("name[%s] is invalid, the name requirement: " +
                    "1~128 characters, support uppercase and lowercase letters, numbers, underscores, and hyphens; " +
                    "It can only start with uppercase and lowercase letters; " +
                    "It does not start or end with a space ", name));
        }
    }

    private void validate(APIUpdateBlockVolumeMsg msg) {
        if (StringUtils.isNotEmpty(msg.getName())) {
            validateName(msg.getName());
        }
        VolumeVO vo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getUuid()).find();
        validateByVender(msg.getName(), vo.getProtocol(), vo.getPrimaryStorageUuid());
    }

    private void validate(APISetVolumeIoThreadPinMsg msg) {
        VmInstanceVO vm = dbf.findByUuid(msg.getVmUuid(), VmInstanceVO.class);
        String hostUuid = vm.getHostUuid();
        if (hostUuid == null) {
            hostUuid = Q.New(LocalStorageResourceRefVO.class)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, vm.getRootVolume().getUuid())
                    .select(LocalStorageResourceRefVO_.hostUuid)
                    .findValue();
        }
        String finalHostUuid = hostUuid;
        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);

        String qemuVersion = KVMSystemTags.QEMU_IMG_VERSION.getTokenByResourceUuid(finalHostUuid, KVMSystemTags.QEMU_IMG_VERSION_TOKEN);
        if (qemuVersion != null && !qemuVersion.isEmpty()) {
            VersionComparator vc = new VersionComparator(qemuVersion);
            if (vc.compare(IOTHREAD_QEMU_VERSION) < 0) {
                throw new ApiMessageInterceptionException(argerr("iothread need qemu version >= %s, but %s on host[%s].", IOTHREAD_QEMU_VERSION, qemuVersion, finalHostUuid));
            }
        }

        String libvirtVersion = KVMSystemTags.LIBVIRT_VERSION.getTokenByResourceUuid(finalHostUuid, KVMSystemTags.LIBVIRT_VERSION_TOKEN);
        if (libvirtVersion != null && !libvirtVersion.isEmpty()) {
            VersionComparator vc = new VersionComparator(libvirtVersion);
            if (vc.compare(IOTHREAD_LIBVIRT_VERSION) < 0) {
                throw new ApiMessageInterceptionException(argerr("iothread need libvirt version >= %s, but %s on host[%s].", IOTHREAD_LIBVIRT_VERSION, libvirtVersion, finalHostUuid));
            }
        }

        if (volume.getType() == VolumeType.Root) {
            throw new ApiMessageInterceptionException(argerr("root volume[%s] cannot set iothreadpin.", msg.getVolumeUuid()));
        }

        if (volume.isAttached() && MevocoVolumeSystemTags.IO_THREAD_PIN.hasTag(msg.getVolumeUuid())) {
            String pinStr = MevocoVolumeSystemTags.IO_THREAD_PIN.getTokenByResourceUuid(msg.getVolumeUuid(), MevocoVolumeSystemTags.IO_THREAD_PIN_TOKEN);
            String[] pinInfo = pinStr.split(IOTHREADPIN_SEPARATOR);
            if (!String.valueOf(msg.getIoThreadId()).equals(pinInfo[0])) {
                throw new ApiMessageInterceptionException(argerr("current iothread id[%s] is not the same as attached vol[%s] iothread[%s].", msg.getIoThreadId(), msg.getVolumeUuid(), pinInfo[0]));
            }
        }
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VolumeMessage) {
            VolumeMessage volumeMessage = (VolumeMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, MevocoConstants.SERVICE_ID, volumeMessage.getVolumeUuid());
        }
    }

    private void validate(APIValidateVolumeSnapshotChainMsg msg) {
        VolumeVO volume = dbf.findByUuid(msg.getUuid(), VolumeVO.class);

        if (volume.getVmInstanceUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("snapshot validation is unsupported for volume[uuid: %s]." +
                    " Volume should be attached to vm", msg.getUuid()));
        }

        if (!Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, volume.getVmInstanceUuid())
                .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused))
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr("snapshot validation is unsupported for volume[uuid: %s]." +
                    " Attached vm is not in state of [%s, %s]", msg.getUuid(), VmInstanceState.Running, VmInstanceState.Paused));
        }
    }

    private void validate(APICreateVolumesSnapshotMsg msg) {
        List<VolumeVO> volumeVOS = new ArrayList<>();
        for (String volumeUuid : msg.getVolumeUuids()) {
            VolumeVO volumeVO = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).find();
            if (volumeVO == null) {
                throw new ApiMessageInterceptionException(argerr(
                        "volume[uuid:%s] can not found", volumeUuid
                ));
            }

            volumeVOS.add(volumeVO);
            if (!volumeVO.getVmInstanceUuid().equals(volumeVOS.get(0).getVmInstanceUuid())) {
                throw new ApiMessageInterceptionException(argerr(
                        "not support take snapshots volume[uuid:%s, uuid:%s] on different vms[uuid:%s, uuid:%s]",
                        volumeUuid, volumeVOS.get(0).getUuid(), volumeVO.getVmInstanceUuid(), volumeVOS.get(0).getVmInstanceUuid()
                ));
            }

            if (!volumeVO.getStatus().equals(VolumeStatus.Ready)) {
                throw new ApiMessageInterceptionException(argerr(
                        "volume[uuid:%s] is not ready", volumeUuid
                ));
            }
        }

        if (volumeVOS.get(0).getVmInstanceUuid() == null) {
            return;
        }

        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, volumeVOS.get(0).getVmInstanceUuid())
                .find();

        if (!ALLOW_TAKE_SNAPSHOTS_VM_STATES.contains(vmInstanceVO.getState())) {
            throw new ApiMessageInterceptionException(argerr(
                    "state of vm[uuid: %s] is %s, not allowed to take snapshots",
                    vmInstanceVO.getUuid(), vmInstanceVO.getState()
            ));
        }
    }

    private void validate(APIResizeDataVolumeMsg msg) {
        VolumeVO vo = dbf.findByUuid(msg.getUuid(), VolumeVO.class);
        if (!vo.getType().toString().equals(VolumeType.Data.toString())) {
            throw new ApiMessageInterceptionException(argerr(
                    "volume[uuid:%s] is not data volume", msg.getUuid()
            ));
        }

        if (!vo.getStatus().equals(VolumeStatus.NotInstantiated)){
            validateResizeVolume(msg.getSize(), vo);
        }

        if (vo.getState().equals(VolumeState.Disabled)) {
            throw new ApiMessageInterceptionException(argerr("can not resize volume[%s], " +
                    "because volume state is Disabled", msg.getVolumeUuid()));
        }

        msg.setSize(getRoundedSizeInMB(msg.getSize()));
    }

    private long getRoundedSizeInMB(long size) {
        long r = SizeUnit.MEGABYTE.toByte(4);
        return ((size + r - 1) / r) * r;
    }

    private void validate(APIResizeRootVolumeMsg msg) {
        if (msg.getVmInstanceUuid() == null && msg.getUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("At least one of vmInstanceUuid or uuid should be set"));
        }

        Q q = Q.New(VolumeVO.class);

        if (msg.getUuid() != null) {
            q.eq(VolumeVO_.uuid, msg.getUuid());
        }

        if (msg.getVmInstanceUuid() != null) {
            q.eq(VolumeVO_.vmInstanceUuid, msg.getVmInstanceUuid());
        }

        VolumeVO vo = q.find();
        if (vo == null) {
            throw new ApiMessageInterceptionException(argerr("no volume[uuid:%s, vmInstanceUuid:%s] can be found", msg.getUuid(), msg.getVmInstanceUuid()));
        }

        if (!vo.getType().toString().equals(VolumeType.Root.toString())) {
            throw new ApiMessageInterceptionException(argerr(
                    "volume[uuid:%s] is not root volume", msg.getUuid()
            ));
        }
        validateResizeVolume(msg.getSize(), vo);

        msg.setSize(getRoundedSizeInMB(msg.getSize()));
        msg.setVmInstanceUuid(vo.getVmInstanceUuid());
        msg.setUuid(vo.getUuid());
    }

    private void validate(APISetVolumeQosMsg msg) {
        VolumeVO vvo = dbf.findByUuid(msg.getUuid(), VolumeVO.class);
        ExponBlockVolumeVO blockVolumeVO = dbf.findByUuid(msg.getUuid(), ExponBlockVolumeVO.class);
        if (vvo.isShareable() && blockVolumeVO == null) {
            throw new ApiMessageInterceptionException(argerr("SharedVolume cannot be set bandwidth."));
        }

        if (msg.getReadBandwidth() != null || msg.getWriteBandwidth() != null
                || msg.getTotalBandwidth() != null || msg.getReadIOPS() != null
                || msg.getWriteIOPS() != null || msg.getTotalIOPS() != null) {

            if (msg.getMode() != null || msg.getVolumeBandwidth() != null) {
                throw new ApiMessageInterceptionException(
                        argerr("Cannot set legacy params and new params at the same time."));
            }

            if (msg.getTotalIOPS() != null && (msg.getReadIOPS() != null || msg.getWriteIOPS() != null)) {
                throw new ApiMessageInterceptionException(
                        argerr("Cannot set the read/write and the total IOPS limits at the same time."));
            }

            if (msg.getTotalBandwidth() != null && (msg.getReadBandwidth() != null || msg.getWriteBandwidth() != null)) {
                throw new ApiMessageInterceptionException(
                        argerr("Cannot set the read/write and the total bandwidth limits at the same time."));
            }
            msg.setVersion(2);
            return;
        }

        msg.setVersion(1);
        if (msg.getVolumeBandwidth() == null) {
            throw new ApiMessageInterceptionException(
                    argerr("The volume bandwidth cannot be null, must give a volume bandwidth value.")
            );
        }

        if (msg.getMode() == null) {
            msg.setMode(MevocoVolumeConstants.VOLUME_QOS_MODE_TOTAL);
        }
    }

    private void validateResizeVolume(long size, VolumeVO vo){
        if (size < vo.getSize()) {
            throw new ApiMessageInterceptionException(argerr(
                    "Cannot shrink [%s] volume[uuid:%s]'s size", vo.getType(), vo.getUuid()
            ));
        }

        long increaseSize = size - vo.getSize();
        if (increaseSize < SizeUnit.MEGABYTE.toByte(4)) {
            throw new ApiMessageInterceptionException(argerr(
                    "Minimum increase size should be larger than 4MB"
            ));
        }

        PrimaryStorageVO psVo = dbf.findByUuid(vo.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        String type = psVo.getType();
        if (StringUtils.equals(type, NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)) {
            String ip = StringUtils.split(psVo.getUrl(), ":/")[0];
            HostVO hostVo= Q.New(HostVO.class).eq(HostVO_.managementIp,ip).find();
            if (hostVo != null && HostState.Disabled.equals(hostVo.getState())){
                throw new OperationFailureException(argerr(
                        "Expansion operation not allowed at host disable"));
            }
        }else if (StringUtils.equals(type, SMPConstants.SMP_TYPE) || StringUtils.equals(type, CephConstants.CEPH_PRIMARY_STORAGE_TYPE)){
            List<String> clusterUuids = Q.New(PrimaryStorageClusterRefVO.class)
                    .select(PrimaryStorageClusterRefVO_.clusterUuid)
                    .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, psVo.getUuid())
                    .listValues();
            List<HostState> states = Q.New(HostVO.class)
                    .select(HostVO_.state)
                    .in(HostVO_.clusterUuid, clusterUuids)
                    .listValues();
            if (!states.contains(HostState.Enabled)) {
                throw new OperationFailureException(argerr(
                        "Expansion operation not allowed at all host disable"));
            }
        }

        if (!vo.isShareable()) {
            return;
        }

        List<ShareableVolumeVmInstanceRefVO> refVOS = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .eq(ShareableVolumeVmInstanceRefVO_.volumeUuid, vo.getUuid()).list();

        if (refVOS == null || refVOS.isEmpty()) {
            return;
        }

        List<String> vmInstanceUuids = refVOS.stream().map(ShareableVolumeVmInstanceRefVO::getVmInstanceUuid).collect(Collectors.toList());
        List<String> notStoppedVmUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .notEq(VmInstanceVO_.state, VmInstanceState.Stopped)
                .in(VmInstanceVO_.uuid, vmInstanceUuids).listValues();

        if (notStoppedVmUuids == null || notStoppedVmUuids.isEmpty()) {
            return;
        }

        throw new ApiMessageInterceptionException(argerr(
                "shared volume[uuid: %s] has attached to not stopped vm instances[uuids: %s]",
                vo.getUuid(), notStoppedVmUuids
        ));
    }
}
