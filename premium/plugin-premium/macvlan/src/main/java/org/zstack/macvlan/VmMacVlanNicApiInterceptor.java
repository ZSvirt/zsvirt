package org.zstack.macvlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmMacVlanNicConstant;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.kvm.KVMConstant;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.argerr;

public class VmMacVlanNicApiInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VmMacVlanNicApiInterceptor.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ResourceConfigFacade rcf;
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachL2NetworkToClusterMsg) {
            validate((APIAttachL2NetworkToClusterMsg) msg);
        } else if (msg instanceof APIAttachNetworkServiceToL3NetworkMsg) {
            validate((APIAttachNetworkServiceToL3NetworkMsg) msg);
        } else if (msg instanceof APICreateL2NetworkMsg) {
            validate((APICreateL2NetworkMsg) msg);
        }
        return msg;
    }

    private void validate(APICreateL2NetworkMsg msg) {
        if (!L2NetworkConstant.VSWITCH_TYPE_MACVLAN.equals(msg.getvSwitchType())) {
            return;
        }

        checkL2NetworkTypeSupportMacVlan(msg.getType());
    }

    private void checkL2NetworkTypeSupportMacVlan(String l2Type) {
        if (!VmMacVlanNicConstant.MACVLAN_L2_NETWORK_TYPES.contains(l2Type)) {
            throw new ApiMessageInterceptionException(argerr("only %s support macvlan", VmMacVlanNicConstant.MACVLAN_L2_NETWORK_TYPES));
        }
    }

    private void validate(APIAttachNetworkServiceToL3NetworkMsg msg) {
        L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();

        String vSwitch = getL2NetworkVswitchType(l3NetworkVO.getL2NetworkUuid());
        if (!L2NetworkConstant.VSWITCH_TYPE_MACVLAN.equals(vSwitch)) {
            return;
        }

        Map<String, List<String>> services = msg.getNetworkServices();
        if (services == null) {
            return;
        }

        if (L3NetworkCategory.Public.equals(l3NetworkVO.getCategory())) {
            throw new ApiMessageInterceptionException(argerr("could not attach network service to l3[uuid:%s], because l2[uuid:%s, vSwitchType:%s] not support any l3 network service for public basic l3network",
                    l3NetworkVO.getUuid(), l3NetworkVO.getL2NetworkUuid(), vSwitch));
        } else if (L3NetworkCategory.Private.equals(l3NetworkVO.getCategory())) {
            if (VpcConstants.VPC_L3_NETWORK_TYPE.equals(l3NetworkVO.getType())) {
                for (String providerUuid : services.keySet()) {
                    boolean exists = Q.New(NetworkServiceProviderVO.class).eq(NetworkServiceProviderVO_.uuid, providerUuid).eq(NetworkServiceProviderVO_.type, VyosConstants.VYOS_ROUTER_PROVIDER_TYPE).isExists();
                    if (!exists) {
                        throw new ApiMessageInterceptionException(argerr("could not attach network service to l3[uuid:%s], because l2[uuid:%s, vSwitchType:%s] only support vrouter network provider for vpc l3network",
                                l3NetworkVO.getUuid(), l3NetworkVO.getL2NetworkUuid(), vSwitch));
                    }
                }

            } else {
                for (String providerUuid : services.keySet()) {
                    boolean exists = Q.New(NetworkServiceProviderVO.class).eq(NetworkServiceProviderVO_.uuid, providerUuid).eq(NetworkServiceProviderVO_.type, VyosConstants.VYOS_ROUTER_PROVIDER_TYPE).isExists();
                    if (!exists) {
                        throw new ApiMessageInterceptionException(argerr("could not attach network service to l3[uuid:%s], because l2[uuid:%s, vSwitchType:%s] only support vrouter network provider for private basic l3network",
                                l3NetworkVO.getUuid(), l3NetworkVO.getL2NetworkUuid(), vSwitch));
                    }
                    if (services.get(providerUuid).size() > 1 || !LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING.equals(services.get(providerUuid).get(0))) {
                        throw new ApiMessageInterceptionException(argerr("could not attach network service to l3[uuid:%s], because l2[uuid:%s, vSwitchType:%s] only support loadbalancer for private basic l3network",
                                l3NetworkVO.getUuid(), l3NetworkVO.getL2NetworkUuid(), vSwitch));
                    }
                }
            }
        }
    }

    private void validate(APIAttachL2NetworkToClusterMsg msg) {
        final String vSwitchType = getL2NetworkVswitchType(msg.getL2NetworkUuid());
        if (!vSwitchType.equals(L2NetworkConstant.VSWITCH_TYPE_MACVLAN)) {
            return;
        }
        checkConflictBetweenDifferentVswitchType(msg.getL2NetworkUuid(), msg.getClusterUuid());
        checkClusterSupportMacVlan(msg.getClusterUuid());
    }

    private void checkClusterSupportMacVlan(String clusterUuid) {
        String type = Q.New(ClusterVO.class).eq(ClusterVO_.uuid, clusterUuid).select(ClusterVO_.hypervisorType).findValue();
        if (type != null && !type.equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "only kvm hosts support l2 which vSwitchType is MacVlan"
            ));
        }
    }

    private String getL2NetworkVswitchType(String l2Uuid) {
        String vSwitchType = Q.New(L2NetworkVO.class)
                .select(L2NetworkVO_.vSwitchType)
                .eq(L2NetworkVO_.uuid, l2Uuid)
                .findValue();

        return vSwitchType;
    }

    private void checkConflictBetweenDifferentVswitchType(String l2Uuid, String clusterUuid) {
        /**
         * l2Networks with same physicalInterface in one cluster
         * can not be different vSwitchType.
         */
        L2NetworkVO l2Vo = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2Uuid).find();

        List<String> l2s = Q.New(L2NetworkClusterRefVO.class)
                .select(L2NetworkClusterRefVO_.l2NetworkUuid)
                .eq(L2NetworkClusterRefVO_.clusterUuid, clusterUuid)
                .listValues();
        List<String> l2Ds = new ArrayList<>();
        if (l2Vo != null){
            l2Ds = Q.New(L2NetworkVO.class)
                    .select(L2NetworkVO_.uuid)
                    .eq(L2NetworkVO_.physicalInterface, l2Vo.getPhysicalInterface())
                    .notEq(L2NetworkVO_.vSwitchType, l2Vo.getvSwitchType())
                    .listValues();
            if (!Collections.disjoint(l2s, l2Ds)) {
                throw new ApiMessageInterceptionException(argerr("can not create %s with physical interface:[%s] which was already been used by another vSwitch type.", l2Vo.getvSwitchType(), l2Vo.getPhysicalInterface()));
            }
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
                APIAttachL2NetworkToClusterMsg.class,
                APIAttachNetworkServiceToL3NetworkMsg.class,
                APICreateL2NetworkMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
