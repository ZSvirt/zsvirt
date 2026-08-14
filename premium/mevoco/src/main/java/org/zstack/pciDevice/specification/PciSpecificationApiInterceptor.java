package org.zstack.pciDevice.specification;

import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.specification.mdev.*;
import org.zstack.pciDevice.specification.pci.*;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-05-06.
 */
public class PciSpecificationApiInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(PciSpecificationApiInterceptor.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIUpdatePciDeviceSpecMsg) {
            validate((APIUpdatePciDeviceSpecMsg) msg);
        } else if (msg instanceof APIGetPciDeviceSpecCandidatesMsg) {
            validate((APIGetPciDeviceSpecCandidatesMsg) msg);
        } else if (msg instanceof APIAddPciDeviceSpecToVmInstanceMsg) {
            validate((APIAddPciDeviceSpecToVmInstanceMsg) msg);
        } else if (msg instanceof APIRemovePciDeviceSpecFromVmInstanceMsg) {
            validate((APIRemovePciDeviceSpecFromVmInstanceMsg) msg);
        } else if (msg instanceof APIGetMdevDeviceSpecCandidatesMsg) {
            validate((APIGetMdevDeviceSpecCandidatesMsg) msg);
        } else if (msg instanceof APIAddMdevDeviceSpecToVmInstanceMsg) {
            validate((APIAddMdevDeviceSpecToVmInstanceMsg) msg);
        } else if (msg instanceof APIRemoveMdevDeviceSpecFromVmInstanceMsg) {
            validate((APIRemoveMdevDeviceSpecFromVmInstanceMsg) msg);
        }
        return msg;
    }

    private void validate(APIUpdatePciDeviceSpecMsg msg) {
        PciDeviceSpecVO spec = dbf.findByUuid(msg.getUuid(), PciDeviceSpecVO.class);
        if (msg.getRomContent() == null && spec.getRomContent() == null && !StringUtils.isEmpty(msg.getRomVersion())) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    "don't set rom version if has no rom content"
            ));
        }

        if (msg.getRomContent() != null && msg.getRomContent().length() > PciDeviceConstants.ROM_MAX_LENGTH) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    "too large pci device rom file"
            ));
        }

        if (msg.isAbandonSpecRom() &&
                (!StringUtils.isEmpty(msg.getRomContent()) || !StringUtils.isEmpty(msg.getRomVersion()))) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    "don't set rom content/version if you want to abandon rom from the spec"
            ));
        }

        if (msg.getRomContent() == null && spec.getRomContent() == null && msg.isAbandonSpecRom()) {
            logger.debug(String.format("there is no rom to abandon in pci device spec[uuid:%s]", msg.getUuid()));
        }
    }

    private void validate(APIGetPciDeviceSpecCandidatesMsg msg) {
        boolean setClusters = msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty();
        boolean setHost = !StringUtils.isEmpty(msg.getHostUuid());
        boolean setVm = !StringUtils.isEmpty(msg.getVmInstanceUuid());
        boolean setVms = msg.getVmInstanceUuids() != null && !msg.getVmInstanceUuids().isEmpty();
        if (BooleanUtils.toInteger(setClusters) +
                BooleanUtils.toInteger(setHost) +
                BooleanUtils.toInteger(setVm) +
                BooleanUtils.toInteger(setVms) != 1) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cluster uuids or host uuid or vm uuid can not be set at same time"));
        }

        // convert msg.getVmInstanceUuid() to msg.getVmInstanceUuids()
        if (setVm && !setVms) {
            msg.setVmInstanceUuids(Collections.singletonList(msg.getVmInstanceUuid()));
            setVms = true;
        }

        if (setClusters) {
            List<String> clsUuids = msg.getClusterUuids();
            if (clsUuids.size() != Q.New(ClusterVO.class)
                    .in(ClusterVO_.uuid, clsUuids).eq(ClusterVO_.state, ClusterState.Enabled).count()) {
                throw new ApiMessageInterceptionException(Platform.argerr("clusters not exist or disabled"));
            }
        }

        if (setHost) {
            HostIommuGetter getter = new HostIommuGetter();
            if (getter.getState(msg.getHostUuid()) != HostIommuStateType.Enabled ||
                    getter.getStatus(msg.getHostUuid()) != HostIommuStatusType.Active) {
                logger.debug(String.format("host[uuid:%s] iommu is not ready, so no pci device spec candidates", msg.getHostUuid()));
                final APIGetPciDeviceSpecCandidatesReply reply = new APIGetPciDeviceSpecCandidatesReply();
                reply.setInventories(Collections.emptyList());
                bus.reply(msg, reply);
                throw new StopRoutingException();
            }
        }

        if (setVms) {
            for (String vmUuid : msg.getVmInstanceUuids()) {
                VmInstanceState state = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .select(VmInstanceVO_.state)
                        .findValue();

                // no need to add new specs to vm when it's not stopped
                if (state != VmInstanceState.Stopped) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "vm instance[uuid:%s, state:%s] needs to be stopped to set pci device spec",
                            vmUuid, state
                    ));
                }
            }
        }

        if (msg.getTypes() == null || msg.getTypes().isEmpty()) {
            msg.setTypes(PciDeviceType.leagalPciDeviceCandidateTypes.stream().map(Enum::toString).collect(Collectors.toList()));
            return;
        }

        for (String type : msg.getTypes()) {
            if (!PciDeviceType.leagalPciDeviceCandidateTypes.contains(PciDeviceType.valueOf(type))) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "illegal type[%s] for pci device spec, only %s are legal",
                                type, PciDeviceType.leagalPciDeviceCandidateTypes));
            }
        }
    }

    private void validate(APIAddPciDeviceSpecToVmInstanceMsg msg) {
        boolean exists = Q.New(VmInstancePciDeviceSpecRefVO.class)
                .eq(VmInstancePciDeviceSpecRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(VmInstancePciDeviceSpecRefVO_.pciSpecUuid, msg.getPciSpecUuid())
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm[uuid:%s] already has pci device spec[uuid:%s]", msg.getVmInstanceUuid(), msg.getPciSpecUuid()
            ));
        }

        // no need to add new specs to vm when it's not stopped
        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vm.getState() != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm instance[uuid:%s, state:%s] needs to be stopped to set pci device spec",
                    vm.getUuid(), vm.getState()
            ));
        }

        PciDeviceSpecVO vo = dbf.findByUuid(msg.getPciSpecUuid(), PciDeviceSpecVO.class);
        if (!PciDeviceType.leagalPciDeviceCandidateTypes.contains(vo.getType())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "illegal type[%s] for pci device spec, only %s are legal",
                    vo.getType(), PciDeviceType.leagalPciDeviceCandidateTypes));
        }
    }

    private void validate(APIRemovePciDeviceSpecFromVmInstanceMsg msg) {
        boolean exists = Q.New(VmInstancePciDeviceSpecRefVO.class)
                .eq(VmInstancePciDeviceSpecRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(VmInstancePciDeviceSpecRefVO_.pciSpecUuid, msg.getPciSpecUuid())
                .isExists();
        if (!exists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm[uuid:%s] doesn't have pci device spec[uuid:%s]", msg.getVmInstanceUuid(), msg.getPciSpecUuid()
            ));
        }

        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vm.getState() != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm instance[uuid:%s, state:%s] needs to be stopped to remove pci device spec[uuid:%s]",
                    vm.getUuid(), vm.getState(), msg.getPciSpecUuid()
            ));
        }
    }

    private void validate(APIGetMdevDeviceSpecCandidatesMsg msg) {
        boolean setClusters = msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty();
        boolean setHost = !StringUtils.isEmpty(msg.getHostUuid());
        boolean setVm = !StringUtils.isEmpty(msg.getVmInstanceUuid());
        boolean setVms = msg.getVmInstanceUuids() != null && !msg.getVmInstanceUuids().isEmpty();
        if (BooleanUtils.toInteger(setClusters) +
                BooleanUtils.toInteger(setHost) +
                BooleanUtils.toInteger(setVm) +
                BooleanUtils.toInteger(setVms) != 1) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cluster uuids or host uuid or vm uuid can not be set at same time"));
        }

        // convert msg.getVmInstanceUuid() to msg.getVmInstanceUuids()
        if (setVm && !setVms) {
            msg.setVmInstanceUuids(Collections.singletonList(msg.getVmInstanceUuid()));
            setVms = true;
        }

        if (setClusters) {
            List<String> clsUuids = msg.getClusterUuids();
            if (clsUuids.size() != Q.New(ClusterVO.class).in(ClusterVO_.uuid, clsUuids).count()) {
                throw new ApiMessageInterceptionException(Platform.argerr("clusters not exist or disabled"));
            }
        }

        if (setHost) {
            HostIommuGetter hostIommuGetter = new HostIommuGetter();
            if (hostIommuGetter.getState(msg.getHostUuid()).equals(HostIommuStateType.Disabled) ||
                    hostIommuGetter.getStatus(msg.getHostUuid()).equals(HostIommuStatusType.Inactive)) {
                logger.debug(String.format("host[uuid:%s] iommu is not ready, so no mdev device spec candidates", msg.getHostUuid()));
                final APIGetMdevDeviceSpecCandidatesReply reply = new APIGetMdevDeviceSpecCandidatesReply();
                reply.setInventories(Collections.emptyList());
                bus.reply(msg, reply);
                throw new StopRoutingException();
            }
        }

        if (setVms) {
            for (String vmUuid : msg.getVmInstanceUuids()) {
                VmInstanceState state = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .select(VmInstanceVO_.state)
                        .findValue();

                // no need to add new specs to vm when it's not stopped
                if (state != VmInstanceState.Stopped) {
                    throw new ApiMessageInterceptionException(Platform.argerr(
                            "vm instance[uuid:%s, state:%s] needs to be stopped to set pci device spec",
                            vmUuid, state
                    ));
                }
            }
        }

        List<String> legalTypes = Arrays.stream(MdevDeviceType.values()).map(Enum::toString).collect(Collectors.toList());
        if (msg.getTypes() == null || msg.getTypes().isEmpty()) {
            return;
        }

        for (String type : msg.getTypes()) {
            if (!legalTypes.contains(type)) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "illegal mdev device type [%s], only %s are legal", type, legalTypes));
            }
        }
    }

    private void validate(APIAddMdevDeviceSpecToVmInstanceMsg msg) {
        boolean exists = Q.New(VmInstanceMdevDeviceSpecRefVO.class)
                .eq(VmInstanceMdevDeviceSpecRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(VmInstanceMdevDeviceSpecRefVO_.mdevSpecUuid, msg.getMdevSpecUuid())
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm[uuid:%s] already has mdev device spec[uuid:%s]", msg.getVmInstanceUuid(), msg.getMdevSpecUuid()
            ));
        }

        // no need to add new specs to vm when it's not stopped
        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vm.getState() != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm instance[uuid:%s, state:%s] needs to be stopped to set mdev device spec",
                    vm.getUuid(), vm.getState()
            ));
        }
    }

    private void validate(APIRemoveMdevDeviceSpecFromVmInstanceMsg msg) {
        boolean exists = Q.New(VmInstanceMdevDeviceSpecRefVO.class)
                .eq(VmInstanceMdevDeviceSpecRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(VmInstanceMdevDeviceSpecRefVO_.mdevSpecUuid, msg.getMdevSpecUuid())
                .isExists();
        if (!exists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm[uuid:%s] doesn't have mdev device spec[uuid:%s]", msg.getVmInstanceUuid(), msg.getMdevSpecUuid()
            ));
        }

        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vm.getState() != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm instance[uuid:%s, state:%s] needs to be stopped to remove mdev device spec[uuid:%s]",
                    vm.getUuid(), vm.getState(), msg.getMdevSpecUuid()
            ));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIAddMdevDeviceSpecToVmInstanceMsg.class,
            APIGetMdevDeviceSpecCandidatesMsg.class,
            APIRemoveMdevDeviceSpecFromVmInstanceMsg.class,
            APIUpdateMdevDeviceSpecMsg.class,
            APIAddPciDeviceSpecToVmInstanceMsg.class,
            APIGetPciDeviceSpecCandidatesMsg.class,
            APIRemovePciDeviceSpecFromVmInstanceMsg.class,
            APIUpdatePciDeviceSpecMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
