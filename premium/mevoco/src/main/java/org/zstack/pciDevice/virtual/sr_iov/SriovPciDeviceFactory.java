package org.zstack.pciDevice.virtual.sr_iov;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.sriov.VmVfNicKvmBackend;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.sriov.EthernetVfPciDeviceVO;
import org.zstack.header.sriov.EthernetVfPciDeviceVO_;
import org.zstack.header.sriov.EthernetVfStatus;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceUpdateExtensionPoint;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.virtual.APIGenerateVirtualPciDevicesMsg;
import org.zstack.pciDevice.virtual.APIUngenerateVirtualPciDevicesMsg;
import org.zstack.pciDevice.virtual.VirtualPciDeviceFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-04-24.
 */
public class SriovPciDeviceFactory implements VirtualPciDeviceFactory, HostNetworkInterfaceUpdateExtensionPoint {
    private static CLogger logger = Utils.getLogger(SriovPciDeviceFactory.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PciDeviceManager pciMgr;
    @Autowired
    protected VmVfNicKvmBackend vfNicKvmBackend;

    @Override
    public String getVirtTechType() {
        return PciDeviceConstants.PCI_VIRT_TECH_SR_IOV;
    }

    @Override
    public void generateVirtualPciDevices(APIGenerateVirtualPciDevicesMsg msg, Completion completion) {
        APIGenerateSriovPciDevicesMsg gmsg = (APIGenerateSriovPciDevicesMsg) msg;
        PciDeviceVO pci = dbf.findByUuid(gmsg.getPciDeviceUuid(), PciDeviceVO.class);
        PciDeviceBackend bkd = pciMgr.getPciDeviceBackendByHostUuid(pci.getHostUuid());
        bkd.generateSriovPciDevices(pci.getHostUuid(), pci.toInventory(), gmsg.getVirtPartNum(), false, new Completion(msg) {
            @Override
            public void success() {
                vfNicKvmBackend.addFdbEntryPhysicalNic(PciDeviceInventory.valueOf(pci));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void ungenerateVirtualPciDevices(APIUngenerateVirtualPciDevicesMsg msg, Completion completion) {
        APIUngenerateSriovPciDevicesMsg umsg = (APIUngenerateSriovPciDevicesMsg) msg;
        PciDeviceVO pci = dbf.findByUuid(umsg.getPciDeviceUuid(), PciDeviceVO.class);
        PciDeviceBackend bkd = pciMgr.getPciDeviceBackendByHostUuid(pci.getHostUuid());
        bkd.ungenerateSriovPciDevices(pci.getHostUuid(), pci.toInventory(), new Completion(msg) {
            @Override
            public void success() {
                vfNicKvmBackend.removeFdbEntryPhysicalNic(PciDeviceInventory.valueOf(pci));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void afterCreated(String hostUuid, List<HostNetworkInterfaceVO> interfaceVOS,
                             List<HostNetworkBondingVO> bondingVOS) {
        if (interfaceVOS.isEmpty()) {
            return;
        }

        Map<String, String> pparentUuidInterfaceNameMap = new HashMap<>();

        for (HostNetworkInterfaceVO vo : interfaceVOS) {
            PciDeviceVO ppci = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.pciDeviceAddress, vo.getPciDeviceAddress())
                    .eq(PciDeviceVO_.type, PciDeviceType.Ethernet_Controller)
                    .eq(PciDeviceVO_.hostUuid, hostUuid).limit(1)
                    .find();
            if (CoreGlobalProperty.UNIT_TEST_ON) {
                if (ppci == null) {
                    continue;
                }
            } else {
                if (ppci == null) {
                    throw new RuntimeException(String.format("physical interface[name:%s, pci address:%s] " +
                            "on host[uuid:%s] can not find pci device",
                            vo.getInterfaceName(), vo.getPciDeviceAddress(), hostUuid));
                }
            }

            pparentUuidInterfaceNameMap.put(ppci.getUuid(), vo.getInterfaceName());
        }

        for (Map.Entry<String, String> e : pparentUuidInterfaceNameMap.entrySet()) {
            String pparentUuid = e.getKey();
            String interfaceName = e.getValue();
            SQL.New(EthernetVfPciDeviceVO.class)
                    .eq(EthernetVfPciDeviceVO_.hostUuid, hostUuid)
                    .eq(EthernetVfPciDeviceVO_.parentUuid, pparentUuid)
                    .set(EthernetVfPciDeviceVO_.interfaceName, interfaceName)
                    .update();
        }
    }
}
