package org.zstack.compute.sriov;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.sriov.APIChangeVfNicHaStateMsg;
import org.zstack.header.sriov.APIChangeVmNicTypeMsg;
import org.zstack.header.sriov.VmVfNicConstant;
import org.zstack.header.sriov.VmVfNicHaState;
import org.zstack.header.sriov.VmVfNicVO;
import org.zstack.header.sriov.VmVfNicVO_;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg;
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

/**
 * Created by GuoYi on 11/28/19.
 */
@InterceptorForService("sriov")
public class VmVfNicApiInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VmVfNicApiInterceptor.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APICreateL2NetworkMsg) {
            validate((APICreateL2NetworkMsg) msg);
        } else if (msg instanceof APIChangeVmNicTypeMsg) {
            validate((APIChangeVmNicTypeMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            validate((APIAttachL3NetworkToVmMsg) msg);
        } else if (msg instanceof APIChangeVmNicNetworkMsg) {
            validate((APIChangeVmNicNetworkMsg) msg);
        } else if (msg instanceof APIUpdateVmNicDriverMsg) {
            validate((APIUpdateVmNicDriverMsg) msg);
        } else if (msg instanceof APIChangeVfNicHaStateMsg) {
            validate((APIChangeVfNicHaStateMsg) msg);
        } else if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        } else if (msg instanceof APILocalStorageMigrateVolumeMsg) {
            validate((APILocalStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVmMsg) {
            validate((APIPrimaryStorageMigrateVmMsg) msg);
        }
        return msg;
    }

    private void checkL2NetworkTypeSupportSriov(String l2Type) {
        if (!L2NetworkType.getSriovSupportedTypeNames().contains(l2Type)) {
            throw new ApiMessageInterceptionException(argerr("only %s support sr-iov", L2NetworkType.getSriovSupportedTypeNames()));
        }
    }

    private void checkL2NetworkVswitchTypeSupportSriov(String vSwitchType) {
        if (L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK.equals(vSwitchType)) {
            throw new ApiMessageInterceptionException(argerr("%s don't support sr-iov", L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK));
        }
    }

    private void checkL3NetworkSupportSriov(String l3Uuid) {
        String l2Uuid = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.uuid, l3Uuid)
                .select(L3NetworkVO_.l2NetworkUuid)
                .findValue();
        if (StringUtils.isEmpty(l2Uuid)) {
            throw new ApiMessageInterceptionException(argerr("L3 Network [uuid:%s] doesn't exist", l3Uuid));
        }

        if (!SriovSystemTags.L2_ENABLE_SRIOV.hasTag(l2Uuid)) {
            throw new ApiMessageInterceptionException(argerr("related l2 network[uuid:%s] of l3 network[uuid:%s] is not sr-iov enabled", l2Uuid, l3Uuid));
        }
    }

    private void checkVmVfNicSupportMigration(String vmUuid, String hostUuid, String errorString) {
        VmInstanceVO vmVO = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        List<String> nicUuids = new ArrayList<>();
        for (VmNicVO nic : vmVO.getVmNics()) {
            if (nic.getType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE)) {
                nicUuids.add(nic.getUuid());
            }
        }

        if (nicUuids.isEmpty()) {
            return;
        }

        List<VmVfNicVO> vfNics = Q.New(VmVfNicVO.class).in(VmVfNicVO_.uuid, nicUuids).list();
        for (VmVfNicVO vf : vfNics) {
            if (!vfPciDeviceUtils.hasAvailableVfDevice(hostUuid, vf.getL3NetworkUuid())) {
                throw new ApiMessageInterceptionException(argerr(
                        "%s, because there is no sr-iov device available on l3Network[uuid:%s] of the host[uuid:%s]",
                        errorString, vf.getL3NetworkUuid(), hostUuid));
            }
        }
    }

    private void validate(APIPrimaryStorageMigrateVmMsg msg) {
        String dstHostUuid = msg.getDstHostUuid();
        if (dstHostUuid != null) {
            checkVmVfNicSupportMigration(msg.getVmInstanceUuid(), dstHostUuid,
                    String.format("could not migrate primary storage to host[uuid:%s] of vm[uuid:%s]",
                            dstHostUuid, msg.getVmInstanceUuid()));
        }
    }

    private void validate(APILocalStorageMigrateVolumeMsg msg) {
        String vmUuid = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                .select(VolumeVO_.vmInstanceUuid)
                .findValue();
        if (vmUuid == null || msg.getDestHostUuid() == null) {
            return;
        }
        checkVmVfNicSupportMigration(vmUuid, msg.getDestHostUuid(),
                String.format("could not migrate volume[uuid:%s] to host[uuid:%s] of vm[uuid:%s]",
                        msg.getVolumeUuid(), msg.getDestHostUuid(), vmUuid));
    }

    private void validate(APIMigrateVmMsg msg) {
        checkVmVfNicSupportMigration(msg.getVmInstanceUuid(), msg.getHostUuid(),
                String.format("could not migrate vm[uuid:%s] to host[uuid:%s]",
                        msg.getVmInstanceUuid(), msg.getHostUuid()));
    }

    private void validate(APIChangeVfNicHaStateMsg msg) {
        VmVfNicVO vo = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, msg.getVfNicUuid()).find();
        if (vo == null) {
            throw new ApiMessageInterceptionException(
                    argerr("could not change vf nic ha state, because vf nic[uuid:%s] doesn't exist",
                            msg.getVfNicUuid()));
        }

        if (!VmVfNicHaState.isValid(msg.getHaState())) {
            throw new ApiMessageInterceptionException(
                    argerr("could not change vf nic ha state, because ha state[%s] is invalid", msg.getHaState()));
        }
    }

    private void validate(APICreateVmInstanceMsg msg) {
        if (msg.getVmNicParams() == null) {
            return;
        }

        List<VmNicParam> nicParams;
        try {
            nicParams = JSONObjectUtil.toCollection(msg.getVmNicParams(), ArrayList.class, VmNicParam.class);
        } catch (JsonSyntaxException e) {
            throw new ApiMessageInterceptionException(argerr("invalid json format, causes: %s", e.getMessage()));
        }

        if (CollectionUtils.isEmpty(nicParams)) {
            return;
        }

        List<String> sriovEnabledL3Uuids = nicParams.stream()
                .filter(VmNicParam::isSriovEnabled)
                .map(VmNicParam::getL3NetworkUuid).distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sriovEnabledL3Uuids)) {
            return;
        }

        sriovEnabledL3Uuids.forEach(this::checkL3NetworkSupportSriov);
    }

    private void validate(APICreateL2NetworkMsg msg) {
        if (CollectionUtils.isEmpty(msg.getSystemTags())) {
            return;
        }

        PatternedSystemTag tag = SriovSystemTags.L2_ENABLE_SRIOV;
        String tagValue = SystemTagUtils.findTagValue(msg.getSystemTags(), tag);
        if (StringUtils.isEmpty(tagValue)) {
            return;
        }

        checkL2NetworkTypeSupportSriov(msg.getType());
        checkL2NetworkVswitchTypeSupportSriov(msg.getvSwitchType());
    }

    private void validate(APIChangeVmNicTypeMsg msg) {
        VmNicVO vnic = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        if (vnic == null) {
            throw new ApiMessageInterceptionException(argerr("vm nic[uuid:%s] doesn't exist", msg.getVmNicUuid()));
        }

        if (VmVfNicConstant.VIRTUAL_FUNCTION_TYPE.equals(vnic.getType())) {
            throw new ApiMessageInterceptionException(argerr("can not change vf nic to vnic type"));
        }

        VmInstanceVO vm = dbf.findByUuid(vnic.getVmInstanceUuid(), VmInstanceVO.class);
        if (vm.getState() != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(argerr("vm nic type could only be updated when the vm is stopped"));
        }
    }

    private void validate(APIAttachL3NetworkToVmMsg msg) {
        if (StringUtils.isEmpty(msg.getVmNicParams())) {
            return;
        }

        VmNicParam vmNicParam;
        try {
            vmNicParam = JSONObjectUtil.toObject(msg.getVmNicParams(), VmNicParam.class);
        } catch (JsonSyntaxException e) {
            throw new ApiMessageInterceptionException(argerr("invalid json format, causes: %s", e.getMessage()));
        }

        if (!vmNicParam.isSriovEnabled()) {
            return;
        }

        checkL3NetworkSupportSriov(msg.getL3NetworkUuid());
    }

    private void validate(APIChangeVmNicNetworkMsg msg) {
        boolean enableSriov = false;
        VmNicParam vmNicParam = new VmNicParam();
        if (!StringUtils.isEmpty(msg.getVmNicParams())) {
            try {
                vmNicParam = JSONObjectUtil.toObject(msg.getVmNicParams(), VmNicParam.class);
            } catch (JsonSyntaxException e) {
                throw new ApiMessageInterceptionException(argerr("invalid json format, causes: %s", e.getMessage()));
            }

            if (vmNicParam.isSriovEnabled()) {
                enableSriov = true;
            }
        }

        boolean isVfNic = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, msg.getVmNicUuid()).isExists();
        if (!enableSriov) {
            if (isVfNic) {
                throw new ApiMessageInterceptionException(argerr("can not change vf nic to vnic type"));
            }
            return;
        }

        if (!isVfNic) {
            throw new ApiMessageInterceptionException(argerr("could not enable sr-iov for vnic"));
        }

        VmNicVO vmNicVO = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        if (!msg.getDestL3NetworkUuid().equals(vmNicVO.getL3NetworkUuid())) {
            throw new ApiMessageInterceptionException(argerr("could not change l3 network of vf nic"));
        }

        if (vmNicParam.getVfParentUuid() != null) {
            List<Tuple> tuples = SQL.New("select itf.uuid, itf.interfaceName from HostNetworkInterfaceVO itf," +
                            " PciDeviceVO pci, EthernetVfPciDeviceVO vf" +
                            " where itf.hostUuid = pci.hostUuid" +
                            " and itf.pciDeviceAddress = pci.pciDeviceAddress" +
                            " and pci.uuid = vf.parentUuid" +
                            " and vf.vmInstanceUuid = :vmUuid" +
                            " and vf.l3NetworkUuid = :l3Uuid", Tuple.class)
                    .param("vmUuid", vmNicVO.getVmInstanceUuid())
                    .param("l3Uuid", vmNicVO.getL3NetworkUuid())
                    .list();

            if (CollectionUtils.isEmpty(tuples)) {
                logger.warn(String.format("could not find parent of vf nic[uuid:%s]", vmNicVO.getUuid()));
                return;
            }
            String VfParentUuid = tuples.get(0).get(0, String.class);
            String parentName = tuples.get(0).get(1, String.class);

            if (!vmNicParam.getVfParentUuid().equals(VfParentUuid)) {
                throw new ApiMessageInterceptionException(argerr("could not change to vf nic of pf[uuid: %s], current vf parent name is [%s]",
                        vmNicParam.getVfParentUuid(), parentName));
            }
        }

        checkL3NetworkSupportSriov(msg.getDestL3NetworkUuid());
    }

    private void validate(APIUpdateVmNicDriverMsg msg) {
        boolean enableSriov = msg.getDriverType() != null && msg.getDriverType().equals(VmNicConstant.NIC_DRIVER_TYPE_SR_IOV);

        boolean isVfNic = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, msg.getVmNicUuid()).isExists();
        if (!enableSriov) {
            if (isVfNic) {
                throw new ApiMessageInterceptionException(argerr("can not change vf nic to vnic type"));
            }
            return;
        }

        if (!isVfNic) {
            throw new ApiMessageInterceptionException(argerr("could not enable sr-iov for vnic"));
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
                APICreateVmInstanceMsg.class,
                APICreateL2NetworkMsg.class,
                APIChangeVmNicTypeMsg.class,
                APIAttachL3NetworkToVmMsg.class,
                APIChangeVmNicNetworkMsg.class,
                APIUpdateVmNicDriverMsg.class,
                APIMigrateVmMsg.class,
                APIPrimaryStorageMigrateVmMsg.class,
                APILocalStorageMigrateVolumeMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
