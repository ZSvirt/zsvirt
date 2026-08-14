package org.zstack.network.l2.virtualSwitch;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.host.HostParam;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.l2.virtualSwitch.header.L2VirtualSwitchNetworkVO;
import org.zstack.network.l2.virtualSwitch.header.UplinkGroupType;
import org.zstack.network.l2.virtualSwitch.header.UplinkGroupVO;
import org.zstack.network.l2.virtualSwitch.header.UplinkGroupVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualSwitchHelper {
    @Autowired
    DatabaseFacade dbf;

    private static final CLogger logger = Utils.getLogger(VirtualSwitchHelper.class);

    public void initUplinkGroup(L2VirtualSwitchNetworkVO vSwitch, List<String> hostUuids,
                                String l2ProviderType, List<HostParam> hostParams) {
        if (CollectionUtils.isEmpty(hostUuids)) {
            return;
        }

        List<UplinkGroupVO> vos = new ArrayList<>();
        List<String> attachedHosts = new ArrayList<>(Q.New(UplinkGroupVO.class).select(UplinkGroupVO_.hostUuid)
                .eq(UplinkGroupVO_.l2NetworkUuid, vSwitch.getUuid())
                .in(UplinkGroupVO_.hostUuid, hostUuids)
                .listValues());
        List<HostNetworkBondingVO> bondingVOs = new ArrayList<>(Q.New(HostNetworkBondingVO.class)
                .eq(HostNetworkBondingVO_.bondingName, vSwitch.getPhysicalInterface())
                .in(HostNetworkBondingVO_.hostUuid, hostUuids)
                .list());
        hostUuids.forEach(uuid -> {
            if (attachedHosts.contains(uuid)) {
                return;
            }

            UplinkGroupVO vo = new UplinkGroupVO();
            vo.setL2NetworkUuid(vSwitch.getUuid());
            vo.setHostUuid(uuid);

            HostParam hostParam = hostParams.stream().filter(it -> uuid.equals(it.getHostUuid())).findFirst().orElse(null);
            if (hostParam == null || Objects.equals(hostParam.getPhysicalInterface(), vSwitch.getPhysicalInterface())) {
                vo.setType(UplinkGroupType.Bonding);
                vo.setInterfaceName(vSwitch.getPhysicalInterface());
                vo.setL2ProviderType(l2ProviderType);

                HostNetworkBondingVO bondingVO = bondingVOs.stream().filter(it -> it.getHostUuid().equals(uuid)).findFirst()
                        .orElse(new HostNetworkBondingVO());
                vo.setBondingUuid(bondingVO.getUuid());
            } else {
                String interfaceUuid = Q.New(HostNetworkInterfaceVO.class)
                        .select(HostNetworkInterfaceVO_.uuid)
                        .eq(HostNetworkInterfaceVO_.hostUuid, uuid)
                        .eq(HostNetworkInterfaceVO_.interfaceName, hostParam.getPhysicalInterface())
                        .findValue();

                vo.setType(UplinkGroupType.PhysicalInterface);
                vo.setInterfaceName(hostParam.getPhysicalInterface());
                vo.setInterfaceUuid(interfaceUuid);
                vo.setL2ProviderType(hostParam.getL2ProviderType() == null ? l2ProviderType : hostParam.getL2ProviderType());
            }
            vos.add(vo);
            logger.debug(String.format("init %s", vo));
        });

        if (!vos.isEmpty()) {
            dbf.persistCollection(vos);
        }
    }

    public void initUplinkGroup(L2VirtualSwitchNetworkVO vSwitch, String hostUuid,
                                String l2ProviderType, HostParam hostParam) {
        initUplinkGroup(vSwitch, Collections.singletonList(hostUuid),
                l2ProviderType, Collections.singletonList(hostParam));
    }

    public void initOrOverrideUplinkGroup(L2VirtualSwitchNetworkVO vSwitch, String hostUuid,
                                          String l2ProviderType, HostParam hostParam) {
        UplinkGroupVO vo = Q.New(UplinkGroupVO.class)
                .eq(UplinkGroupVO_.l2NetworkUuid, vSwitch.getUuid())
                .eq(UplinkGroupVO_.hostUuid, hostUuid)
                .find();

        boolean newCreated = vo == null;
        if (newCreated) {
            vo = new UplinkGroupVO();
            vo.setL2NetworkUuid(vSwitch.getUuid());
            vo.setHostUuid(hostUuid);
        }

        if (hostParam == null || hostParam.getHostUuid() == null
                || Objects.equals(hostParam.getPhysicalInterface(), vSwitch.getPhysicalInterface())) {
            vo.setType(UplinkGroupType.Bonding);
            vo.setInterfaceName(vSwitch.getPhysicalInterface());
            vo.setL2ProviderType(l2ProviderType);

            String bondingUuid = Q.New(HostNetworkBondingVO.class)
                    .select(HostNetworkBondingVO_.uuid)
                    .eq(HostNetworkBondingVO_.bondingName, vSwitch.getPhysicalInterface())
                    .eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                    .findValue();
            vo.setBondingUuid(bondingUuid);
        } else {
            String interfaceUuid = Q.New(HostNetworkInterfaceVO.class)
                    .select(HostNetworkInterfaceVO_.uuid)
                    .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                    .eq(HostNetworkInterfaceVO_.interfaceName, hostParam.getPhysicalInterface())
                    .findValue();

            vo.setType(UplinkGroupType.PhysicalInterface);
            vo.setInterfaceName(hostParam.getPhysicalInterface());
            vo.setInterfaceUuid(interfaceUuid);
            vo.setL2ProviderType(hostParam.getL2ProviderType() == null ? l2ProviderType : hostParam.getL2ProviderType());
        }

        if (newCreated) {
            dbf.persistAndRefresh(vo);
            logger.debug(String.format("init %s", vo));
        } else {
            dbf.updateAndRefresh(vo);
            logger.debug(String.format("override %s", vo));
        }
    }

    public void initUplinkGroup(List<L2VirtualSwitchNetworkVO> vSwitches, String hostUuid,
                                Map<String, String> l2ProviderTypeMap) {
        if (CollectionUtils.isEmpty(vSwitches)) {
            return;
        }

        List<UplinkGroupVO> vos = new ArrayList<>();
        List<String> attachedVSwitches = Q.New(UplinkGroupVO.class).select(UplinkGroupVO_.l2NetworkUuid)
                .eq(UplinkGroupVO_.hostUuid, hostUuid)
                .in(UplinkGroupVO_.l2NetworkUuid, vSwitches.stream().map(L2VirtualSwitchNetworkVO::getUuid)
                        .collect(Collectors.toList()))
                .listValues();
        List<HostNetworkBondingVO> bondingVOS = Q.New(HostNetworkBondingVO.class)
                .eq(HostNetworkBondingVO_.hostUuid, hostUuid)
                .in(HostNetworkBondingVO_.bondingName, vSwitches.stream().map(L2VirtualSwitchNetworkVO::getPhysicalInterface)
                        .collect(Collectors.toList()))
                .list();
        vSwitches.forEach(vSwitch -> {
            if (attachedVSwitches.contains(vSwitch.getUuid())) {
                return;
            }

            UplinkGroupVO vo = new UplinkGroupVO();
            vo.setL2NetworkUuid(vSwitch.getUuid());
            vo.setHostUuid(hostUuid);
            vo.setL2ProviderType(l2ProviderTypeMap.get(vSwitch.getUuid()));
            vo.setType(UplinkGroupType.Bonding);
            vo.setInterfaceName(vSwitch.getPhysicalInterface());

            HostNetworkBondingVO bondingVO = bondingVOS.stream().filter(it -> it.getBondingName().equals(vSwitch.getPhysicalInterface()))
                    .findFirst().orElse(new HostNetworkBondingVO());
            vo.setBondingUuid(bondingVO.getUuid());
            vos.add(vo);
            logger.debug(String.format("init %s", vo));
        });

        if (!vos.isEmpty()) {
            dbf.persistCollection(vos);
        }
    }
}
