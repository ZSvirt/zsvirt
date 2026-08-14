package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.pciDevice.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-05-07.
 */
public class MdevDeviceApiInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(MdevDeviceApiInterceptor.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIUpdateMdevDeviceMsg) {
            validate((APIUpdateMdevDeviceMsg) msg);
        } else if (msg instanceof APIAttachMdevDeviceToVmMsg) {
            validate((APIAttachMdevDeviceToVmMsg) msg);
        } else if (msg instanceof APIDetachMdevDeviceFromVmMsg) {
            validate((APIDetachMdevDeviceFromVmMsg) msg);
        } else if (msg instanceof APIGetMdevDeviceCandidatesMsg) {
            validate((APIGetMdevDeviceCandidatesMsg) msg);
        } else if (msg instanceof APIDeleteMdevDeviceMsg) {
            validate((APIDeleteMdevDeviceMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof MdevDeviceMessage) {
            MdevDeviceMessage mmsg = (MdevDeviceMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, MdevDeviceConstants.SERVICE_ID, mmsg.getMdevDeviceUuid());
        }
    }

    private void validate(APIUpdateMdevDeviceMsg msg) {
        MdevDeviceVO mdev = dbf.findByUuid(msg.getMdevDeviceUuid(), MdevDeviceVO.class);
        if (msg.getState() != null && mdev.getStatus() == MdevDeviceStatus.Attached) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot change the state of mdev device that's in attached status"));
        }
    }

    private void validate(APIAttachMdevDeviceToVmMsg msg) {
        MdevDeviceVO mdev = dbf.findByUuid(msg.getMdevDeviceUuid(), MdevDeviceVO.class);
        if (mdev.getState() == MdevDeviceState.Disabled || mdev.getStatus() == MdevDeviceStatus.Attached) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot attach mdev device[uuid:%s] to vm, make sure it's enabled and un-attached",
                    msg.getMdevDeviceUuid()
            ));
        }

        boolean isSe = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.uuid, msg.getMdevDeviceUuid())
                .eq(MdevDeviceVO_.type, MdevDeviceType.SE_Controller)
                .isExists();
        if (!isSe) {
            boolean stopped = Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                    .eq(VmInstanceVO_.state, VmInstanceState.Stopped)
                    .isExists();
            if (!stopped) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "cannot attach mdev device to vm instance that's not stopped"));
            }
        }

        boolean wrongHost = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .notEq(PciDeviceVO_.hostUuid, mdev.getHostUuid())
                .isExists();
        if (wrongHost) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm[uuid:%s] has pci devices attached that are in different host with mdev device[uuid:%s]",
                    msg.getVmInstanceUuid(), msg.getMdevDeviceUuid()));
        }

        wrongHost = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .notEq(MdevDeviceVO_.hostUuid, mdev.getHostUuid())
                .isExists();
        if (wrongHost) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "vm[uuid:%s] has mdev devices attached that are in different host with mdev device[uuid:%s]",
                    msg.getVmInstanceUuid(), msg.getMdevDeviceUuid()));
        }

        HostVO host = dbf.findByUuid(mdev.getHostUuid(), HostVO.class);
        if (host.getState() != HostState.Enabled || host.getStatus() != HostStatus.Connected) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the host[uuid:%s] that holds mdev device[uuid:%s] is not [%s] and [%s]",
                    mdev.getHostUuid(), mdev.getUuid(), HostState.Enabled, HostStatus.Connected
            ));
        }

        if (isSe) {
            long attachedNum = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.hostUuid, mdev.getHostUuid())
                    .eq(MdevDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .count();
            if (attachedNum > 0) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "the vm[uuid:%s] that holds se mdev device can not attach more se mdev[%s]",
                        msg.getVmInstanceUuid(), mdev.getUuid()
                ));
            }
            return;
        }

        HostIommuGetter getter = new HostIommuGetter();
        if (getter.getState(mdev.getHostUuid()) != HostIommuStateType.Enabled ||
                getter.getStatus(mdev.getHostUuid()) != HostIommuStatusType.Active) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "IOMMU of the host[uuid:%s] that hosts pci device[uuid:%s] is not [%s] and [%s]",
                    mdev.getHostUuid(), mdev.getUuid(), HostState.Enabled, HostStatus.Connected));
        }
    }

    private void validate(APIDetachMdevDeviceFromVmMsg msg) {
        boolean exists = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.uuid, msg.getMdevDeviceUuid())
                .eq(MdevDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .isExists();
        if (!exists) {
            throw new ApiMessageInterceptionException(
                    Platform.argerr("mdev device [uuid:%s] is not attached to vm[uuid:%s]",
                            msg.getMdevDeviceUuid(), msg.getVmInstanceUuid()));
        }

        boolean isSe = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.uuid, msg.getMdevDeviceUuid())
                .eq(MdevDeviceVO_.type, MdevDeviceType.SE_Controller)
                .isExists();
        if (isSe) {
            MdevDeviceVO mdev = dbf.findByUuid(msg.getMdevDeviceUuid(), MdevDeviceVO.class);
            HostVO host = dbf.findByUuid(mdev.getHostUuid(), HostVO.class);
            if (host.getState() != HostState.Enabled || host.getStatus() != HostStatus.Connected) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "the host[uuid:%s] that holds mdev device[uuid:%s] is not [%s] and [%s]",
                        mdev.getHostUuid(), mdev.getUuid(), HostState.Enabled, HostStatus.Connected
                ));
            }
            return;
        }

        boolean stopped = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .eq(VmInstanceVO_.state, VmInstanceState.Stopped)
                .isExists();
        if (!stopped) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot detach mdev device from vm instance when it's not stopped"));
        }
    }

    private void validate(APIGetMdevDeviceCandidatesMsg msg) {
        boolean setClusters = msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty();
        boolean setHost = !StringUtils.isEmpty(msg.getHostUuid());
        boolean setVm = !StringUtils.isEmpty(msg.getVmInstanceUuid());
        if ((setClusters && setHost) || (setClusters && setVm) || (setHost && setVm)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cluster uuids or host uuid or vm uuid can not be set at same time"));
        }

        if (setClusters) {
            List<String> clsUuids = msg.getClusterUuids();
            if (clsUuids.size() != Q.New(ClusterVO.class).in(ClusterVO_.uuid, clsUuids).count()) {
                throw new ApiMessageInterceptionException(Platform.argerr("clusters not exist or disabled"));
            }
        }

        if (setHost) {
            HostIommuGetter getter = new HostIommuGetter();
            if (getter.getState(msg.getHostUuid()) != HostIommuStateType.Enabled ||
                    getter.getStatus(msg.getHostUuid()) != HostIommuStatusType.Active) {
                logger.debug(String.format("host[uuid:%s] iommu is not ready, so no mdev device candidates", msg.getHostUuid()));
                final APIGetMdevDeviceCandidatesReply reply = new APIGetMdevDeviceCandidatesReply();
                reply.setInventories(Collections.emptyList());
                bus.reply(msg, reply);
                throw new StopRoutingException();
            }
        }

        if (setVm) {
            boolean stopped = Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                    .eq(VmInstanceVO_.state, VmInstanceState.Stopped)
                    .isExists();
            if (!stopped) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "cannot attach mdev device to vm instance that's not stopped"));
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

    private void validate(APIDeleteMdevDeviceMsg msg) {
        boolean attached = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.uuid, msg.getMdevDeviceUuid())
                .eq(MdevDeviceVO_.status, MdevDeviceStatus.Attached)
                .isExists();
        if (attached) {
            throw new ApiMessageInterceptionException(
                    Platform.argerr("cannot delete mdev device when it's attached"));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIAttachMdevDeviceToVmMsg.class,
            APIDeleteMdevDeviceMsg.class,
            APIDetachMdevDeviceFromVmMsg.class,
            APIGetMdevDeviceCandidatesMsg.class,
            APIUpdateMdevDeviceMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
