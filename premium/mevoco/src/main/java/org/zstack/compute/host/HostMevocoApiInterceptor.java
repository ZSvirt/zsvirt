package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vtep.VtepVO_;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

/**
 * Created by MaJin on 2019/12/17.
 */
public class HostMevocoApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIPowerOffHostMsg) {
            validate((APIPowerOffHostMsg) msg);
        } else if (msg instanceof APISetIpOnHostNetworkInterfaceMsg) {
            validate((APISetIpOnHostNetworkInterfaceMsg) msg);
        } else if (msg instanceof APISetIpOnHostNetworkBondingMsg) {
            validate((APISetIpOnHostNetworkBondingMsg) msg);
        } else if (msg instanceof APISetServiceTypeOnHostNetworkInterfaceMsg) {
            validate((APISetServiceTypeOnHostNetworkInterfaceMsg) msg);
        } else if (msg instanceof APISetServiceTypeOnHostNetworkBondingMsg) {
            validate((APISetServiceTypeOnHostNetworkBondingMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(
                APIAddKVMHostFromConfigFileMsg.class,
                APICheckKVMHostConfigFileMsg.class,
                APIIdentifyHostMsg.class,
                APILocateHostNetworkInterfaceMsg.class,
                APIGetHostNetworkFactsMsg.class,
                APIGetClusterHostNetworkFactsMsg.class,
                APIGetCandidateNetworkBondingsMsg.class,
                APIGetCandidateNetworkInterfacesMsg.class,
                APIGetCandidateInterfaceVlanIdsMsg.class,
                APIPowerOffHostMsg.class,
                APIChangeHostPasswordMsg.class,
                APIQueryHostNetworkBondingMsg.class,
                APIQueryHostNetworkInterfaceMsg.class,
                APIGetHostPhysicalMemoryFactsMsg.class,
                APIQueryHostPhysicalMemoryMsg.class,
                APIQueryHostPhysicalCpuMsg.class,
                APIGetHostNUMATopologyMsg.class,
                APIGetHostResourceAllocationMsg.class,
                APISetIpOnHostNetworkInterfaceMsg.class,
                APISetIpOnHostNetworkBondingMsg.class,
                APIUpdateHostNetworkInterfaceMsg.class,
                APISetServiceTypeOnHostNetworkBondingMsg.class,
                APISetServiceTypeOnHostNetworkInterfaceMsg.class,
                APIGetInterfaceServiceTypeStatisticMsg.class,
                APIAllocateHostResourceMsg.class,
                APIUpdateHostIscsiInitiatorNameMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof HostMessage) {
            HostMessage hmsg = (HostMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
        }
    }

    private void validate(APIPowerOffHostMsg msg) {
        List<Tuple> nameips = Q.New(HostVO.class).select(HostVO_.name, HostVO_.managementIp)
                .in(HostVO_.uuid, msg.getHostUuids())
                .notEq(HostVO_.status, HostStatus.Connected)
                .listTuple();

        if (!nameips.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("host(s) [%s] is not Connected, not support to power off",
                    nameips.stream().map(it -> it.get(1, String.class) + "/" + it.get(0, String.class))
                            .collect(Collectors.joining(", "))));
        }
    }

    private void checkNetworkConfig(String ipAddress, String netmask) {
        if (ipAddress != null && !NetworkUtils.isIpv4Address(ipAddress)) {
            throw new ApiMessageInterceptionException(argerr("invalid ip address format[%s]", ipAddress));
        }

        if (netmask != null && !NetworkUtils.isNetmask(netmask)) {
            throw new ApiMessageInterceptionException(argerr("invalid netmask format[%s]", netmask));
        }

        if (ipAddress != null && netmask == null) {
            throw new ApiMessageInterceptionException(argerr("invalid ip set, it must be set with netmask"));
        }
    }

    private void validate(APISetIpOnHostNetworkInterfaceMsg msg) {
        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();
        List <VtepVO> vtepVOS = Q.New(VtepVO.class).eq(VtepVO_.hostUuid, msg.getHostUuid()).list();
        HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceVO.class);

        if (interfaceVO != null && interfaceVO.getIpAddresses() != null) {
            List<String> ipAddresses = Arrays.asList(interfaceVO.getIpAddresses().split(","));
            String[] ipNetmaskArray = ipAddresses.get(0).split("/");
            // The interface corresponding to the management network ip does not support modification
            if (ipNetmaskArray[0].equals(hostVO.getManagementIp()) || ipNetmaskArray[0].equals(interfaceVO.getCallBackIp())) {
                if (msg.getIpAddress() != null || msg.getNetmask() != null) {
                    throw new ApiMessageInterceptionException(argerr("cannot set ip on interface corresponding to the management network"));
                }
            }
            //The interface configured with VTEP IP does not support modification
            for (VtepVO vtepVO : vtepVOS) {
                if (vtepVO != null && ipNetmaskArray[0].equals(vtepVO.getVtepIp())) {
                    if (msg.getIpAddress() != null || msg.getNetmask() != null) {
                        throw new ApiMessageInterceptionException(argerr("cannot bind with interface configured with vtep ip"));
                    }
                }
            }
        }

        // cannot configure the same ipAddress on the same host
        List<String> sameHostInterfaceIpAddresses = Q.New(HostNetworkInterfaceVO.class).select(HostNetworkInterfaceVO_.ipAddresses)
                .eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid()).notEq(HostNetworkInterfaceVO_.uuid, msg.getInterfaceUuid()).listValues();
        List<String> sameHostBondingIpAddresses = Q.New(HostNetworkBondingVO.class).select(HostNetworkBondingVO_.ipAddresses)
                .eq(HostNetworkBondingVO_.hostUuid, msg.getHostUuid()).listValues();
        sameHostInterfaceIpAddresses.addAll(sameHostBondingIpAddresses);
        for (String ipAddresses : sameHostInterfaceIpAddresses) {
            if (ipAddresses != null) {
                List<String> addresses = Arrays.asList(ipAddresses.split(","));
                String[] ipNetmaskArray = addresses.get(0).split("/");
                if (ipNetmaskArray[0].equals(msg.getIpAddress())) {
                    throw new ApiMessageInterceptionException(argerr("cannot set ip which has been set on the other interfaces"));
                }
            }
        }

        //The interface added to the bond does not support modifying the ip
        if (interfaceVO != null && interfaceVO.getBondingUuid() != null) {
            if (interfaceVO.getIpAddresses() != null) {
                List<String> ipAddresses = Arrays.asList(interfaceVO.getIpAddresses().split(","));
                String[] ipNetmaskArray = ipAddresses.get(0).split("/");
                if (!Objects.equals(msg.getIpAddress(), ipNetmaskArray[0]) || !Objects.equals(NetworkUtils.convertNetmask(Integer.parseInt(ipNetmaskArray[1])), msg.getNetmask())) {
                    throw new ApiMessageInterceptionException(argerr("cannot set ip on bonding slaves"));
                }
            } else {
                if (msg.getIpAddress() != null) {
                    throw new ApiMessageInterceptionException(argerr("cannot set ip on bonding slaves"));
                }
            }
        }

        //The interface added to the bridge does not support modifying the ip
        if (interfaceVO != null && NetworkInterfaceType.bridgeSlave.toString().equals(interfaceVO.getInterfaceType())) {
            if (interfaceVO.getIpAddresses() != null) {
                List<String> ipAddresses = Arrays.asList(interfaceVO.getIpAddresses().split(","));
                String[] ipNetmaskArray = ipAddresses.get(0).split("/");
                if (!Objects.equals(msg.getIpAddress(), ipNetmaskArray[0]) || !Objects.equals(NetworkUtils.convertNetmask(Integer.parseInt(ipNetmaskArray[1])), msg.getNetmask())) {
                    throw new ApiMessageInterceptionException(argerr("cannot set ip on bridge slaves"));
                }
            } else {
                if (msg.getIpAddress() != null) {
                    throw new ApiMessageInterceptionException(argerr("cannot set ip on bridge slaves"));
                }
            }
        }

        if (!Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.uuid, msg.getInterfaceUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("invalid interface uuid", msg.getInterfaceUuid()));
        }

        checkNetworkConfig(msg.getIpAddress(), msg.getNetmask());
    }

    private void validate(APISetIpOnHostNetworkBondingMsg msg) {
        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();
        List <VtepVO> vtepVOS = Q.New(VtepVO.class).eq(VtepVO_.hostUuid, msg.getHostUuid()).list();
        HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class);

        if (bondingVO != null && bondingVO.getIpAddresses() != null) {
            List<String> ipAddresses = Arrays.asList(bondingVO.getIpAddresses().split(","));
            String[] ipNetmaskArray = ipAddresses.get(0).split("/");
            // The interface corresponding to the management network ip does not support modification
            if (ipNetmaskArray[0].equals(hostVO.getManagementIp()) || ipNetmaskArray[0].equals(bondingVO.getCallBackIp())) {
                if (msg.getIpAddress() != null || msg.getNetmask() != null) {
                    throw new ApiMessageInterceptionException(argerr("cannot set ip on bonding corresponding to the management network"));
                }
            }
            //The interface configured with VTEP IP does not support modification
            for (VtepVO vtepVO : vtepVOS) {
                if (vtepVO != null && ipNetmaskArray[0].equals(vtepVO.getVtepIp())) {
                    if (msg.getIpAddress() != null || msg.getNetmask() != null) {
                        throw new ApiMessageInterceptionException(argerr("cannot bind with interface configured with vtep ip"));
                    }
                }
            }
        }

        // cannot configure the same ipAddress on the same host
        List<String> sameHostInterfaceIpAddresses = Q.New(HostNetworkInterfaceVO.class).select(HostNetworkInterfaceVO_.ipAddresses)
                .eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid()).listValues();
        List<String> sameHostBondingIpAddresses = Q.New(HostNetworkBondingVO.class).select(HostNetworkBondingVO_.ipAddresses)
                .eq(HostNetworkBondingVO_.hostUuid, msg.getHostUuid()).notEq(HostNetworkInterfaceVO_.uuid, msg.getBondingUuid()).listValues();
        sameHostInterfaceIpAddresses.addAll(sameHostBondingIpAddresses);
        for (String ipAddresses : sameHostInterfaceIpAddresses) {
            if (ipAddresses != null) {
                List<String> addresses = Arrays.asList(ipAddresses.split(","));
                String[] ipNetmaskArray = addresses.get(0).split("/");
                if (ipNetmaskArray[0].equals(msg.getIpAddress())) {
                    throw new ApiMessageInterceptionException(argerr("cannot set ip which has been set on the other interfaces"));
                }
            }
        }

        //The interface added to the bridge does not support modifying the ip
        if (bondingVO != null && NetworkInterfaceType.bridgeSlave.toString().equals(bondingVO.getBondingType())) {
            if (bondingVO.getIpAddresses() != null) {
                List<String> ipAddresses = Arrays.asList(bondingVO.getIpAddresses().split(","));
                String[] ipNetmaskArray = ipAddresses.get(0).split("/");
                if (!Objects.equals(msg.getIpAddress(), ipNetmaskArray[0]) || !Objects.equals(NetworkUtils.convertNetmask(Integer.parseInt(ipNetmaskArray[1])), msg.getNetmask())) {
                    throw new ApiMessageInterceptionException(argerr("cannot set ip on bridge slaves"));
                }
            } else {
                if (msg.getIpAddress() != null) {
                    throw new ApiMessageInterceptionException(argerr("cannot set ip on bridge slaves"));
                }
            }
        }

        if (!Q.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.uuid, msg.getBondingUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("invalid bonding uuid", msg.getBondingUuid()));
        }

        checkNetworkConfig(msg.getIpAddress(), msg.getNetmask());
    }

    private void validate(APISetServiceTypeOnHostNetworkInterfaceMsg msg) {
        if (msg.getVlanIds() == null || msg.getVlanIds().isEmpty()) {
            msg.setVlanIds(Collections.singletonList(0));
        }
        for (String interfaceUuid : msg.getInterfaceUuids()) {
            for (Integer vlanId : msg.getVlanIds()) {
                List<HostNetworkInterfaceServiceType> serviceTypes = Q.New(HostNetworkInterfaceServiceRefVO.class).select(HostNetworkInterfaceServiceRefVO_.serviceType)
                        .eq(HostNetworkInterfaceServiceRefVO_.interfaceUuid, interfaceUuid)
                        .eq(HostNetworkInterfaceServiceRefVO_.vlanId, vlanId)
                        .listValues();
                if (serviceTypes.contains(HostNetworkInterfaceServiceType.ManagementNetwork) && (msg.getServiceTypes() == null || (msg.getServiceTypes() != null && !msg.getServiceTypes().contains(HostNetworkInterfaceServiceType.ManagementNetwork.toString())))) {
                    throw new ApiMessageInterceptionException((argerr("can not detach management network on interface, because management is the automatic acquisition type")));
                }
                if (!serviceTypes.contains(HostNetworkInterfaceServiceType.ManagementNetwork) && msg.getServiceTypes() != null && msg.getServiceTypes().contains(HostNetworkInterfaceServiceType.ManagementNetwork.toString())) {
                    throw new ApiMessageInterceptionException((argerr("can not set management network on interface, because management is the automatic acquisition type")));
                }
            }
        }
    }

    private void validate(APISetServiceTypeOnHostNetworkBondingMsg msg) {
        if (msg.getVlanIds() == null || msg.getVlanIds().isEmpty()) {
            msg.setVlanIds(Collections.singletonList(0));
        }
        for (String bondingUuid : msg.getBondingUuids()) {
            for (Integer vlanId : msg.getVlanIds()) {
                List<HostNetworkInterfaceServiceType> serviceTypes = Q.New(HostNetworkBondingServiceRefVO.class).select(HostNetworkBondingServiceRefVO_.serviceType)
                        .eq(HostNetworkBondingServiceRefVO_.bondingUuid, bondingUuid)
                        .eq(HostNetworkBondingServiceRefVO_.vlanId, vlanId)
                        .listValues();
                if (serviceTypes.contains(HostNetworkInterfaceServiceType.ManagementNetwork) && (msg.getServiceTypes() == null || (msg.getServiceTypes() != null && !msg.getServiceTypes().contains(HostNetworkInterfaceServiceType.ManagementNetwork.toString())))) {
                    throw new ApiMessageInterceptionException((argerr("can not detach management network on bonding, because management is the automatic acquisition type")));
                }
                if (!serviceTypes.contains(HostNetworkInterfaceServiceType.ManagementNetwork) && msg.getServiceTypes() != null && msg.getServiceTypes().contains(HostNetworkInterfaceServiceType.ManagementNetwork.toString())) {
                    throw new ApiMessageInterceptionException((argerr("can not set management network on bonding, because management is the automatic acquisition type")));
                }
            }
        }
    }
}
