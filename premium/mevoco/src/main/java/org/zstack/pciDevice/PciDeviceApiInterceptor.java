package org.zstack.pciDevice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.sriov.EthernetVfStatus;
import org.zstack.header.storage.snapshot.group.MemorySnapshotValidatorExtensionPoint;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.pciDevice.specification.mdev.PciDeviceMdevSpecRefVO;
import org.zstack.pciDevice.specification.mdev.PciDeviceMdevSpecRefVO_;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecVO;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecVO_;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.pciDevice.virtual.sr_iov.APIGenerateSriovPciDevicesMsg;
import org.zstack.pciDevice.virtual.sr_iov.APIUngenerateSriovPciDevicesMsg;
import org.zstack.pciDevice.virtual.vfio_mdev.*;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

/**
 * Created by weiwang on 17/06/2017.
 */
@InterceptorForService("pciDevice")
public class PciDeviceApiInterceptor implements ApiMessageInterceptor, MemorySnapshotValidatorExtensionPoint {
    private static final CLogger logger = Utils.getLogger(PciDeviceApiInterceptor.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ResourceConfigFacade rcf;

    public static List<PciDeviceStatus> allowedPciDeviceAttachableStatus = PciDeviceStatus.attachablePciDeviceStatus;
    public static List<VmInstanceState> allowedVmInstanceAttachableStatus = asList(VmInstanceState.Running, VmInstanceState.Stopped);

    public static List<PciDeviceStatus> allowedPciDeviceDetachableStatus = asList(PciDeviceStatus.Attached);
    public static List<VmInstanceState> allowedVmInstanceDetachableStatus = asList(VmInstanceState.Running, VmInstanceState.Stopped);
    public static List<VmInstanceState> allowedVmInstanceNonHotPlugStatus = asList(VmInstanceState.Stopped);

    private Boolean allowedPciDeviceHotPlugUnplug(VmInstanceVO vmvo) {
        boolean allowed = true;
        boolean enableHotPlug = rcf.getResourceConfigValue(PciDeviceGlobalConfig.ENABLE_HOT_PLUG, vmvo.getUuid(), Boolean.class);
        if (!enableHotPlug && !allowedVmInstanceNonHotPlugStatus.contains(vmvo.getState())) {
            allowed = false;
        }
        return allowed;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachPciDeviceToVmMsg) {
            validate((APIAttachPciDeviceToVmMsg) msg);
        } else if (msg instanceof APIDetachPciDeviceFromVmMsg) {
            validate((APIDetachPciDeviceFromVmMsg) msg);
        } else if (msg instanceof APICreatePciDeviceOfferingMsg) {
            validate((APICreatePciDeviceOfferingMsg) msg);
        } else if (msg instanceof APIDeletePciDeviceMsg) {
            validate((APIDeletePciDeviceMsg) msg);
        } else if (msg instanceof APIUpdatePciDeviceMsg) {
            validate((APIUpdatePciDeviceMsg) msg);
        } else if (msg instanceof APIUpdateHostIommuStateMsg) {
            validate((APIUpdateHostIommuStateMsg) msg);
        } else if (msg instanceof APIGetPciDeviceCandidatesForAttachingVmMsg) {
            validate((APIGetPciDeviceCandidatesForAttachingVmMsg) msg);
        } else if (msg instanceof APIGetPciDeviceCandidatesForNewCreateVmMsg) {
            validate((APIGetPciDeviceCandidatesForNewCreateVmMsg) msg);
        } else if (msg instanceof APIGenerateSriovPciDevicesMsg) {
            validate((APIGenerateSriovPciDevicesMsg) msg);
        } else if (msg instanceof APIUngenerateSriovPciDevicesMsg) {
            validate((APIUngenerateSriovPciDevicesMsg) msg);
        } else if (msg instanceof APIGenerateMdevDevicesMsg) {
            validate((APIGenerateMdevDevicesMsg) msg);
        } else if (msg instanceof APIUngenerateMdevDevicesMsg) {
            validate((APIUngenerateMdevDevicesMsg) msg);
        }
        return msg;
    }


    private void validate(APIGetPciDeviceCandidatesForNewCreateVmMsg msg) {
        if ((msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) && msg.getHostUuid() != null) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    "cluster uuids or host uuid can not be set at same time"
            ));
        }

        HostIommuGetter hostIommuGetter = new HostIommuGetter();
        if (msg.getHostUuid() != null && (
                HostIommuStatusType.Inactive.equals(
                    hostIommuGetter.getStatus(msg.getHostUuid()))
                    || HostIommuStateType.Disabled.equals(
                    hostIommuGetter.getState(msg.getHostUuid())))) {
            final APIGetPciDeviceCandidatesForAttachingVmReply reply = new APIGetPciDeviceCandidatesForAttachingVmReply();
            reply.setInventories(Collections.emptyList());
            bus.reply(msg, reply);
            throw new StopRoutingException();
        }

        if (msg.getTypes() == null || msg.getTypes().isEmpty()) {
            msg.setTypes(PciDeviceType.leagalPciDeviceCandidateTypes.stream().map(Enum::toString).collect(Collectors.toList()));
            return;
        }

        for (String type : msg.getTypes()) {
            if (!PciDeviceType.leagalPciDeviceCandidateTypes.contains(PciDeviceType.valueOf(type))) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "illegal type[%s] for pci device, only %s are legal",
                                type, PciDeviceType.leagalPciDeviceCandidateTypes));
            }
        }
    }

    private void validate(APIGetPciDeviceCandidatesForAttachingVmMsg msg) {
        VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (!(allowedVmInstanceAttachableStatus.contains(vmvo.getState()))) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("vm instance[uuid:%s] not in attachable status[%s] for pci device to attach", vmvo.getUuid(), allowedVmInstanceAttachableStatus)
            ));
        }

        if (msg.getTypes() == null || msg.getTypes().isEmpty()) {
            msg.setTypes(PciDeviceType.leagalPciDeviceCandidateTypes.stream().map(Enum::toString).collect(Collectors.toList()));
            return;
        }

        for (String type : msg.getTypes()) {
            if (!PciDeviceType.leagalPciDeviceCandidateTypes.contains(PciDeviceType.valueOf(type))) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "illegal type[%s] for pci device, only %s are legal",
                                type, PciDeviceType.leagalPciDeviceCandidateTypes));
            }
        }
    }

    private void validate(APIUpdateHostIommuStateMsg msg) {
        if (msg.getState() != null && msg.getState().equalsIgnoreCase(HostIommuStateType.Enabled.toString())) {
            msg.setState(HostIommuStateType.Enabled.toString());
        } else if (msg.getState() != null && msg.getState().equalsIgnoreCase(HostIommuStateType.Disabled.toString())) {
            msg.setState(HostIommuStateType.Disabled.toString());
        }
    }

    private void validate(APIAttachPciDeviceToVmMsg msg) {
        PciDeviceVO vo = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, msg.getPciDeviceUuid()).find();

        if (!PciDeviceType.leagalPciDeviceCandidateTypes.contains(vo.getType())) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    "illegal type[%s] for pci device, only %s are legal",
                    vo.getType(), PciDeviceType.leagalPciDeviceCandidateTypes));
        }

        if (PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(msg.getPciDeviceUuid()).isEmpty()) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    PciDeviceUtils.iommuGroupError(PciDeviceInventory.valueOf(vo))));
        }

        if (vo.getVirtStatus() == PciDeviceVirtStatus.SRIOV_VIRTUALIZED ||
                vo.getVirtStatus() == PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("pci device[uuid:%s] has been virtualized so it cannot be attached", msg.getPciDeviceUuid())
            ));
        }

        if (!(allowedPciDeviceAttachableStatus.contains(vo.getStatus()))) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("pci device[uuid:%s] not in attachable status[%s]", msg.getPciDeviceUuid(), allowedPciDeviceAttachableStatus)
            ));
        }

        if (vo.getVmInstanceUuid() != null) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("pci device[uuid:%s] has been attached to vm instance %s", msg.getPciDeviceUuid(), vo.getVmInstanceUuid())
            ));
        }

        if (vo.getState() != PciDeviceState.Enabled) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("pci device[uuid:%s] not in attachable state[%s]", msg.getPciDeviceUuid(), PciDeviceState.Enabled)
            ));
        }

        if (vo.getPassThroughState() != PciDevicePassThroughState.Enabled) {
            throw new ApiMessageInterceptionException(argerr("pci device[uuid:%s] not in enabled pass-through state[%s]",
                    msg.getPciDeviceUuid(), PciDevicePassThroughState.Enabled));
        }

        VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (vo.getVirtStatus() == PciDeviceVirtStatus.SRIOV_VIRTUAL && vmvo.getState() != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("vm instance[uuid:%s] not in attachable status[%s] for virtual pci device to attach", msg.getPciDeviceUuid(), VmInstanceState.Stopped)
            ));
        }

        if (!(allowedVmInstanceAttachableStatus.contains(vmvo.getState()))) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("vm instance[uuid:%s] not in attachable status[%s] for pci device to attach", msg.getPciDeviceUuid(), allowedVmInstanceAttachableStatus)
            ));
        }

        if (!allowedPciDeviceHotPlugUnplug(vmvo)) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("vm instance[uuid:%s] not in attachable status[%s] for pci device to attach", msg.getPciDeviceUuid(), allowedVmInstanceNonHotPlugStatus)
            ));
        }

        if (new HostIommuGetter().getState(vo.getHostUuid()).equals(HostIommuStateType.Disabled)) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("IOMMU state of the host[uuid:%s] that pci device[uuid:%s] belongs is not enabled", vo.getHostUuid(), msg.getPciDeviceUuid())
            ));
        }

        if (new HostIommuGetter().getStatus(vo.getHostUuid()).equals(HostIommuStatusType.Inactive)) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("IOMMU status of the host[uuid:%s] that pci device[uuid:%s] belongs is not active", vo.getHostUuid(), msg.getPciDeviceUuid())
            ));
        }

        validatePciDeviceHostConflict(vo, vmvo);
        validateMdevDeviceHostConflict(vo, vmvo);

        HostVO host = Q.New(HostVO.class).eq(HostVO_.uuid, vo.getHostUuid()).find();
        if (!host.getState().equals(HostState.Enabled) || !host.getStatus().equals(HostStatus.Connected)) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("the host[uuid:%s] that holds pci device[uuid:%s] is not in valid state[%s] or status[%s]",
                            vo.getHostUuid(), msg.getPciDeviceUuid(), HostState.Enabled, HostStatus.Connected)
            ));
        }
    }

    private void validatePciDeviceHostConflict(PciDeviceVO pciDeviceVO, VmInstanceVO vmInstanceVO) {
        List<PciDeviceVO> attachedPciDeviceVOS = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.vmInstanceUuid, vmInstanceVO.getUuid()).list();
        for (PciDeviceVO vo : attachedPciDeviceVOS) {
            if (!vo.getHostUuid().equals(pciDeviceVO.getHostUuid())) {
                throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("this pci device[uuid:%s] has different host uuid than the pci device[uuid:%s] attached to vm", pciDeviceVO.getUuid(), vo.getUuid())
                ));
            }
        }
    }

    private void validateMdevDeviceHostConflict(PciDeviceVO pciDeviceVO, VmInstanceVO vmInstanceVO) {
        List<MdevDeviceVO> attachedMdevDeviceVOS = Q.New(MdevDeviceVO.class).eq(MdevDeviceVO_.vmInstanceUuid, vmInstanceVO.getUuid()).list();
        for (MdevDeviceVO vo : attachedMdevDeviceVOS) {
            if (!vo.getHostUuid().equals(pciDeviceVO.getHostUuid())) {
                throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("this pci device[uuid:%s] has different host uuid from the mdev device[uuid:%s] attached to vm", pciDeviceVO.getUuid(), vo.getUuid())
                ));
            }
        }
    }

    private void validate(APIDetachPciDeviceFromVmMsg msg) {
        PciDeviceVO vo = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, msg.getPciDeviceUuid()).find();

        if (PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(msg.getPciDeviceUuid()).isEmpty()) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    PciDeviceUtils.iommuGroupError(PciDeviceInventory.valueOf(vo))));
        }

        if (!(allowedPciDeviceDetachableStatus.contains(vo.getStatus()))) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("pci device[uuid:%s] status not in detachable status %s", msg.getPciDeviceUuid(), allowedPciDeviceDetachableStatus)
            ));
        }

        if (vo.getVmInstanceUuid() == null || !vo.getVmInstanceUuid().equals(msg.getVmInstanceUuid())) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("pci device[uuid:%s] has not attached to vm instance %s", msg.getPciDeviceUuid(), msg.getVmInstanceUuid())
            ));
        }

        VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();

        if (vo.getVirtStatus() == PciDeviceVirtStatus.SRIOV_VIRTUAL && vmvo.getState() != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("vm instance[uuid:%s] not in detachable status[%s] for virtual pci device to detach", msg.getPciDeviceUuid(), VmInstanceState.Stopped)
            ));
        }

        if (!(allowedVmInstanceDetachableStatus.contains(vmvo.getState()))) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("vm instance[uuid:%s] not in detachable status[%s] for pci device to detach", msg.getPciDeviceUuid(), allowedVmInstanceDetachableStatus)
            ));
        }

        if (!allowedPciDeviceHotPlugUnplug(vmvo)) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("vm instance[uuid:%s] not in detachable status[%s] for pci device to detach", msg.getPciDeviceUuid(), allowedVmInstanceNonHotPlugStatus)
            ));
        }
    }

    private void validate(APICreatePciDeviceOfferingMsg msg) {
        Pattern p = Pattern.compile("\\w\\w\\w\\w");
        Matcher matcher = p.matcher(msg.getVendorId());
        if (!matcher.find()) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("wrong vendor id format")
            ));
        }

        matcher = p.matcher(msg.getDeviceId());
        if (!matcher.find()) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("wrong device id format")
            ));
        }

        if (msg.getSubvendorId() != null) {
            matcher = p.matcher(msg.getSubvendorId());
            if (!matcher.find()) {
                throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("wrong subvendor id format")
                ));
            }
        }

        if (msg.getSubdeviceId() != null) {
            matcher = p.matcher(msg.getSubdeviceId());
            if (!matcher.find()) {
                throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("wrong subdevice id format")
                ));
            }
        }

        /* new PciDeviceOffering can not be same with other offering */
        PciDeviceVO newVo = new PciDeviceVO();
        newVo.setVendorId(msg.getVendorId());
        newVo.setDeviceId(msg.getDeviceId());
        newVo.setSubvendorId(msg.getSubvendorId());
        newVo.setSubdeviceId(msg.getSubdeviceId());
        List<PciDeviceOfferingVO> vos = Q.New(PciDeviceOfferingVO.class).list();
        for (PciDeviceOfferingVO vo : vos) {
            if (vo.matchPciDevice(newVo)) {
                throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                        String.format("Pci device offering have same pci config")
                ));
            }
        }
    }

    private void validate(APIDeletePciDeviceMsg msg) {
        PciDeviceVO vo = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, msg.getUuid()).find();

        if (new HostIommuGetter().getState(vo.getHostUuid()).equals(HostIommuStateType.Disabled)) {
            return;
        }

        throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                "DO NOT DELETE PCI DEVICES MANUALLY"
        ));
    }

    private void validate(APIUpdatePciDeviceMsg msg) {
        if (msg.getDescription() != null && msg.getDescription().length() > 2048) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("pci device description too long, must shorter than 2048")
            ));
        }

        if (msg.getMetaData() != null && msg.getMetaData().length() > 2048) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("pci device metadata too long, must shorter than 2048")
            ));
        }

        if (msg.getState() != null && msg.getState().equalsIgnoreCase(PciDeviceState.Enabled.toString())) {
            msg.setState(PciDeviceState.Enabled.toString());
        } else if (msg.getState() != null && msg.getState().equalsIgnoreCase(PciDeviceState.Disabled.toString())) {
            msg.setState(PciDeviceState.Disabled.toString());
        }

        if (msg.getPassThroughState() != null) {
            validatePciDevicePassThroughState(msg.getUuid());
        }

        if (msg.getMetaData() != null && msg.getMetaData().matches("(\\w*:\\w*)(;\\w*:\\w*)*+")) {
            List<PciDeviceMetaDataEntry> entries = new ArrayList<>();
            for (String s : msg.getMetaData().split(";")) {
                PciDeviceMetaDataEntry entry = new PciDeviceMetaDataEntry(
                        s.split(":")[0],
                        PciDeviceMetaDataEntry.PciDeviceMetaDataOperator.Equal,
                        s.split(":")[1]
                );
                entries.add(entry);
            }
            msg.setMetaData(new PciDeviceMetaData(entries).toString());
        } else if (msg.getMetaData() != null) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("pci device metadata not correct, must like k1:v1;k2:v2")
            ));
        }

    }

    private void validatePciDevicePassThroughState(String pciUuid) {
        PciDeviceVO vo = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.uuid, pciUuid).find();
        if (vo.getPassThroughState() == PciDevicePassThroughState.Enabled) {
            if (vo.getVmInstanceUuid() != null) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "pci device[uuid:%s] cannot update pass-through state, because it is attached on vm[uuid:%s]",
                        vo.getUuid(), vo.getVmInstanceUuid()));
            }
        } else if (vo.getPassThroughState() == PciDevicePassThroughState.Available) {
            String hostIommuState = new HostIommuGetter().getState(vo.getHostUuid()).toString();
            String hostIommuStatus = new HostIommuGetter().getStatus(vo.getHostUuid()).toString();
            if (Objects.equals(hostIommuState, HostIommuStateType.Disabled.toString())) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "pci device[uuid:%s] cannot update pass-through state, because iommu is disabled on host[uuid:%s]",
                        vo.getUuid(), vo.getHostUuid()));
            } else if (Objects.equals(hostIommuStatus, HostIommuStatusType.Inactive.toString())) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "pci device[uuid:%s] cannot update pass-through state, because iommu is inactive on host[uuid:%s]",
                        vo.getUuid(), vo.getHostUuid()));

            }
        } else {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "pci device[uuid:%s] cannot update pass-through state, because state is [%s]",
                    vo.getHostUuid(), vo.getPassThroughState()));
        }
    }

    private void validate(APIGenerateSriovPciDevicesMsg msg) {
        PciDeviceVO pci = dbf.findByUuid(msg.getPciDeviceUuid(), PciDeviceVO.class);
        if (pci == null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "pci device[uuid:%s] doesn't exist", msg.getPciDeviceUuid()));
        }

        if (new HostIommuGetter().getState(pci.getHostUuid()) == HostIommuStateType.Disabled) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "could not enable sriov for device because iommu is disabled on host[uuid:%s]", pci.getHostUuid()));
        }

        boolean attached;
        boolean virtualized;
        List<String> specUuids;

        if (pci.getType() == PciDeviceType.GPU_Video_Controller || pci.getType() == PciDeviceType.GPU_3D_Controller) {
            virtualized = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.hostUuid, pci.getHostUuid())
                    .eq(PciDeviceVO_.type, pci.getType())
                    .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                    .isExists();

            attached = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.hostUuid, pci.getHostUuid())
                    .eq(PciDeviceVO_.type, pci.getType())
                    .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE)
                    .eq(PciDeviceVO_.status, PciDeviceStatus.Attached)
                    .isExists();

            specUuids = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.hostUuid, pci.getHostUuid())
                    .eq(PciDeviceVO_.type, pci.getType())
                    .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE)
                    .select(PciDeviceVO_.pciSpecUuid)
                    .listValues();
        } else {
            virtualized = pci.getVirtStatus() == PciDeviceVirtStatus.SRIOV_VIRTUALIZED;

            attached = pci.getStatus() == PciDeviceStatus.Attached;

            specUuids = Collections.singletonList(pci.getPciSpecUuid());

            if (PciDevicePassThroughState.Enabled.equals(pci.getPassThroughState())) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "pci device[uuid:%s] is in pass-through state, cannot be virtualized", pci.getUuid()));
            }
        }

        if (virtualized) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "pci devices in host[uuid:%s] already sriov virtualized", pci.getHostUuid()));
        }

        if (attached) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot sr-iov virtualize pci devices in host[uuid:%s] that are attached to vm", pci.getHostUuid()));
        }

        DebugUtils.Assert(!specUuids.isEmpty(), "cannot find pci device spec of " + pci.getUuid());

        int minIns = Q.New(PciDeviceSpecVO.class)
                .in(PciDeviceSpecVO_.uuid, specUuids)
                .select(PciDeviceSpecVO_.maxPartNum)
                .orderBy(PciDeviceSpecVO_.maxPartNum, SimpleQuery.Od.ASC)
                .limit(1)
                .findValue();
        if (minIns < msg.getVirtPartNum()) {
            throw new ApiMessageInterceptionException(
                    Platform.argerr("only %d virtual pci devices can be generated by %ss in host[uuid:%s]",
                            minIns, pci.getType(), pci.getHostUuid())
            );
        }

        boolean connected = Q.New(HostVO.class)
                .eq(HostVO_.uuid, pci.getHostUuid())
                .eq(HostVO_.status, HostStatus.Connected)
                .isExists();
        if (!connected) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the host[uuid:%s] that pci device[uuid:%s] in is not Connected",
                    pci.getHostUuid(), pci.getUuid()
            ));
        }

        HostNetworkInterfaceVO interfaceVO = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.pciDeviceAddress, msg.getPciDeviceUuid()).find();
        if (interfaceVO != null && interfaceVO.getBondingUuid() != null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot sr-iov virtualize pci devices on interface[uuid:%s] that are been bonded", interfaceVO.getUuid()));
        }
    }

    private void validate(APIUngenerateSriovPciDevicesMsg msg) {
        PciDeviceVO pci = dbf.findByUuid(msg.getPciDeviceUuid(), PciDeviceVO.class);
        if (pci == null || pci.getVirtStatus() != PciDeviceVirtStatus.SRIOV_VIRTUALIZED) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "pci device[uuid:%s] doesn't exist or is not sriov virtualized", msg.getPciDeviceUuid()));
        }

        boolean attached = false;
        if (pci.getType() == PciDeviceType.GPU_Video_Controller || pci.getType()  == PciDeviceType.GPU_3D_Controller) {
            attached = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.hostUuid, pci.getHostUuid())
                    .eq(PciDeviceVO_.type, pci.getType())
                    .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUAL)
                    .eq(PciDeviceVO_.status, PciDeviceStatus.Attached)
                    .isExists();
        }
        // attached vf nics will be auto detached from vm before ungenerate, so no exception
        if (attached) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "virtual pci devices generated from pci devices in host[uuid:%s] still attached to vm", pci.getHostUuid()));
        }

        boolean connected = Q.New(HostVO.class)
                .eq(HostVO_.uuid, pci.getHostUuid())
                .eq(HostVO_.status, HostStatus.Connected)
                .isExists();
        if (!connected) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the host[uuid:%s] that pci device[uuid:%s] in is not Connected",
                    pci.getHostUuid(), pci.getUuid()
            ));
        }

        if (pci.getType().equals(PciDeviceType.Ethernet_Controller)) {
            List<String> pausedVms = SQL.New("select vm.uuid from VmInstanceVO vm, EthernetVfPciDeviceVO vf" +
                            " where vm.state = :vmState" +
                            " and vm.uuid = vf.vmInstanceUuid" +
                            " and vf.vfStatus = :vfStatus" +
                            " and vf.parentUuid = :parentUuid", String.class)
                    .param("vmState", VmInstanceState.Paused)
                    .param("vfStatus", EthernetVfStatus.Attached)
                    .param("parentUuid", pci.getUuid())
                    .list();
            if (!pausedVms.isEmpty()) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "sub-devices of pci device[uuid:%s] are attached to paused VMs[uuids:%s], please detach them first",
                        pci.getUuid(), pausedVms
                ));
            }
        }
    }

    private void validate(APIGenerateMdevDevicesMsg msg) {
        PciDeviceVO pci = dbf.findByUuid(msg.getPciDeviceUuid(), PciDeviceVO.class);
        if (pci.getState() != PciDeviceState.Enabled ||
                pci.getStatus() == PciDeviceStatus.Attached ||
                pci.getVirtStatus() != PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE) {
            throw new ApiMessageInterceptionException(
                    Platform.argerr("pci device[uuid:%s] cannot be virtualized into mdevs, " +
                            "make sure it's enabled and un-attached", msg.getPciDeviceUuid())
            );
        }

        boolean exists = Q.New(PciDeviceMdevSpecRefVO.class)
                .eq(PciDeviceMdevSpecRefVO_.pciDeviceUuid, msg.getPciDeviceUuid())
                .eq(PciDeviceMdevSpecRefVO_.mdevSpecUuid, msg.getMdevSpecUuid())
                .isExists();
        if (!exists) {
            throw new ApiMessageInterceptionException(
                    Platform.argerr("pci device[uuid:%s] cannot be virtualized by mdev spec[uuid:%s]",
                            msg.getPciDeviceUuid(), msg.getMdevSpecUuid()
                    )
            );
        }

        boolean connected = Q.New(HostVO.class)
                .eq(HostVO_.uuid, pci.getHostUuid())
                .eq(HostVO_.status, HostStatus.Connected)
                .isExists();
        if (!connected) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the host[uuid:%s] that pci device[uuid:%s] in is not Connected",
                    pci.getHostUuid(), pci.getUuid()
            ));
        }
    }

    private void validate(APIUngenerateMdevDevicesMsg msg) {
        PciDeviceVO pci = dbf.findByUuid(msg.getPciDeviceUuid(), PciDeviceVO.class);
        if (pci.getVirtStatus() != PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED) {
            throw new ApiMessageInterceptionException(
                    Platform.argerr("pci device[uuid:%s] is not virtualized into mdevs", msg.getPciDeviceUuid())
            );
        }

        List<String> mdevUuids = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.parentUuid, msg.getPciDeviceUuid())
                .select(MdevDeviceVO_.uuid)
                .listValues();
        if (mdevUuids != null && !mdevUuids.isEmpty()) {
            msg.setMdevUuids(mdevUuids);
        }

        boolean exists = Q.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.uuid, mdevUuids)
                .eq(MdevDeviceVO_.status, MdevDeviceStatus.Attached)
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "mdev devices generated from pci device[uuid:%s] still attached to vm", msg.getPciDeviceUuid()));
        }

        boolean connected = Q.New(HostVO.class)
                .eq(HostVO_.uuid, pci.getHostUuid())
                .eq(HostVO_.status, HostStatus.Connected)
                .isExists();
        if (!connected) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the host[uuid:%s] that pci device[uuid:%s] in is not Connected",
                    pci.getHostUuid(), pci.getUuid()
            ));
        }
    }


    @Override
    public ErrorCode checkVmWhereMemorySnapshotExistExternalDevices(String VmInstanceUuid) {
        if (Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, VmInstanceUuid)
                .notEq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUAL)
                .in(PciDeviceVO_.type, Arrays.asList(PciDeviceType.GPU_Video_Controller, PciDeviceType.GPU_3D_Controller))
                .isExists()) {
            return argerr("please umount all GPU devices of the vm[%s] and try again", VmInstanceUuid);
        }

        if (Q.New(MdevDeviceVO.class).eq(MdevDeviceVO_.vmInstanceUuid, VmInstanceUuid).isExists()) {
            return argerr("please umount all vGPU devices of the vm[%s] and try again", VmInstanceUuid);
        }

        if (Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, VmInstanceUuid)
                .in(PciDeviceVO_.type, Arrays.asList(PciDeviceType.Moxa_Device, PciDeviceType.Custom))
                .isExists()) {
            return argerr("please umount other pci devices of the vm[%s] and try again", VmInstanceUuid);
        }

        return null;
    }
}
