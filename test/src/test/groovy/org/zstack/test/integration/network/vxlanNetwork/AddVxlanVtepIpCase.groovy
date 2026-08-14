package org.zstack.test.integration.network.vxlanNetwork

import org.apache.commons.collections.list.SynchronizedList
import org.springframework.http.HttpEntity
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.stream.Collectors

class AddVxlanVtepIpCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
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
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    attachPrimaryStorage("local")

                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testVxlanVtepIpChanged()
            testCreateVxlanPoll()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testVxlanVtepIpChanged() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster1") as ClusterInventory
        def host1 = env.inventoryByName("kvm1") as KVMHostInventory
        def host2 = env.inventoryByName("kvm2") as KVMHostInventory

        def pool = createL2VxlanNetworkPool {
            name = "TestVxlanPool1"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkPoolInventory

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            resp.setSuccess(true)
            return resp
        }

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

        def vxlan = createL2VxlanNetwork {
            poolUuid = pool.uuid
            name = "TestVxlan1"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkInventory

        def l3 = createL3Network {
            name = "TestL3Net1"
            l2NetworkUuid = vxlan.uuid
        } as L3NetworkInventory

        addIpRange {
            name = "TestIpRange"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.100.2"
            endIp = "192.168.100.253"
            gateway = "192.168.100.1"
            netmask = "255.255.255.0"
        }

        def flatProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0] as NetworkServiceProviderInventory

        def netServices = ["${flatProvider.uuid}":["DHCP", "Eip", "Userdata"]]

        attachNetworkServiceToL3Network {
            delegate.l3NetworkUuid = l3.uuid
            delegate.networkServices = netServices
        }

        List<VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd> cmds = new ArrayList<>()
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { rsp, HttpEntity<String> e ->
            VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd cmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd.class)
            cmds.add(cmd)
            return rsp
        }

        createVxlanVtep {
            hostUuid = host1.uuid
            poolUuid = pool.uuid
            vtepIp = "127.1.0.1"
        }
        assert cmds.size() == 0

        cmds = new ArrayList<>()
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { rsp, HttpEntity<String> e ->
            VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd cmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd.class)
            cmds.add(cmd)
            return rsp
        }
        createVxlanVtep {
            hostUuid = host2.uuid
            poolUuid = pool.uuid
            vtepIp = "127.1.0.2"
        }
        assert cmds != null

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            if (entity.getHeaders().get("X-Resource-UUID")[0] == host1.uuid) {
                resp.vtepIp = "127.1.0.1"
            } else {
                resp.vtepIp = "127.1.0.2"
            }
            resp.setSuccess(true)
            return resp
        }

        VxlanKvmAgentCommands.CheckVxlanCidrCmd ccmd = null
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            ccmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.CheckVxlanCidrCmd.class)
            return rsp
        }
        def vm1 = createVmInstance {
            name = "TestVm1"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = (env.inventoryByName("image1") as ImageInventory).uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
        } as VmInstanceInventory
        assert ccmd != null
        assert ccmd.vtepip == "127.1.0.2" || ccmd.vtepip == "127.1.0.1"

        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            ccmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.CheckVxlanCidrCmd.class)
            return rsp
        }
        reconnectHost { uuid = host1.uuid }
        assert ccmd != null
        assert ccmd.vtepip == "127.1.0.1"

        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            ccmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.CheckVxlanCidrCmd.class)
            return rsp
        }
        reconnectHost { uuid = host2.uuid }
        assert ccmd != null
        assert ccmd.vtepip == "127.1.0.2"

        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            ccmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.CheckVxlanCidrCmd.class)
            return rsp
        }
        rebootVmInstance { uuid = vm1.uuid }
        assert ccmd != null
        assert ccmd.vtepip == "127.1.0.2" || ccmd.vtepip == "127.1.0.1"

        env.cleanAfterSimulatorHandlers()
    }

    void testCreateVxlanPoll() {
        def zone = env.inventoryByName("zone") as ZoneInventory
        def cluster = env.inventoryByName("cluster1") as ClusterInventory
        def host1 = env.inventoryByName("kvm1") as KVMHostInventory
        def host2 = env.inventoryByName("kvm2") as KVMHostInventory

        def pool = createL2VxlanNetworkPool {
            name = "TestVxlanPool2"
            zoneUuid = zone.uuid
        } as L2VxlanNetworkPoolInventory

        createVniRange {
            startVni = 10
            endVni = 20
            l2NetworkUuid = pool.uuid
            name = "TestRange2"
        }

        String vtep1 = "127.1.0.1"
        String vtep2 = "127.1.0.2"
        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            if (entity.getHeaders().get("X-Resource-UUID")[0] == host1.uuid) {
                resp.vtepIp = vtep1
            } else {
                resp.vtepIp = vtep2
            }
            resp.setSuccess(true)
            return resp
        }
        def cmds = [] as SynchronizedList<String>
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { rsp, HttpEntity<String> e ->
            VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd cmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd.class)
            cmds.add(cmd)
            return rsp
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = pool.uuid
            clusterUuid = cluster.uuid
            systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{192.168.0.0/16}".toString()]
        }

        List<VtepInventory> vtepinvs = queryVtep {
            conditions = ["poolUuid=${pool.getUuid()}".toString()]
        }

        List<String> vtepIps = vtepinvs.stream().map{vtep -> vtep.vtepIp}.distinct().collect(Collectors.toList())
        assert vtepIps.size() == 2
        assert vtepIps.contains(vtep1)
        assert vtepIps.contains(vtep2)
        retryInSecs(15) {
            // if don not have vxlannetwork, can not send cmd VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH
            assert cmds.size()==0
        }
    }
}
