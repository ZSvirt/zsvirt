package org.zstack.testlib

import org.zstack.kvm.KVMAgentCommands
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatDnsBackend
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatUserdataBackend

/**
 * Created by xing5 on 2017/2/26.
 */
class FlatnetworkSimualtor implements Simulator {
    @Override
    void registerSimulators(EnvSpec spec) {
        spec.simulator(FlatDhcpBackend.APPLY_DHCP_PATH) {
            return new FlatDhcpBackend.ApplyDhcpRsp()
        }

        spec.simulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) {
            return new FlatDhcpBackend.ApplyDhcpRsp()
        }

        spec.simulator(FlatDhcpBackend.PREPARE_DHCP_PATH) {
            return new FlatDhcpBackend.PrepareDhcpRsp()
        }

        spec.simulator(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH) {
            return new FlatDhcpBackend.PrepareDhcpRsp()
        }

        spec.simulator(FlatDhcpBackend.RESET_DEFAULT_GATEWAY_PATH) {
            return new FlatDhcpBackend.ResetDefaultGatewayRsp()
        }

        spec.simulator(FlatDhcpBackend.RELEASE_DHCP_PATH) {
            return new FlatDhcpBackend.ReleaseDhcpRsp()
        }

        spec.simulator(FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH) {
            return new FlatDhcpBackend.DeleteNamespaceRsp()
        }

        spec.simulator(FlatDhcpBackend.DHCP_FLUSH_NAMESPACE_PATH) {
            return new FlatDhcpBackend.FlushDhcpNamespaceRsp()
        }

        spec.simulator(FlatDhcpBackend.ARPING_NAMESPACE_PATH) {
            Map<String, List<String>> result = new HashMap<>()
            FlatDhcpBackend.ArpingRsp rsp = new FlatDhcpBackend.ArpingRsp()
            rsp.result = result
            return rsp
        }

        spec.simulator(FlatDhcpBackend.DHCP_CONNECT_PATH) {
            return new FlatDhcpBackend.ConnectRsp()
        }

        spec.simulator(FlatEipBackend.APPLY_EIP_PATH) {
            return new FlatNetworkServiceConstant.AgentRsp()
        }

        spec.simulator(FlatEipBackend.DELETE_EIP_PATH) {
            return new FlatNetworkServiceConstant.AgentRsp()
        }

        spec.simulator(FlatEipBackend.BATCH_APPLY_EIP_PATH) {
            return new FlatNetworkServiceConstant.AgentRsp()
        }

        spec.simulator(FlatEipBackend.BATCH_DELETE_EIP_PATH) {
            return new FlatNetworkServiceConstant.AgentRsp()
        }

        spec.simulator(FlatUserdataBackend.APPLY_USER_DATA) {
            return new FlatUserdataBackend.ApplyUserdataRsp()
        }

        spec.simulator(FlatUserdataBackend.BATCH_APPLY_USER_DATA) {
            return new KVMAgentCommands.AgentResponse()
        }

        spec.simulator(FlatUserdataBackend.RELEASE_USER_DATA) {
            return new FlatUserdataBackend.ReleaseUserdataRsp()
        }

        spec.simulator(FlatUserdataBackend.CLEANUP_USER_DATA) {
            return new FlatUserdataBackend.CleanupUserdataRsp()
        }
    }
}
