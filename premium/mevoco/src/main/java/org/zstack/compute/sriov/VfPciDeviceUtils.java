package org.zstack.compute.sriov;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmNicManager;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkGetInterfaceExtensionPoint;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.sriov.*;
import org.zstack.header.vdpa.VmVdpaNicConstant;
import org.zstack.header.vdpa.VmVdpaNicVO;
import org.zstack.header.vdpa.VmVdpaNicVO_;
import org.zstack.header.vm.*;
import org.zstack.network.hostNetworkInterface.*;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.pciDevice.PciDeviceInventory;
import org.zstack.pciDevice.PciDeviceStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VfPciDeviceUtils {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    VmNicManager nicManager;
    @Autowired
    private PluginRegistry pluginRgty;

    private static final CLogger logger = Utils.getLogger(VfPciDeviceUtils.class);

    List<String> getInterfaceNamesOfL2Network(String hostUuid, String l2NetworkUuid) {
        List<String> ret = new ArrayList<>();

        Tuple tuple = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2NetworkUuid)
                .select(L2NetworkVO_.physicalInterface, L2NetworkVO_.type)
                .findTuple();
        String l2InterfaceName = tuple.get(0, String.class);
        String l2Type = tuple.get(1, String.class);

        for (L2NetworkGetInterfaceExtensionPoint ext : pluginRgty.getExtensionList(L2NetworkGetInterfaceExtensionPoint.class)) {
            if (Objects.equals(ext.getType().toString(), l2Type)) {
                ret.addAll(ext.getHostNetworkInterfaceNames(l2NetworkUuid, hostUuid));
            }
        }

        if (!ret.isEmpty()) {
            return ret;
        }

        if (Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                .eq(HostNetworkInterfaceVO_.interfaceName, l2InterfaceName).isExists()) {
            ret.add(l2InterfaceName);
            return ret;
        }

        HostNetworkBondingVO bondingVO = Q.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                .eq(HostNetworkBondingVO_.bondingName, l2InterfaceName).find();
        if (bondingVO == null) {
            logger.debug(String.format("can not get physical interface of l2 network[uuid:%s] on host [uuid:%s]",
                    l2NetworkUuid, hostUuid));
            return ret;
        }

        return bondingVO.getSlaves().stream().map(HostNetworkInterfaceVO::getInterfaceName).collect(Collectors.toList());
    }

    List<String> getInterfaceNamesOfL3Network(String hostUuid, String l3NetworkUuid) {
        String l2NetworkUuid = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3NetworkUuid)
                .select(L3NetworkVO_.l2NetworkUuid).findValue();
        return getInterfaceNamesOfL2Network(hostUuid, l2NetworkUuid);
    }

    public boolean hasAvailableVfDevice(String hostUuid, String vmUuid, String l3NetworkUuid) {
        if (Q.New(EthernetVfPciDeviceVO.class)
                .eq(EthernetVfPciDeviceVO_.hostUuid, hostUuid)
                .eq(EthernetVfPciDeviceVO_.vmInstanceUuid, vmUuid)
                .eq(EthernetVfPciDeviceVO_.l3NetworkUuid, l3NetworkUuid).isExists()) {
            /* vf has been allocated */
            return true;
        }

        List<String> l2InterfaceNames = getInterfaceNamesOfL3Network(hostUuid, l3NetworkUuid);
        if (l2InterfaceNames.isEmpty()) {
            return false;
        }

        return Q.New(EthernetVfPciDeviceVO.class)
                .in(EthernetVfPciDeviceVO_.interfaceName, l2InterfaceNames)
                .eq(EthernetVfPciDeviceVO_.hostUuid, hostUuid)
                .eq(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Available).count() > 0;
    }

    public boolean hasAvailableVfDevice(String hostUuid, String l3NetworkUuid) {
        List<String> l2InterfaceNames = getInterfaceNamesOfL3Network(hostUuid, l3NetworkUuid);
        if (l2InterfaceNames.isEmpty()) {
            return false;
        }

        return Q.New(EthernetVfPciDeviceVO.class)
                .in(EthernetVfPciDeviceVO_.interfaceName, l2InterfaceNames)
                .eq(EthernetVfPciDeviceVO_.hostUuid, hostUuid)
                .eq(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Available).count() > 0;
    }

    public boolean hasAvailableVfDeviceForL2(String hostUuid, String l2NetworkUuid) {
        List<String> l2InterfaceNames = getInterfaceNamesOfL2Network(hostUuid, l2NetworkUuid);
        if (l2InterfaceNames.isEmpty()) {
            return false;
        }

        return Q.New(EthernetVfPciDeviceVO.class)
                .in(EthernetVfPciDeviceVO_.interfaceName, l2InterfaceNames)
                .eq(EthernetVfPciDeviceVO_.hostUuid, hostUuid)
                .eq(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Available).count() > 0;
    }

    public PciDeviceInventory reserveVfDevice(String hostUuid, String vmUuid, String l3NetworkUuid, String parentUuid,
                                              boolean releaseOldVf, EthernetVfStatus status) {
        EthernetVfPciDeviceVO oldVo = Q.New(EthernetVfPciDeviceVO.class)
                .eq(EthernetVfPciDeviceVO_.vmInstanceUuid, vmUuid)
                .eq(EthernetVfPciDeviceVO_.l3NetworkUuid, l3NetworkUuid).find();
        if (oldVo != null) {
            if (oldVo.getHostUuid().equals(hostUuid)) {
                /* vm is running on old host, no need to change vf device */
                if (oldVo.getVfStatus() != status && oldVo.getVfStatus() != EthernetVfStatus.Attached) {
                    oldVo.setVfStatus(status);
                    if (status == EthernetVfStatus.Attached) {
                        oldVo.setStatus(PciDeviceStatus.Attached);
                    } else {
                        oldVo.setStatus(PciDeviceStatus.Reserved);
                    }
                    oldVo = dbf.updateAndRefresh(oldVo);
                }
                return EthernetVfPciDeviceInventory.valueOf(oldVo);
            } else {
                if (releaseOldVf) {
                    /* vm is running on another host, release old vf device */
                    releaseVfDevice(EthernetVfPciDeviceInventory.valueOf(oldVo));
                } else {
                    /* 在vm迁移时使用: 源云主机在选择了目的物理机之后，变成Paused
                    *  迁移成功，释放他变成Available; 迁移失败，恢复使用变成Attached */
                    pauseVfPciDevice(EthernetVfPciDeviceInventory.valueOf(oldVo));
                }
            }
        }
        List<String> l2InterfaceNames = getInterfaceNamesOfL3Network(hostUuid, l3NetworkUuid);
        if (l2InterfaceNames.isEmpty()) {
            logger.debug(String.format("can not find interface of l3 network[uuid:%s] on host[uuid:%s]",
                    l3NetworkUuid, hostUuid));
            return null;
        }

        String interfaceInfo = String.format("name: %s", l2InterfaceNames);
        Q q = Q.New(EthernetVfPciDeviceVO.class)
                .in(EthernetVfPciDeviceVO_.interfaceName, l2InterfaceNames)
                .eq(EthernetVfPciDeviceVO_.hostUuid, hostUuid)
                .eq(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Available);
        if (parentUuid != null) {
            q.eq(EthernetVfPciDeviceVO_.parentUuid, parentUuid);
            interfaceInfo = interfaceInfo.concat(String.format(", uuid: %s", parentUuid));
        }

        EthernetVfPciDeviceVO vo = q.limit(1).find();
        if (vo == null) {
            logger.debug(String.format("there is no available vf pci device of interface [%s] of host[uuid:%s]",
                    interfaceInfo, hostUuid));
            return null;
        }

        vo.setVmInstanceUuid(vmUuid);
        vo.setVmUuid(vmUuid);
        vo.setL3NetworkUuid(l3NetworkUuid);
        vo.setVfStatus(status);
        if (status == EthernetVfStatus.Attached) {
            vo.setStatus(PciDeviceStatus.Attached);
        } else {
            vo.setStatus(PciDeviceStatus.Reserved);
        }
        dbf.update(vo);

        logger.debug(String.format("reserve vf pci[address:%s] device of interface [%s] of host[uuid:%s] for vm[uuid:%s]",
                vo.getPciDeviceAddress(), interfaceInfo, hostUuid, vmUuid));

        return EthernetVfPciDeviceInventory.valueOf(vo);
    }

    public PciDeviceInventory allocateReservedVfDevice(VmNicInventory inv) {
        EthernetVfPciDeviceVO vo = Q.New(EthernetVfPciDeviceVO.class)
                .eq(EthernetVfPciDeviceVO_.vmInstanceUuid, inv.getVmInstanceUuid())
                .eq(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Reserved)
                .eq(EthernetVfPciDeviceVO_.l3NetworkUuid, inv.getL3NetworkUuid()).find();
        if (vo != null) {
            /* has been allocated(for example, rebooted vm) or reserved(for example, new created vm) */
            if (vo.getVfStatus() == EthernetVfStatus.Attached) {
                return EthernetVfPciDeviceInventory.valueOf(vo);
            }

            if (vo.getVfStatus() == EthernetVfStatus.Available) {
                logger.debug(String.format("vf pci device[hostUuid:%s, pci address: %s] is not reserved, but assigned to vnic[uuid:%s]",
                        vo.getHostUuid(), vo.getPciDeviceAddress(), inv.getUuid()));
            }

            vo.setVfStatus(EthernetVfStatus.Attached);
            vo.setStatus(PciDeviceStatus.Attached);
            vo = dbf.updateAndRefresh(vo);
            return EthernetVfPciDeviceInventory.valueOf(vo);
        }

        throw new RuntimeException(String.format("vf pci device for vm[uuid:%s], l3 network [uuid:%s] doesn't find",
                inv.getVmInstanceUuid(), inv.getL3NetworkUuid()));
    }


    public void releaseVfDevice(PciDeviceInventory inv) {
        /* 可能是vf，也可能是vdpa网卡 */
        SQL.New(VmVfNicVO.class).eq(VmVfNicVO_.pciDeviceUuid, inv.getUuid())
                .set(VmVfNicVO_.pciDeviceUuid, null).update();
        SQL.New(VmVdpaNicVO.class).eq(VmVdpaNicVO_.pciDeviceUuid, inv.getUuid())
                .set(VmVdpaNicVO_.pciDeviceUuid, null).update();

        SQL.New(EthernetVfPciDeviceVO.class).eq(EthernetVfPciDeviceVO_.uuid, inv.getUuid())
                .set(EthernetVfPciDeviceVO_.vmInstanceUuid, null)
                .set(EthernetVfPciDeviceVO_.vmUuid, null)
                .set(EthernetVfPciDeviceVO_.l3NetworkUuid, null)
                .set(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Available)
                .set(EthernetVfPciDeviceVO_.status, PciDeviceStatus.Active).update();
        logger.debug(String.format("release vf pci[uuid:%s, address:%s] of host[uuid:%s]",
                inv.getUuid(), inv.getPciDeviceAddress(), inv.getHostUuid()));
    }

    public void releaseVfDevice(VmInstanceInventory inv) {
        List<String> nicUuids = inv.getVmNics().stream().map(VmNicInventory::getUuid).collect(Collectors.toList());
        if (!nicUuids.isEmpty()) {
            SQL.New(VmVfNicVO.class).in(VmVfNicVO_.uuid, nicUuids)
                    .set(VmVfNicVO_.pciDeviceUuid, null).update();
            SQL.New(VmVdpaNicVO.class).in(VmVdpaNicVO_.uuid, nicUuids)
                    .set(VmVdpaNicVO_.pciDeviceUuid, null).update();
        }
        SQL.New(EthernetVfPciDeviceVO.class)
                .eq(EthernetVfPciDeviceVO_.vmInstanceUuid, inv.getUuid())
                .notEq(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Releasing)
                .set(EthernetVfPciDeviceVO_.vmInstanceUuid, null)
                .set(EthernetVfPciDeviceVO_.vmUuid, null)
                .set(EthernetVfPciDeviceVO_.l3NetworkUuid, null)
                .set(EthernetVfPciDeviceVO_.status, PciDeviceStatus.Active)
                .set(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Available).update();
        logger.debug(String.format("release vf pci of vm[uuid:%s, name:%s]",
                inv.getUuid(), inv.getName()));
    }

    public void releaseVfDevice(VmNicInventory nic) {
        String pciDeviceUuid;
        if (nic.getType().equals(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE)) {
            VmVfNicVO vfNicVO = dbf.findByUuid(nic.getUuid(), VmVfNicVO.class);
            pciDeviceUuid = vfNicVO.getPciDeviceUuid();

            SQL.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, nic.getUuid())
                    .set(VmVfNicVO_.pciDeviceUuid, null).update();
        } else if (nic.getType().equals(VmVdpaNicConstant.VIRTIO_DATA_PATH_ACCEL_TYPE)) {
            VmVdpaNicVO vdpaNicVO = dbf.findByUuid(nic.getUuid(), VmVdpaNicVO.class);
            pciDeviceUuid = vdpaNicVO.getPciDeviceUuid();

            SQL.New(VmVdpaNicVO.class).eq(VmVdpaNicVO_.uuid, nic.getUuid())
                    .set(VmVdpaNicVO_.pciDeviceUuid, null).update();
        } else {
            return;
        }

        SQL.New(EthernetVfPciDeviceVO.class).eq(EthernetVfPciDeviceVO_.uuid, pciDeviceUuid)
                .notEq(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Releasing)
                .set(EthernetVfPciDeviceVO_.vmInstanceUuid, null)
                .set(EthernetVfPciDeviceVO_.vmUuid, null)
                .set(EthernetVfPciDeviceVO_.l3NetworkUuid, null)
                .set(EthernetVfPciDeviceVO_.status, PciDeviceStatus.Active)
                .set(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Available).update();
        logger.debug(String.format("release vf pci of vnic[vmUuid:%s, l3NetworkUuid:%s]",
                nic.getVmInstanceUuid(), nic.getL3NetworkUuid()));
    }

    public void pauseVfPciDevice(PciDeviceInventory inv) {
        SQL.New(EthernetVfPciDeviceVO.class).eq(EthernetVfPciDeviceVO_.uuid, inv.getUuid())
                .eq(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Attached)
                .set(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Releasing).update();
    }

    public void restoreVfPciDevice(VmNicInventory nic) {
        SQL.New(EthernetVfPciDeviceVO.class).eq(EthernetVfPciDeviceVO_.vmInstanceUuid, nic.getVmInstanceUuid())
                .eq(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Releasing)
                .eq(EthernetVfPciDeviceVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                .set(EthernetVfPciDeviceVO_.status, PciDeviceStatus.Attached)
                .set(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Attached).update();
    }

    public void releaseToBeReleasedVfPciDevice(VmNicInventory nic) {
        SQL.New(EthernetVfPciDeviceVO.class).eq(EthernetVfPciDeviceVO_.vmInstanceUuid, nic.getVmInstanceUuid())
                .eq(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Releasing)
                .eq(EthernetVfPciDeviceVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                .set(EthernetVfPciDeviceVO_.vmInstanceUuid, null)
                .set(EthernetVfPciDeviceVO_.vmUuid, null)
                .set(EthernetVfPciDeviceVO_.l3NetworkUuid, null)
                .set(EthernetVfPciDeviceVO_.status, PciDeviceStatus.Active)
                .set(EthernetVfPciDeviceVO_.vfStatus, EthernetVfStatus.Available).update();
        logger.debug(String.format("release vf pci of vnic[vmUuid:%s, l3NetworkUuid:%s]",
                nic.getVmInstanceUuid(), nic.getL3NetworkUuid()));
    }

    public List<String> getL3UuidsFromVmNicParams(List<VmNicParam> params) {
        return params.stream().filter(VmNicParam::isSriovEnabled)
                .map(VmNicParam::getL3NetworkUuid).collect(Collectors.toList());
    }

    public List<String> getHostUuidsFromVmNicParams(List<VmNicParam> params) {
        List<String> interfaceUuids = params.stream().filter(VmNicParam::isSriovEnabled)
                .map(VmNicParam::getVfParentUuid)
                .filter(Objects::nonNull).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(interfaceUuids)) {
            return new ArrayList<>();
        }

        return Q.New(HostNetworkInterfaceVO.class).in(HostNetworkInterfaceVO_.uuid, interfaceUuids)
                .select(HostNetworkInterfaceVO_.hostUuid).listValues();
    }

    public Map<String, String> getL3PciDeviceMapFromVmNicParams(List<VmNicParam> params) {
        Map<String, String> map = new HashMap<>();
        for (VmNicParam param : params) {
            if (param.isSriovEnabled() && param.getVfParentUuid() != null) {
                String pciDeviceUuid = SQL.New("select pci.uuid from HostNetworkInterfaceVO if, PciDeviceVO pci" +
                                " where if.uuid = :interfaceUuid" +
                                " and if.pciDeviceAddress = pci.pciDeviceAddress" +
                                " and if.hostUuid = pci.hostUuid", String.class)
                        .param("interfaceUuid", param.getVfParentUuid())
                        .limit(1)
                        .find();

                map.put(param.getL3NetworkUuid(), pciDeviceUuid);
            }
        }

        return map;
    }

    public List<String> getL3UuidsNeedVdpa(List<String> l3Uuids) {
        List<String> ret = new ArrayList<>();
        for (String l3Uuid : l3Uuids) {
            String l2Uuid = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.uuid, l3Uuid)
                    .select(L3NetworkVO_.l2NetworkUuid).findValue();
            L2NetworkVO l2NetworkVO = dbf.findByUuid(l2Uuid, L2NetworkVO.class);
            if (l2NetworkVO.getvSwitchType().equals(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK)
                    && !NetworkServiceGlobalConfig.ENABLE_VHOSTUSER.value(Boolean.class)) {
                ret.add(l3Uuid);
            }
        }

        return ret;
    }

    public List<String> getL3UuidsWithVfNic(String vmUuid) {
        List<String> ret = new ArrayList<>();
        VmInstanceVO vmVo = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        for (VmNicVO nic : vmVo.getVmNics()) {
            VmNicType nicType = VmNicType.valueOf(nic.getType());
            if (nicType.isUseSRIOV()) {
                ret.add(nic.getL3NetworkUuid());
            }
        }

        return ret;
    }

    public boolean isSriovEnabledOnL2Network(String l2Uuid) {
        return SriovSystemTags.L2_ENABLE_SRIOV.hasTag(l2Uuid);
    }


}
