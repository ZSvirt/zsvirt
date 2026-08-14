package org.zstack.compute.vm;

import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.Component;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.VSwitchType;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.tag.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmNicManagerImpl implements VmNicManager, VmNicExtensionPoint, PrepareDbInitialValueExtensionPoint, VmPlatformChangedExtensionPoint, Component {
    private static final CLogger logger = Utils.getLogger(VmNicManagerImpl.class);

    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private DatabaseFacade dbf;

    private List<String> supportNicDriverTypes;
    private String defaultPVNicDriver;
    private String defaultNicDriver;
    private String pcNetNicDriver;

    @Override
    public void afterAddIpAddress(String vmNicUUid, String usedIpUuid) {
        /* update UsedIpVO */
        SQL.New(UsedIpVO.class).eq(UsedIpVO_.uuid, usedIpUuid).set(UsedIpVO_.vmNicUuid, vmNicUUid).update();

        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid).find();

        UsedIpVO temp = null;
        /* if there is ipv4 addresses, we put the first attached ipv4 address to VmNic.ip
         * or we put the first attached ipv6 address to vmNic.Ip */
        List<UsedIpVO> refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv4)
                .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
        if (refs != null && !refs.isEmpty()) {
            temp = refs.get(0);
        } else {
            refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv6)
                    .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
            if (refs != null && !refs.isEmpty()) {
                temp = refs.get(0);
            }
        }

        if (temp != null && !temp.getUuid().equals(nic.getUsedIpUuid())) {
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid)
                    .set(VmNicVO_.ip, temp.getIp())
                    .set(VmNicVO_.netmask, temp.getNetmask())
                    .set(VmNicVO_.gateway, temp.getGateway())
                    .set(VmNicVO_.usedIpUuid, temp.getUuid())
                    .set(VmNicVO_.ipVersion, temp.getIpVersion())
                    .set(VmNicVO_.l3NetworkUuid, temp.getL3NetworkUuid()).update();
        }
    }

    @Override
    public void afterDelIpAddress(String vmNicUUid, String usedIpUuid) {
        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid).find();
        if (nic.getUsedIpUuid() != null && !nic.getUsedIpUuid().equals(usedIpUuid)) {
            return;
        }

        UsedIpVO temp = null;
        /* if there is ipv4 addresses, we put the first attached ipv4 address to VmNic.ip
         * or we put the first attached ipv6 address to vmNic.Ip */
        List<UsedIpVO> refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv4)
                .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
        if (refs != null && !refs.isEmpty()) {
            temp = refs.get(0);
        } else {
            refs = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipVersion, IPv6Constants.IPv6)
                    .eq(UsedIpVO_.vmNicUuid, vmNicUUid).orderBy(UsedIpVO_.createDate, SimpleQuery.Od.ASC).list();
            if (refs != null && !refs.isEmpty()) {
                temp = refs.get(0);
            }
        }

        if (temp != null) {
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUUid)
                    .set(VmNicVO_.ip, temp.getIp())
                    .set(VmNicVO_.netmask, temp.getNetmask())
                    .set(VmNicVO_.gateway, temp.getGateway())
                    .set(VmNicVO_.usedIpUuid, temp.getUuid())
                    .set(VmNicVO_.ipVersion, temp.getIpVersion())
                    .set(VmNicVO_.l3NetworkUuid, temp.getL3NetworkUuid()).update();
        }
    }

    @Override
    public void prepareDbInitialValue() {
        List<VmNicVO> nics = Q.New(VmNicVO.class).notNull(VmNicVO_.vmInstanceUuid).list();
        List<VmNicVO> ns = nics.stream()
                .filter(v -> v.getDriverType() == null
                        && v.getType().equals(VmInstanceConstant.VIRTUAL_NIC_TYPE)
                        && v.getVmInstanceUuid() != null)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(ns)) {
            return;
        }

        List<String> vmUuids = ns.stream()
                .map(VmNicVO::getVmInstanceUuid)
                .collect(Collectors.toList());

        List<Tuple> tupleList = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.platform)
                .in(VmInstanceVO_.uuid, vmUuids)
                .listTuple();

        Map<String, String> vmPlatforms = Maps.newHashMap();
        for (Tuple vmTuple : tupleList) {
            String vmUuid = vmTuple.get(0, String.class);
            String vmPlatform = vmTuple.get(1, String.class);
            vmPlatforms.put(vmUuid, ImagePlatform.valueOf(vmPlatform).isParaVirtualization() ?
                    defaultPVNicDriver : defaultNicDriver);
        }

        Map<Boolean, List<String>> nicGroups = nics.stream()
                .filter(v -> vmPlatforms.containsKey(v.getVmInstanceUuid()))
                .collect(
                        Collectors.groupingBy(
                                v -> vmPlatforms.get(v.getVmInstanceUuid()).equals(defaultPVNicDriver),
                                Collectors.mapping(VmNicVO::getUuid, Collectors.toList()))
                );

        List<String> pvNics = nicGroups.get(true);
        List<String> defaultNics = nicGroups.get(false);

        if (CollectionUtils.isNotEmpty(pvNics)) {
            SQL.New(VmNicVO.class)
                    .in(VmNicVO_.uuid, pvNics)
                    .set(VmNicVO_.driverType, defaultPVNicDriver)
                    .update();
        }

        if (CollectionUtils.isNotEmpty(defaultNics)) {
            SQL.New(VmNicVO.class)
                    .in(VmNicVO_.uuid, defaultNics)
                    .set(VmNicVO_.driverType, defaultNicDriver)
                    .update();
        }
    }

    @Override
    public boolean skipPlatformChange(VmInstanceInventory vm, String previousPlatform, String nowPlatform) {
        return false;
    }

    @Override
    public void vmPlatformChange(VmInstanceInventory vm, String previousPlatform, String nowPlatform) {
        String driver = null;
        for (VmNicSetDriverExtensionPoint ext : pluginRegistry.getExtensionList(VmNicSetDriverExtensionPoint.class)) {
            driver = ext.getPreferredVmNicDriver(vm);
            if (driver != null) {
                break;
            }
        }

        if (driver != null) {
            resetVmNicDriverType(vm.getUuid(), driver);
            return;
        }

        if (ImagePlatform.valueOf(nowPlatform).isParaVirtualization()) {
            resetVmNicDriverType(vm.getUuid(), defaultPVNicDriver);
            return;
        }

        resetVmNicDriverType(vm.getUuid(), defaultNicDriver);
    }

    private void resetVmNicDriverType(String vmUuid, String driverType) {
        VmInstanceVO vo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
        List<String> needUpdateNics = vo.getVmNics().stream()
                .filter(nic -> nic.getDriverType() == null || !nic.getDriverType().equals(driverType))
                .map(VmNicVO::getUuid)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(needUpdateNics)) {
            return;
        }

        SQL.New(VmNicVO.class).in(VmNicVO_.uuid, needUpdateNics).set(VmNicVO_.driverType, driverType).update();
    }

    public void setSupportNicDriverTypes(List<String> supportNicDriverTypes) {
        this.supportNicDriverTypes = supportNicDriverTypes;
    }

    public void setDefaultPVNicDriver(String defaultPVNicDriver) {
        this.defaultPVNicDriver = defaultPVNicDriver;
    }

    public void setDefaultNicDriver(String defaultNicDriver) {
        this.defaultNicDriver = defaultNicDriver;
    }

    @Override
    public List<String> getSupportNicDriverTypes() {
        return supportNicDriverTypes;
    }

    @Override
    public String getDefaultPVNicDriver() {
        return defaultPVNicDriver;
    }

    @Override
    public String getDefaultNicDriver() {
        return defaultNicDriver;
    }

    @Override
    public String getPcNetNicDriver() {
        return pcNetNicDriver;
    }

    public void setPcNetNicDriver(String pcNetNicDriver) {
        this.pcNetNicDriver = pcNetNicDriver;
    }

    @Override
    public void setNicDriverType(VmNicInventory nic, boolean isImageSupportVirtIo, boolean isParaVirtualization, VmInstanceInventory vm) {
        String driver = null;
        for (VmNicSetDriverExtensionPoint ext : pluginRegistry.getExtensionList(VmNicSetDriverExtensionPoint.class)) {
            driver = ext.getPreferredVmNicDriver(vm);
            if (driver != null) {
                break;
            }
        }

        if (driver != null) {
            nic.setDriverType(driver);
            return;
        }

        if (isImageSupportVirtIo || isParaVirtualization || VmSystemTags.VIRTIO.hasTag(nic.getVmInstanceUuid())) {
            nic.setDriverType(getDefaultPVNicDriver());
        } else {
            nic.setDriverType(getDefaultNicDriver());
        }
    }

    @Override
    public VmNicType getVmNicType(String vmUuid, L3NetworkInventory l3nw, boolean enableSriov) {
        logger.debug(String.format("create %s on l3 network[uuid:%s] inside VmAllocateNicFlow",
                enableSriov ? "vf nic" : "vnic", l3nw.getUuid()));
        boolean enableVhostUser = NetworkServiceGlobalConfig.ENABLE_VHOSTUSER.value(Boolean.class);

        L2NetworkVO l2nw =  dbf.findByUuid(l3nw.getL2NetworkUuid(), L2NetworkVO.class);
        VmNicType type;
        if (l2nw.getType().equals(L2NetworkConstant.L2_TF_NETWORK_TYPE)) {
            type = VmNicType.valueOf(VmInstanceConstant.TF_VIRTUAL_NIC_TYPE);
        } else {
            VSwitchType vSwitchType = VSwitchType.valueOf(l2nw.getvSwitchType());
            type = vSwitchType.getVmNicTypeWithCondition(enableSriov, enableVhostUser);
        }

        return type;
    }

    @Override
    public boolean start() {
        VmSystemTags.VIRTIO.installLifeCycleListener(new SystemTagLifeCycleListener() {
            @Override
            public void tagCreated(SystemTagInventory tag) {
                resetVmNicDriverType(tag.getResourceUuid(), defaultPVNicDriver);
                vidm.deleteResourceMetadataByVmModifyVirtIO(tag.getResourceUuid());
            }

            @Override
            public void tagDeleted(SystemTagInventory tag) {
                resetVmNicDriverType(tag.getResourceUuid(), defaultNicDriver);
                vidm.deleteResourceMetadataByVmModifyVirtIO(tag.getResourceUuid());
            }

            @Override
            public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {

            }
        });

        VmSystemTags.VIRTIO.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                VmInstanceState state = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, resourceUuid)
                        .select(VmInstanceVO_.state)
                        .findValue();

                if (state == VmInstanceState.Running || state == VmInstanceState.Unknown) {
                    throw new OperationFailureException(argerr("vm current state[%s], " +
                            "modify virtio requires the vm state[%s]", state, VmInstanceState.Stopped));
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
