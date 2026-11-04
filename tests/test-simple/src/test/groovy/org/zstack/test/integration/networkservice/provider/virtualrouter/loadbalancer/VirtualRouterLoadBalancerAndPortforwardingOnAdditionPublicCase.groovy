package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.core.db.Q
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_
import org.zstack.network.service.vip.VipNetworkServicesRefVO
import org.zstack.network.service.vip.VipNetworkServicesRefVO_
import org.zstack.network.service.vip.VipPeerL3NetworkRefVO
import org.zstack.network.service.vip.VipPeerL3NetworkRefVO_
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.VipUseForList
/**
 * @author: zhanyong.miao
 * @date: 2019-12-19
 * */
class VirtualRouterLoadBalancerAndPortforwardingOnAdditionPublicCase extends SubCase {

    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnServicesEnv()
    }

    @Override
    void test() {
        env.create {
            VipInventory vip = createVipFromAdditionPublicNetwork()
            testGetCandidateVmNicsForLoadBalancer(vip)
            testGetCandidateVmNicsForPortforwding(vip)
        }
    }

    VipInventory createVipFromAdditionPublicNetwork() {
        def l2 = env.inventoryByName("l2") as L2NetworkInventory

        L3NetworkInventory l3_1 = createL3Network {
            delegate.category = "Public"
            delegate.l2NetworkUuid = l2.uuid
            delegate.name = "pubL3-2"
        }

        addIpRange {
            delegate.name = "TestIpRange"
            delegate.l3NetworkUuid = l3_1.uuid
            delegate.startIp = "11.168.200.10"
            delegate.endIp = "11.168.200.253"
            delegate.gateway = "11.168.200.1"
            delegate.netmask = "255.255.255.0"
        }

        VirtualRouterVmVO vr = Q.New(VirtualRouterVmVO.class).find()
        assert vr != null

        attachL3NetworkToVm {
            delegate.l3NetworkUuid = l3_1.getUuid()
            delegate.vmInstanceUuid = vr.uuid
        }

        return createVip {
            name = "vip-1"
            l3NetworkUuid = l3_1.uuid
        }
    }

    void testGetCandidateVmNicsForLoadBalancer(VipInventory vip) {
        /* create loadbalancer on this vip */
        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-lb"
            vipUuid = vip.getUuid()
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.LB_NETWORK_SERVICE_TYPE
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 0

        def listener = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            name = "listener"
            instancePort = 22
            loadBalancerPort = 22
            protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
        } as LoadBalancerListenerInventory

        def result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        } as List<VmNicInventory>
        assert result.size() == 1

        addVmNicToLoadBalancer {
            vmNicUuids = [result.get(0).uuid]
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 1

        result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        }
        assert result.size() == 0

        deleteLoadBalancer {
            uuid = lb.uuid
        }
    }

    void testGetCandidateVmNicsForPortforwding(VipInventory vip) {
        /* create portforwarding on this vip */
        PortForwardingRuleInventory pf =  createPortForwardingRule {
            vipUuid = vip.uuid
            vipPortStart = 21L
            protocolType = "UDP"
            name = "port"
        }

        assert Q.New(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.vipUuid, vip.uuid).isExists()

        def result = getPortForwardingAttachableVmNics {
            ruleUuid = pf.uuid
        } as List<VmNicInventory>

        assert result.size() == 1

        attachPortForwardingRule {
            ruleUuid = pf.uuid
            vmNicUuid  = result[0].uuid
        }

        result = getPortForwardingAttachableVmNics {
            ruleUuid = pf.uuid
        }
        assert result.size() == 0

        deletePortForwardingRule {
            uuid = pf.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
