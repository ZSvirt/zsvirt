package org.zstack.test.integration.network.vxlanNetwork

import org.apache.commons.collections.list.SynchronizedList
import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkGlobalConfig
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static java.util.Arrays.asList
/**
 * @author: zhanyong.miao
 * @date: 2019-10-23
 * */
class VxlanLazyAttachCase extends SubCase {
    EnvSpec env
    String host1VtepIp = "192.168.100.10"
    String host2VtepIp = "192.168.100.11"

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-novlan")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }
                l2NoVlanNetwork {
                    name = "l2-novlan"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-novlan"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE
                            types = [NetworkServiceType.DHCP.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.99.10"
                            endIp = "192.168.99.249"
                            netmask = "255.255.255.0"
                            gateway = "192.168.99.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testVxlanCreateCase()
            testVxlanAttachNetworkCase()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testVxlanAttachNetworkCase() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster1") as ClusterInventory

        VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.@value = true;
        assert VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.value(boolean .class) == true

        L2VxlanNetworkPoolInventory poolinv = createL2VxlanNetworkPool {
            delegate.name = "TestVxlanPool"
            delegate.zoneUuid = zone.uuid
        }


        createVniRange {
            delegate.startVni = 10
            delegate.endVni = 50
            delegate.l2NetworkUuid = poolinv.uuid
            delegate.name = "TestRange1"
        }

        L2NetworkInventory netinv = createL2VxlanNetwork {
            delegate.poolUuid = poolinv.uuid
            delegate.name = "TestVxlan1"
            delegate.zoneUuid = zone.uuid
        }

        L3NetworkInventory l3 = createL3Network {
            delegate.name = "TestL3Net1"
            delegate.l2NetworkUuid = netinv.uuid
        }

        addIpRange {
            delegate.name = "TestIpRange"
            delegate.l3NetworkUuid = l3.uuid
            delegate.startIp = "192.168.100.2"
            delegate.endIp = "192.168.100.253"
            delegate.gateway = "192.168.100.1"
            delegate.netmask = "255.255.255.0"
        }

        NetworkServiceProviderInventory networkServiceProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0]

        Map<String, List<String>> netServices = new HashMap<>()
        netServices.put(networkServiceProvider.getUuid(), asList("DHCP", "Eip", "Userdata"))

        attachNetworkServiceToL3Network {
            delegate.l3NetworkUuid = l3.uuid
            delegate.networkServices = netServices
        }

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "TestVm0"
            delegate.instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            delegate.imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            delegate.l3NetworkUuids = [(env.specByName("l3-novlan") as L3NetworkSpec).inventory.uuid]
        }

        FlatDhcpBackend.BatchPrepareDhcpCmd dhcpCmd
        env.afterSimulator(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.BatchPrepareDhcpCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchPrepareDhcpCmd.class)

            if (cmd.dhcpInfos.get(0).namespaceName.contains(l3.uuid)) {
                dhcpCmd = cmd
            }

            return rsp
        }

        expect(AssertionError.class) {
            attachL3NetworkToVm {
                delegate.l3NetworkUuid = l3.uuid
                delegate.vmInstanceUuid = vm.uuid
            }
        }

        List<VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd> fdbcmds = new ArrayList<>()
        fdbcmds = Collections.synchronizedList(new ArrayList())
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { rsp, HttpEntity<String> e ->
            VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd cmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd.class)
            fdbcmds.add(cmd)
            return rsp
        }
        attachL2NetworkToCluster {
            delegate.l2NetworkUuid = poolinv.uuid
            delegate.clusterUuid = cluster.uuid
            delegate.systemTags = ["l2NetworkUuid::${poolinv.uuid}::clusterUuid::${cluster.uuid}::cidr::{192.168.100.0/24}".toString()]
        }
        assert fdbcmds.size() == 2
        for (VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd fdbCmd : fdbcmds) {
            assert fdbCmd.peers.size() == 1
        }

        attachL3NetworkToVm {
            delegate.l3NetworkUuid = l3.uuid
            delegate.vmInstanceUuid = vm.uuid
        }

        assert dhcpCmd != null && dhcpCmd.dhcpInfos.get(0).bridgeName != null

        deleteL2Network {
            delegate.uuid = poolinv.getUuid()
        }

    }

    void testVxlanCreateCase() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster1") as ClusterInventory
        def host1 = env.inventoryByName("kvm1") as KVMHostInventory
        def host2 = env.inventoryByName("kvm2") as KVMHostInventory

        VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.@value = false;
        assert VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.value(boolean .class) == false


        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.CreateVxlanBridgeResponse()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.PopulateVxlanFdbResponse()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.CreateVxlanBridgesCmd()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            if (entity.getHeaders().get("X-Resource-UUID")[0] == host1.uuid) {
                resp.vtepIp = host1VtepIp
            } else {
                resp.vtepIp = host2VtepIp
            }
            resp.setSuccess(true)
            return resp
        }

        def pool = createL2VxlanNetworkPool {
            name = "TestVxlanPool1"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkPoolInventory

        attachL2NetworkToCluster {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid
            systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{192.168.0.0/16}".toString()]
        }

        createVniRange {
            startVni = 100
            endVni = 10000
            l2NetworkUuid = pool.uuid
            name = "TestRange1"
        }

        boolean disconnected = true
        KVMAgentCommands.ConnectCmd reConnectCmd = null
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            reConnectCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ConnectCmd.class)
            if ((reConnectCmd.hostUuid == host1.uuid) && disconnected) {
                rsp.success = false
            }
            return rsp
        }

        def realizeRecords = [] as SynchronizedList<String>
        List<VxlanKvmAgentCommands.CreateVxlanBridgeCmd> createBridgeCmds = new ArrayList<>()
        createBridgeCmds = Collections.synchronizedList(new ArrayList())
        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(entity.body, VxlanKvmAgentCommands.CreateVxlanBridgeCmd.class)
            realizeRecords.add(cmd.l2NetworkUuid)
            createBridgeCmds.add(cmd)
            return new VxlanKvmAgentCommands.CreateVxlanBridgesCmd()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(entity.body, VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd.class)
            //realizeRecords.addAll(cmd.networkUuids)
            return new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd()
        }

        expect(AssertionError.class) {
            reconnectHost { uuid = host1.uuid }
        }

        L2VxlanNetworkInventory vxlan1 = createL2VxlanNetwork {
            poolUuid = pool.uuid
            name = "TestVxlan1"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkInventory

        retryInSecs {
            assert realizeRecords.size() == 1
        }

        /* because host is disconnected, so only send create bridge command to host2 */
        retryInSecs {
            assert createBridgeCmds.size() == 1
        }
        for (VxlanKvmAgentCommands.CreateVxlanBridgeCmd cmd : createBridgeCmds) {
            assert cmd.peers.size() == 1
            if (cmd.vtepIp == host1VtepIp) {
                assert cmd.peers.get(0) == host2VtepIp
            } else {
                assert cmd.peers.get(0) == host1VtepIp
            }
        }

        disconnected = false
        realizeRecords.clear()
        reconnectHost { uuid = host1.uuid }

        L2VxlanNetworkInventory vxlan2 = createL2VxlanNetwork {
            poolUuid = pool.uuid
            name = "TestVxlan2"
            zoneUuid = zone.uuid
        }

        retryInSecs {
            assert realizeRecords.size() == 2
        }

        VxlanKvmAgentCommands.CreateVxlanBridgesCmd vxlanCmd = null
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORKS_PATH) { rsp, HttpEntity<String> e ->
            vxlanCmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.CreateVxlanBridgesCmd.class)
            return rsp
        }

        List<VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd> fdbcmds = new ArrayList<>()
        fdbcmds = Collections.synchronizedList(new ArrayList())
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { rsp, HttpEntity<String> e ->
            VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd cmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd.class)
            fdbcmds.add(cmd)
            return rsp
        }
        reconnectHost { uuid = host1.uuid }

        assert fdbcmds.size() == 1
        assert vxlanCmd.bridgeCmds.size() == 2
        for (VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd fdbCmd : fdbcmds) {
            assert fdbCmd.peers.size() == 1
            assert fdbCmd.peers.get(0) == host2VtepIp
        }
        for (VxlanKvmAgentCommands.CreateVxlanBridgeCmd bridgeCmd : vxlanCmd.bridgeCmds) {
            assert bridgeCmd.vni == vxlan2.vni || bridgeCmd.vni == vxlan1.vni
            assert bridgeCmd.mtu == 1450
        }

    }
}