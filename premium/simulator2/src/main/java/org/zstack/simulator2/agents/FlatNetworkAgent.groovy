package org.zstack.simulator2.agents

import org.zstack.kvm.KVMAgentCommands
import org.zstack.network.plugin.FlatGratuitousARPBackend
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatDnsBackend
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.network.service.vipQos.flat.FlatVipQosBackend
import org.zstack.simulator2.Simulator

/**
 * Created by xing5 on 2017/9/19.
 */
class FlatNetworkAgent extends Agent {
    FlatNetworkAgent(Simulator simulator) {
        super(simulator)
    }

    @Override
    void setupAgentHandler() {
        handle(FlatDhcpBackend.APPLY_DHCP_PATH) {
            return new FlatDhcpBackend.ApplyDhcpRsp()
        }

        handle(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) {
            return new FlatDhcpBackend.ApplyDhcpRsp()
        }

        handle(FlatDhcpBackend.PREPARE_DHCP_PATH) {
            return new FlatDhcpBackend.PrepareDhcpRsp()
        }

        handle(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH) {
            return new FlatDhcpBackend.PrepareDhcpRsp()
        }

        handle(FlatDhcpBackend.RESET_DEFAULT_GATEWAY_PATH) {
            return new FlatDhcpBackend.ResetDefaultGatewayRsp()
        }

        handle(FlatDhcpBackend.RELEASE_DHCP_PATH) {
            return new FlatDhcpBackend.ReleaseDhcpRsp()
        }

        handle(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) {
            return new FlatDhcpBackend.DeleteNamespaceRsp()
        }

        handle(FlatDhcpBackend.DHCP_FLUSH_NAMESPACE_PATH) {
            return new FlatDhcpBackend.FlushDhcpNamespaceRsp()
        }

        handle(FlatDhcpBackend.DHCP_CONNECT_PATH) {
            return new FlatDhcpBackend.ConnectRsp()
        }

        handle(FlatEipBackend.APPLY_EIP_PATH) {
            return new FlatNetworkServiceConstant.AgentRsp()
        }

        handle(FlatEipBackend.DELETE_EIP_PATH) {
            return new FlatNetworkServiceConstant.AgentRsp()
        }

        handle(FlatEipBackend.BATCH_APPLY_EIP_PATH) {
            return new FlatNetworkServiceConstant.AgentRsp()
        }

        handle(FlatEipBackend.BATCH_DELETE_EIP_PATH) {
            return new FlatNetworkServiceConstant.AgentRsp()
        }

        handle(FlatUserdataBackend.APPLY_USER_DATA) {
            return new FlatUserdataBackend.ApplyUserdataRsp()
        }

        handle(FlatUserdataBackend.BATCH_APPLY_USER_DATA) {
            return new KVMAgentCommands.AgentResponse()
        }

        handle(FlatUserdataBackend.RELEASE_USER_DATA) {
            return new FlatUserdataBackend.ReleaseUserdataRsp()
        }

        handle(FlatUserdataBackend.CLEANUP_USER_DATA) {
            return new FlatUserdataBackend.CleanupUserdataRsp()
        }

        handle(FlatVipQosBackend.FLAT_SET_VIP_QOS) {
            return new FlatVipQosBackend.SetVipQosRsp()
        }

        handle(FlatVipQosBackend.FLAT_DELETE_VIP_QOS) {
            return new FlatVipQosBackend.DeleteVipQosRsp()
        }

        handle(FlatVipQosBackend.FLAT_DELETE_VIPALL_QOS) {
            return new FlatVipQosBackend.DeleteVipAllQosRsp()
        }

        handle(FlatGratuitousARPBackend.APPLY_GRATUITOUS_ARP) {
            return new FlatGratuitousARPBackend.ApplyGratuitousARPRsp()
        }

        handle(FlatGratuitousARPBackend.RELEASE_GRATUITOUS_ARP) {
            return new FlatGratuitousARPBackend.ReleaseGratuitousARPRsp()
        }

        handle(FlatGratuitousARPBackend.UPDATE_GRATUITOUS_ARP_SETTINGS) {
            return new FlatGratuitousARPBackend.ApplyGratuitousARPRsp()
        }
    }
}
