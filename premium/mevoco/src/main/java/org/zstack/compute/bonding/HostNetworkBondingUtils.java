package org.zstack.compute.bonding;

import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.NetworkInterfaceType;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vtep.VtepVO_;
import org.zstack.pciDevice.PciDeviceUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

public class HostNetworkBondingUtils {
    public static ErrorCode validateDeleteBonding(HostNetworkBondingVO bondingVO) {
        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, bondingVO.getHostUuid()).find();
        List<VtepVO> vtepVOS = Q.New(VtepVO.class).eq(VtepVO_.hostUuid, bondingVO.getHostUuid()).list();

        if (bondingVO.getIpAddresses() != null) {
            List<String> ipAddresses = Arrays.asList(bondingVO.getIpAddresses().split(","));
            String[] ipNetmaskArray = ipAddresses.get(0).split("/");
            //The bonding corresponding to the management network ip does not support deleting
            if (ipNetmaskArray[0].equals(hostVO.getManagementIp()) || ipNetmaskArray[0].equals(bondingVO.getCallBackIp())) {
                return argerr("cannot delete bonding corresponding to the management network");
            }
            //The interface configured with VTEP IP does not support deleting
            for (VtepVO vtepVO : vtepVOS) {
                if (!vtepVOS.isEmpty() && ipNetmaskArray[0].equals(vtepVO.getVtepIp())) {
                    return argerr("cannot delete bonding configured with vtep ip");
                }
            }
        }

        return null;
    }

    public static ErrorCode validateBondingModeAndPolicy(String mode, String xmitHashPolicy) {
        if (HostNetworkBondingConstant.BONDING_MODE_AB.equals(mode)) {
            if (xmitHashPolicy != null) {
                return argerr("cannot assign xmit_hash_policy [%s] for mode [%s]," +
                        " because only mode 802.3ad support specifying different xmit_hash_policys", xmitHashPolicy, mode);
            }
        } else if (xmitHashPolicy == null) {
            return argerr("xmit_hash_policy for mode [%s] should not be null", mode);
        }

        return null;
    }

    public static List<String> getSlaveUuidsBySlaveNamesOnHosts(List<String> slaveNames, List<String> hostUuids) {
        List<String> slaveUuids = new ArrayList<>();
        List<HostNetworkInterfaceVO> interfaceVOs = Q.New(HostNetworkInterfaceVO.class)
                .in(HostNetworkInterfaceVO_.interfaceName, slaveNames)
                .in(HostNetworkInterfaceVO_.hostUuid, hostUuids)
                .list();

        if (interfaceVOs.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("interface in slaveNames[%s] does not exist on the hosts", slaveNames));
        }

        slaveNames.forEach(slaveName -> {
            hostUuids.forEach(hostUuid -> {
                String slaveUuid = interfaceVOs.stream()
                        .filter(vo -> vo.getInterfaceName().equals(slaveName) && vo.getHostUuid().equals(hostUuid))
                        .map(HostNetworkInterfaceVO::getUuid)
                        .findFirst().orElse(null);
                if (slaveUuid == null) {
                    throw new ApiMessageInterceptionException(argerr("there is no interface[%s] on host[uuid:%s]"
                            , slaveName, hostUuid));
                }

                slaveUuids.add(slaveUuid);
            });
        });

        return slaveUuids;
    }

    public static List<String> getSlaveUuidsBySlaveNamesOnHosts(List<String> slaveNames, String hostUuid) {
        return getSlaveUuidsBySlaveNamesOnHosts(slaveNames, Collections.singletonList(hostUuid));
    }

    public static Map<String, List<String>> checkAndGetBondingSlavesMapOnHosts(List<String> slaveUuids, List<String> hostUuids) {
        Map<String, List<String>> slavesMap = new HashMap<>();
        for (String hostUuid : hostUuids) {
            List<String> slaves = HostNetworkBondingUtils.checkAndGetBondingSlavesOnHost(slaveUuids, hostUuid, null);
            slavesMap.put(hostUuid, slaves);
        }

        return slavesMap;
    }

    public static List<String> checkAndGetBondingSlavesOnHost(List<String> slaveUuids, String hostUuid, String bondingUuid) {
        if (slaveUuids.isEmpty()) {
            return slaveUuids;
        }

        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).find();
        List<VtepVO> vtepVOS = Q.New(VtepVO.class).eq(VtepVO_.hostUuid, hostUuid).list();
        Set<HostNetworkInterfaceVO> slaves = new HashSet<>();
        List<HostNetworkInterfaceVO> interfaceVOs = Q.New(HostNetworkInterfaceVO.class)
                .in(HostNetworkInterfaceVO_.uuid, slaveUuids)
                .eq(HostNetworkInterfaceVO_.hostUuid, hostUuid)
                .list();

        if (interfaceVOs.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("there is no slave interface on the host[uuid:%s]", hostUuid));
        }

        //Non-local host NICs do not support binding as slave
        if (bondingUuid != null && interfaceVOs.size() != slaveUuids.size()) {
            throw new ApiMessageInterceptionException(argerr("can not have interfaces in a bond which is not on the same host[%s].", hostUuid));
        }

        for (HostNetworkInterfaceVO interfaceVO : interfaceVOs) {
            if (interfaceVO.getIpAddresses() != null) {
                List<String> ipAddresses = Arrays.asList(interfaceVO.getIpAddresses().split(","));
                String[] ipNetmaskArray = ipAddresses.get(0).split("/");
                //The interface corresponding to the management network ip does not support binding
                if (ipNetmaskArray[0].equals(hostVO.getManagementIp()) || ipNetmaskArray[0].equals(interfaceVO.getCallBackIp())) {
                    throw new ApiMessageInterceptionException(argerr("cannot bind with interface corresponding to the management network."));
                }
                //The interface configured with VTEP IP does not support binding
                for (VtepVO vtepVO : vtepVOS) {
                    if (!vtepVOS.isEmpty() && ipNetmaskArray[0].equals(vtepVO.getVtepIp())) {
                        throw new ApiMessageInterceptionException(argerr("cannot bind with interface configured with vtep ip"));
                    }
                }
            }

            //The bound network card does not support binding as a slave except own slave
            if (interfaceVO.getBondingUuid() != null && !interfaceVO.getBondingUuid().equals(bondingUuid)) {
                throw new ApiMessageInterceptionException(argerr("bonding card can not have occupied interfaces," +
                        " which was already been used by bonding[uuid:%s]", bondingUuid));
            }

            //The network card that has been used as a network bridge does not support binding as a slave
            if (interfaceVO.getInterfaceType() != null && interfaceVO.getInterfaceType().equals(NetworkInterfaceType.bridgeSlave.toString())) {
                throw new ApiMessageInterceptionException(argerr("bonding card can not have interfaces that has been used as a network bridge," +
                        " which was already been used by host[%s]", hostUuid));
            }

            if (PciDeviceUtils.checkIfPciDevicePassThroughStateIsEnabled(interfaceVO.getInterfaceName(), hostUuid)) {
                throw new ApiMessageInterceptionException(argerr("bonding card can not have interfaces that has been pass-through"));
            }
            slaves.add(interfaceVO);
        }

        //Restrictions on the speed of the network card
        int speedAmount = (int) slaves.stream().map(HostNetworkInterfaceVO::getSpeed).distinct().filter(speed -> speed > 0).count();
        if (speedAmount > 1) {
            throw new ApiMessageInterceptionException(argerr("bonding card can not have interfaces with different speed, which is on the host[%s]", hostUuid));
        }

        return interfaceVOs.stream().map(HostNetworkInterfaceVO::getUuid).collect(Collectors.toList());
    }

    public static void checkBondingSlavesAmountWithCertainMode(Integer size, String mode) {
        if (size > 8 || size < 1) {
            throw new ApiMessageInterceptionException(argerr("bonding card can not have [%s] interfaces," +
                    "it must be the number between[1~8]", size));
        }
    }
}
