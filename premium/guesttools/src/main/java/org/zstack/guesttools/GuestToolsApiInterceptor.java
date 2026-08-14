package org.zstack.guesttools;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmConfigSyncHelper;
import org.zstack.compute.vm.VmHostnameUtils;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.guesttools.advanced.APICreateVmCustomSpecificationMsg;
import org.zstack.guesttools.advanced.APIUpdateVmCustomSpecificationMsg;
import org.zstack.guesttools.advanced.VmCustomSpecificationUtils;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.configuration.VmCustomSpecificationDomainMode;
import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.sriov.APIChangeVfNicHaStateMsg;
import org.zstack.header.sriov.VmVfNicConstant;
import org.zstack.header.sriov.VmVfNicHaState;
import org.zstack.header.sriov.VmVfNicVO;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.vm.*;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.header.vm.VmInstanceConstant.USER_VM_TYPE;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by GuoYi on 2019-09-17.
 */
@InterceptorForService("guest.tools")
public class GuestToolsApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(GuestToolsApiInterceptor.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    private static final VmConfigSyncHelper vmConfigSyncHelper = new VmConfigSyncHelper();

    private void setServiceId(APIMessage msg) {
        if (msg instanceof APIAttachGuestToolsIsoToVmMsg) {
            APIAttachGuestToolsIsoToVmMsg amsg = (APIAttachGuestToolsIsoToVmMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, GuestToolsConstant.SERVICE_ID, amsg.getUuid());
        } else if (msg instanceof APIGetVmGuestToolsInfoMsg) {
            APIGetVmGuestToolsInfoMsg gmsg = (APIGetVmGuestToolsInfoMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, GuestToolsConstant.SERVICE_ID, gmsg.getVmInstanceUuid());
        } else if (msg instanceof APIUpdateVmNetworkConfigMsg) {
            APIUpdateVmNetworkConfigMsg umsg = (APIUpdateVmNetworkConfigMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, GuestToolsConstant.SERVICE_ID, umsg.getVmInstanceUuid());
        } else if (msg instanceof APIDetachGuestToolsIsoFromVmMsg) {
            APIDetachGuestToolsIsoFromVmMsg umsg = (APIDetachGuestToolsIsoFromVmMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, GuestToolsConstant.SERVICE_ID, umsg.getVmInstanceUuid());
        } else if (msg instanceof APIUpdateGuestToolsStateMsg) {
            APIUpdateGuestToolsStateMsg umsg = (APIUpdateGuestToolsStateMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, GuestToolsConstant.SERVICE_ID, umsg.getVmInstanceUuid());
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APISetVmHostnameMsg.class);
        ret.add(APIMigrateVmMsg.class);
        ret.add(APIChangeVfNicHaStateMsg.class);
        ret.add(APIPrimaryStorageMigrateVmMsg.class);
        ret.add(APICloneVmInstanceMsg.class);
        ret.add(APICreateVmInstanceFromTemplatedVmInstanceMsg.class);
        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachGuestToolsIsoToVmMsg) {
            validate((APIAttachGuestToolsIsoToVmMsg) msg);
        } else if (msg instanceof APIGetVmGuestToolsInfoMsg) {
            validate((APIGetVmGuestToolsInfoMsg) msg);
        } else if (msg instanceof APIUpdateVmNetworkConfigMsg) {
            validate((APIUpdateVmNetworkConfigMsg) msg);
        } else if (msg instanceof APIUpdateGuestToolsStateMsg) {
            validate((APIUpdateGuestToolsStateMsg) msg);
        } else if (msg instanceof APISetVmHostnameMsg) {
            validate((APISetVmHostnameMsg) msg);
        } else if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        } else if (msg instanceof APIChangeVfNicHaStateMsg) {
            validate((APIChangeVfNicHaStateMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVmMsg) {
            validate((APIPrimaryStorageMigrateVmMsg) msg);
        } else if (msg instanceof APICloneVmInstanceMsg) {
            validate((APICloneVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromTemplatedVmInstanceMsg) {
            validate((APICreateVmInstanceFromTemplatedVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmCustomSpecificationMsg) {
            validate((APICreateVmCustomSpecificationMsg) msg);
        } else if (msg instanceof APIUpdateVmCustomSpecificationMsg) {
            validate((APIUpdateVmCustomSpecificationMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APIPrimaryStorageMigrateVmMsg msg) {
        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vm.getState() != VmInstanceState.Running) {
            return;
        }

        List<String> nicUuids = new ArrayList<>();
        for (VmNicVO nic : vm.getVmNics()) {
            if (nic.getType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE)) {
                nicUuids.add(nic.getUuid());
            }
        }
        if (nicUuids.isEmpty()) {
            return;
        }

        GuestToolsStateVO toolsState = Q.New(GuestToolsStateVO.class)
                .eq(GuestToolsStateVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .find();
        if (!toolsState.getQgaState().equals(GuestToolsQgaState.Running)){
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "could not migrate primary storage, because vm[uuid:%s] attached vf nic but guesttools not running",
                    msg.getVmInstanceUuid()));
        }
    }

    private void validate(APIChangeVfNicHaStateMsg msg) {
        VmVfNicVO vfNicVO = dbf.findByUuid(msg.getVfNicUuid(), VmVfNicVO.class);
        if (vfNicVO == null) {
            return;
        }

        if (msg.getHaState().equals(VmVfNicHaState.Enabled.toString())) {
            GuestToolsStateVO toolsState = Q.New(GuestToolsStateVO.class)
                    .eq(GuestToolsStateVO_.vmInstanceUuid, vfNicVO.getVmInstanceUuid())
                    .find();
            if (!toolsState.getQgaState().equals(GuestToolsQgaState.Running)) {
                throw new ApiMessageInterceptionException(Platform.argerr(
                        "could not change vf nic ha state, because guesttools not running."));
            }
        }
    }

    private void validate(APIMigrateVmMsg msg) {
       VmInstanceVO vmVO = dbf.findByUuid(msg.getVmUuid(), VmInstanceVO.class);
        List<String> nicUuids = new ArrayList<>();
        for (VmNicVO nic : vmVO.getVmNics()) {
            if (nic.getType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE)) {
                nicUuids.add(nic.getUuid());
            }
        }

        if (nicUuids.isEmpty()) {
            return;
        }

        GuestToolsStateVO toolsState = Q.New(GuestToolsStateVO.class)
                .eq(GuestToolsStateVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .find();
        if (!toolsState.getQgaState().equals(GuestToolsQgaState.Running)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "could not migrate vm[uuid:%s], because vf nic is attached but guesttools not running", msg.getVmUuid()));
        }
    }

    private void validate(APISetVmHostnameMsg msg) {
        String hostname = msg.getHostname();
        String vmUuid = msg.getVmInstanceUuid();
        VmInstanceVO vmVO = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        String defaultL3uuid = vmVO.getDefaultL3NetworkUuid();
        if (defaultL3uuid == null || defaultL3uuid.isEmpty()) {
            throw new ApiMessageInterceptionException(Platform.operr("unable to set vm hostname. the vm[uuid:%s] do not have default L3 network", vmUuid));
        }
        String sql = "select t" +
                        " from SystemTagVO t, VmInstanceVO vm, VmNicVO nic" +
                        " where t.resourceUuid = vm.uuid" +
                        " and vm.uuid = nic.vmInstanceUuid" +
                        " and nic.l3NetworkUuid = :l3Uuid" +
                        " and t.tag = :sysTag";
        TypedQuery<SystemTagVO> q = dbf.getEntityManager().createQuery(sql, SystemTagVO.class);
        q.setParameter("l3Uuid", defaultL3uuid);
        q.setParameter("sysTag", TagUtils.tagPatternToSqlPattern(VmSystemTags.HOSTNAME.instantiateTag(
                                                    map(e(VmSystemTags.HOSTNAME_TOKEN, hostname)))));
        List<SystemTagVO> vos = q.getResultList();
        if (!vos.isEmpty()) {
            SystemTagVO sameTag = vos.get(0);
            throw new ApiMessageInterceptionException(Platform.argerr("conflict hostname, there has been a VM[uuid:%s] having hostname[%s] on L3 network[uuid:%s]",
                                                sameTag.getResourceUuid(), hostname, defaultL3uuid)
                    .withOpaque("vm.uuid", sameTag.getResourceUuid())
                    .withOpaque("hostname", hostname)
                    .withOpaque("l3.uuid", defaultL3uuid));
        }

        VmHostnameUtils.validateHostname(hostname, ImagePlatform.Windows.toString().equals(vmVO.getPlatform()));
    }

    private void validate(APIUpdateVmNetworkConfigMsg msg) {
        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vm.getState() != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "update vm[uuid:%s] network config failed, because vm not running.", msg.getVmInstanceUuid()));
        }

        if (!USER_VM_TYPE.equals(vm.getType() )) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "update vm[uuid:%s] network config failed, because the vm type %s is not supported.",
                                msg.getVmInstanceUuid(), vm.getType()));
        }

        GuestToolsStateVO toolsState = Q.New(GuestToolsStateVO.class)
                .eq(GuestToolsStateVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .find();
        if (toolsState.getQgaState().equals(GuestToolsQgaState.NotUpgraded)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "update vm[uuid:%s] network config failed, because the guesttools version is too low for this feature.",
                            msg.getVmInstanceUuid()));
        } else if (!toolsState.getQgaState().equals(GuestToolsQgaState.Running)){
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "update vm[uuid:%s] network config failed, because guesttools not running.", msg.getVmInstanceUuid()));
        }
    }

    private void validate(APIAttachGuestToolsIsoToVmMsg msg) {
        VmInstanceVO vm = dbf.findByUuid(msg.getUuid(), VmInstanceVO.class);
        if (!GuestToolsConstant.HYPERVISOR_TYPES_SUPPORT_GUEST_TOOLS.contains(vm.getHypervisorType())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot attach guest-tools iso to vm[uuid:%s] because it's hypervisor type is not supported",
                    msg.getUuid()
            ));
        }

        if (vm.getState() != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot attach guest-tools iso to vm[uuid:%s] because it's not running",
                    msg.getUuid()
            ));
        }

        if (!VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot attach guest-tools iso to vm[uuid:%s] because it's not user vm",
                    msg.getUuid()
            ));
        }

        if (CollectionUtils.isEmpty(vm.getVmCdRoms())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot attach guest-tools iso to vm[uuid:%s] because it has no cdrom",
                    msg.getUuid()
            ));
        }

        // ZSTAC-55541: For now, we will sync NIC information to new created VM which include guest-tools in their image
        if (vmConfigSyncHelper.vmNeedSyncPorts(vm.getUuid())) {
            vmConfigSyncHelper.afterVmSyncPorts(vm.getUuid());
        }

    }

    private void validate(APIGetVmGuestToolsInfoMsg msg) {
        validateVmGuestToolsMsg(msg);
        validateGuestToolsDebugItems(msg);
    }

    private void validateVmGuestToolsMsg(VmInstanceMessage msg) {
        final String vmUuid = msg.getVmInstanceUuid();
        VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);

        if (vm.getState() != VmInstanceState.Running && vm.getState() != VmInstanceState.VolumeRecovering) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot get guest-tools info from vm[uuid:%s] because it's not running", vmUuid
            ));
        }

        if (!VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot get guest-tools info from vm[uuid:%s] because it's not user vm", vmUuid
            ));
        }
    }

    private void validateGuestToolsDebugItems(APIGetVmGuestToolsInfoMsg msg) {
        if (CollectionUtils.isEmpty(msg.getDebug())) {
            return;
        }

        if (msg.getDebug().contains(GuestToolsInfoDebugItem.ALL)) {
            msg.setDebug(Stream.of(GuestToolsInfoDebugItem.values())
                    .map(i -> i.item)
                    .collect(Collectors.toSet()));
            return;
        }

        Set<String> invalidSet = msg.getDebug().stream()
                .filter(item -> GuestToolsInfoDebugItem.findByItemName(item) == null)
                .collect(Collectors.toSet());
        if (!invalidSet.isEmpty()) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "invalid debug parameter: %s", invalidSet));
        }
    }

    private void validate(APIUpdateGuestToolsStateMsg msg) {
        VmInstanceVO vmVo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vmVo == null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "can not update guest tools state for vm [uuid:%s] because vm is deleted", msg.getVmInstanceUuid()));
        }

        if (!VmInstanceConstant.USER_VM_TYPE.equals(vmVo.getType())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "can not update guest tools state for vm[uuid:%s] because it's not user vm",
                    msg.getVmInstanceUuid()));
        }
    }

    private void validate(APICloneVmInstanceMsg msg) {
        if (msg.getVmCustomSpecification() != null) {
            String platform = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.platform)
                    .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                    .findValue();
            checkIfCustomSpecificationSupported(msg.getVmInstanceUuid(), platform);
            VmCustomSpecificationUtils.validate(msg.getVmCustomSpecification(), platform);
        }
    }

    private void validate(APICreateVmInstanceFromTemplatedVmInstanceMsg msg) {
        if (msg.getVmCustomSpecification() != null) {
            checkIfCustomSpecificationSupported(msg.getTemplatedVmInstanceUuid(), msg.getPlatform());
            VmCustomSpecificationUtils.validate(msg.getVmCustomSpecification(), msg.getPlatform());
        }
    }

    private void checkIfCustomSpecificationSupported(String vmUuid, String platform) {
        GuestToolsStateVO toolsState = Q.New(GuestToolsStateVO.class)
                .eq(GuestToolsStateVO_.vmInstanceUuid, vmUuid)
                .find();

        if (toolsState == null || toolsState.getVersion() == null) {
            throw new ApiMessageInterceptionException(Platform.operr(
                    "vm[uuid:%s] has no guest tools installed, custom specification is not supported", vmUuid));
        }

        if (!ImagePlatform.Windows.toString().equals(platform)) {
            logger.debug("Linux vm support custom specification without specific guest tools");
            return;
        }

        if (new VersionComparator(toolsState.getVersion()).compare(
                GuestToolsConstant.CUSTOM_SPECIFICATION_SUPPORT_VERSION_FOR_WINDOWS) < 0) {
            throw new ApiMessageInterceptionException(Platform.operr(
                    "custom specification need guest tools version >= [%s], but got [%s] on vm[uuid:%s]",
                    GuestToolsConstant.CUSTOM_SPECIFICATION_SUPPORT_VERSION_FOR_WINDOWS, toolsState.getVersion(), vmUuid));

        }
    }

    private void validate(APICreateVmCustomSpecificationMsg msg) {
        VmCustomSpecificationStruct spec = new VmCustomSpecificationStruct();
        spec.setPlatform(msg.getPlatform());
        spec.setHostname(msg.getHostname());
        spec.setRootPassword(msg.getRootPassword());
        spec.setGenerateSID(msg.isGenerateSID());
        if (msg.getDomainMode() != null) {
            spec.setDomainMode(VmCustomSpecificationDomainMode.valueOf(msg.getDomainMode()));
        }
        spec.setDomainName(msg.getDomainName());
        spec.setDomainUsername(msg.getDomainUsername());
        spec.setDomainPassword(msg.getDomainPassword());
        spec.setOrganization(msg.getOrganization());

        VmCustomSpecificationUtils.validate(spec);
        msg.setVmCustomSpecification(spec);
    }

    private void validate(APIUpdateVmCustomSpecificationMsg msg) {
        VmCustomSpecificationStruct spec = new VmCustomSpecificationStruct();
        spec.setUuid(msg.getUuid());
        spec.setHostname(msg.getHostname());
        spec.setRootPassword(msg.getRootPassword());
        spec.setGenerateSID(msg.isGenerateSID());
        if (msg.getDomainMode() != null) {
            spec.setDomainMode(VmCustomSpecificationDomainMode.valueOf(msg.getDomainMode()));
        }
        spec.setDomainName(msg.getDomainName());
        spec.setDomainUsername(msg.getDomainUsername());
        spec.setDomainPassword(msg.getDomainPassword());
        spec.setOrganization(msg.getOrganization());

        VmCustomSpecificationUtils.validate(spec);
        msg.setVmCustomSpecification(spec);
    }
}
