package org.zstack.pciDevice.ethernet;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.sriov.EthernetVfPciDeviceVO;
import org.zstack.header.sriov.EthernetVfStatus;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecVO;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/8 11:11
 */
public class EthernetFactory implements PciDeviceTypeFactory {

    private static CLogger logger = Utils.getLogger(EthernetFactory.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Override
    public List<PciDeviceType> getTypes() {
        List<PciDeviceType> types = new ArrayList<>();
        types.add(PciDeviceType.Ethernet_Controller);
        return types;
    }

    @Override
    public void createPciDevices(List<PciDeviceVO> vos) {
        /* vos包含了物理网卡pci信息和vf网卡pci信息 */
        if (vos.isEmpty()) {
            return;
        }

        List<PciDeviceVO> physicalNicPcis = vos.stream().filter(vo -> vo.getParentUuid() == null)
                .collect(Collectors.toList());
        if (!physicalNicPcis.isEmpty()) {
            dbf.persistCollection(physicalNicPcis);
        }

        List<PciDeviceVO> vfPcis = vos.stream().filter(vo -> vo.getParentUuid() != null)
                .collect(Collectors.toList());
        if (vfPcis.isEmpty()) {
            return;
        }

        String hostUuid = vfPcis.get(0).getHostUuid();
        List<HostNetworkInterfaceVO> hostInts = Q.New(HostNetworkInterfaceVO.class)
                .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid).list();
        Map<String, String> hostInterfacePciAddressNameMap = hostInts
                .stream().collect(Collectors.toMap(HostNetworkInterfaceVO::getPciDeviceAddress,
                        HostNetworkInterfaceVO::getInterfaceName));

        Map<String, String> parentUuidToNameMap = new HashMap<>();
        List<EthernetVfPciDeviceVO> vfVos = new ArrayList<>();
        for (PciDeviceVO pvo : vfPcis) {
            if (parentUuidToNameMap.get(pvo.getParentUuid()) == null) {
                PciDeviceVO ppvo = dbf.findByUuid(pvo.getParentUuid(), PciDeviceVO.class);
                String name = hostInterfacePciAddressNameMap.get(ppvo.getPciDeviceAddress());
                parentUuidToNameMap.put(pvo.getParentUuid(), name);
            }


            EthernetVfPciDeviceVO vfVO = new EthernetVfPciDeviceVO(pvo);
            vfVO.setHostDevUuid(pvo.getHostUuid());
            vfVO.setUuid(pvo.getUuid());
            vfVO.setVfStatus(EthernetVfStatus.Available);
            vfVO.setInterfaceName(parentUuidToNameMap.get(pvo.getParentUuid()));

            vfVos.add(vfVO);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("add vf pci information: %s", JSONObjectUtil.toJsonString(vfVos)));
        }

        dbf.persistCollection(vfVos);
    }

    @Override
    public void updatePciDevices(List<PciDeviceVO> vos) {
        dbf.updateCollection(vos);
    }

    @Override
    public void removePciDevices(List<PciDeviceTO> tos) {
        List<String> garbageUuids = tos.stream().map(PciDeviceTO::getUuid).collect(Collectors.toList());
        if (!garbageUuids.isEmpty()) {
            SQL.New(PciDeviceVO.class).in(PciDeviceVO_.uuid, garbageUuids).hardDelete();
            SQL.New(MdevDeviceVO.class).in(MdevDeviceVO_.parentUuid, garbageUuids).hardDelete();
            logger.debug(String.format("delete garbage ethernet pci devices[uuids:%s, desc:%s] in host[uuid:%s]",
                    garbageUuids,
                    tos.stream().map(PciDeviceTO::getDescription).collect(Collectors.toList()),
                    tos.get(0).getHostUuid()));
        }
    }
}
