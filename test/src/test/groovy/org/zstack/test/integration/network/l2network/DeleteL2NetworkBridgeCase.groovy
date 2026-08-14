package org.zstack.test.integration.network.l2network

import org.zstack.sdk.*
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.springframework.http.HttpEntity
import org.zstack.utils.data.SizeUnit
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.apache.commons.collections.list.SynchronizedList
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.core.db.SQL
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.host.HostStatus

class DeleteL2NetworkBridgeCase extends SubCase {
    EnvSpec env
    ZoneInventory zone
    ClusterInventory cluster
    HostInventory host1
    HostInventory host2

    @Override
    void clean() {
        env.delete()
    }

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
            }


            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
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
            initEnv()
            testDeleteL2NoVlanNetwork()
            testDeleteL2VlanNetwork()
            testDeleteL2VxlanNetwork()
            testDeleteL2VxlanPool()
            testDetachL2NoVlanNetworkFromCluster()
            testDetachL2VlanNetworkFromCluster()
            testDetachL2VxlanPoolFromCluster()
            testDeleteL2VOSuccessEvenIfResponseFail()
        }
    }

    void initEnv(){
        zone = env.inventoryByName("zone") as ZoneInventory
        cluster = env.inventoryByName("cluster") as ClusterInventory
        host1 = env.inventoryByName("kvm1") as HostInventory
        host2 = env.inventoryByName("kvm2") as HostInventory
    }

    void testDeleteL2NoVlanNetwork() {
        def nicName = "eth0"

        def l2_noVlan = createL2NoVlanNetwork {
            name = "l2-noVlan"
            zoneUuid = zone.uuid
            physicalInterface = nicName
        } as L2NetworkInventory

        attachL2NetworkToCluster {
            l2NetworkUuid = l2_noVlan.uuid
            clusterUuid = cluster.uuid
        }

        def cmds = [] as SynchronizedList<KVMAgentCommands.DeleteBridgeCmd>
        env.afterSimulator(KVMConstant.KVM_DELETE_L2NOVLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteBridgeCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DeleteBridgeCmd.class)
            cmds.add(deleteBridgeCmd)
            return rsp
        }

        deleteL2Network {
            uuid = l2_noVlan.uuid
        }

        assert cmds.size()==2

    }

    void testDetachL2NoVlanNetworkFromCluster() {
        def nicName = "eth1"

        def l2_noVlan = createL2NoVlanNetwork {
            name = "l2-noVlan"
            zoneUuid = zone.uuid
            physicalInterface = nicName
        } as L2NetworkInventory

        attachL2NetworkToCluster {
            l2NetworkUuid = l2_noVlan.uuid
            clusterUuid = cluster.uuid
        }

        def cmds = [] as SynchronizedList<KVMAgentCommands.DeleteBridgeCmd>
        env.afterSimulator(KVMConstant.KVM_DELETE_L2NOVLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteBridgeCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DeleteBridgeCmd.class)
            cmds.add(deleteBridgeCmd)
            return rsp
        }

        detachL2NetworkFromCluster {
            clusterUuid = cluster.uuid
            l2NetworkUuid = l2_noVlan.uuid
        }

        assert cmds.size()==2

    }




    void testDeleteL2VlanNetwork(){
        def nicName = "eth0"

        def l2_vlan = createL2VlanNetwork {
            name = "L2-Vlan"
            zoneUuid = zone.uuid
            physicalInterface = nicName
            vlan = 120
        } as L2NetworkInventory

        attachL2NetworkToCluster {
            l2NetworkUuid = l2_vlan.uuid
            clusterUuid = cluster.uuid
        }

        def cmds = [] as SynchronizedList<KVMAgentCommands.DeleteVlanBridgeCmd>
        env.afterSimulator(KVMConstant.KVM_DELETE_L2VLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteVlanBridgeCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DeleteVlanBridgeCmd.class)
            cmds.add(deleteVlanBridgeCmd)
            return rsp
        }

        deleteL2Network {
            uuid = l2_vlan.uuid
        }

        assert cmds.size()==2

    }


    void testDetachL2VlanNetworkFromCluster(){
        def nicName = "eth2"

        def l2_vlan = createL2VlanNetwork {
            name = "L2-vlan"
            zoneUuid = zone.uuid
            physicalInterface = nicName
            vlan = 130
        } as L2NetworkInventory

        attachL2NetworkToCluster {
            l2NetworkUuid = l2_vlan.uuid
            clusterUuid = cluster.uuid
        }

        def cmds = [] as SynchronizedList<KVMAgentCommands.DeleteVlanBridgeCmd>
        env.afterSimulator(KVMConstant.KVM_DELETE_L2VLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteVlanBridgeCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DeleteVlanBridgeCmd.class)
            cmds.add(deleteVlanBridgeCmd)
            return rsp
        }

        detachL2NetworkFromCluster {
            clusterUuid = cluster.uuid
            l2NetworkUuid = l2_vlan.uuid
        }


        assert cmds.size()==2

    }


    void testDeleteL2VxlanNetwork(){
        L2VxlanNetworkPoolInventory pool = createL2VxlanNetworkPool {
            name= "vxlan"
            zoneUuid = zone.uuid
        }

        createVniRange {
            startVni = 100
            endVni = 10000
            l2NetworkUuid = pool.getUuid()
            name = "TestRange1"
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            VxlanKvmAgentCommands.CheckVxlanCidrResponse resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse()
            if (entity.getHeaders().get("X-Resource-UUID")[0].equals(host1.uuid)) {
                resp.vtepIp = "192.168.200.10"
            } else {
                resp.vtepIp = "192.168.200.11"
            }
            resp.setSuccess(true)
            return resp
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = pool.getUuid()
            clusterUuid = cluster.uuid
            systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{192.168.100.0/24}".toString()]
        }

        L2NetworkInventory l2_vxlan = createL2VxlanNetwork {
            poolUuid = pool.getUuid()
            name = "TestVxlan1"
            zoneUuid = zone.uuid
        }


        def cmds = [] as SynchronizedList<VxlanKvmAgentCommands.DeleteVxlanBridgeCmd>
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_DELETE_L2VXLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteVxlanBridgeCmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.DeleteVxlanBridgeCmd.class)
            cmds.add(deleteVxlanBridgeCmd)
            return rsp
        }

        deleteL2Network {
            uuid = l2_vxlan.uuid
        }

        assert cmds.size()==2
    }


    void testDeleteL2VxlanPool(){
        L2VxlanNetworkPoolInventory pool = createL2VxlanNetworkPool {
            name= "vxlan"
            zoneUuid = zone.uuid
        }

        createVniRange {
            startVni = 20000
            endVni = 30000
            l2NetworkUuid = pool.uuid
            name = "range"
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            VxlanKvmAgentCommands.CheckVxlanCidrResponse resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse()
            if (entity.getHeaders().get("X-Resource-UUID")[0].equals(host1.uuid)) {
                resp.vtepIp = "192.168.100.10"
            } else {
                resp.vtepIp = "192.168.100.11"
            }
            resp.setSuccess(true)
            return resp
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = pool.getUuid()
            clusterUuid = cluster.uuid
            systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{192.168.100.0/24}".toString()]
        }

        L2NetworkInventory l2_vxlan1 = createL2VxlanNetwork {
            poolUuid = pool.getUuid()
            name = "vxlan1"
            zoneUuid = zone.uuid
        }

        L2NetworkInventory l2_vxlan2 = createL2VxlanNetwork {
            poolUuid = pool.uuid
            name = "vxlan2"
            zoneUuid = zone.uuid
        }


        def cmds = [] as SynchronizedList<VxlanKvmAgentCommands.DeleteVxlanBridgeCmd>
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_DELETE_L2VXLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteVxlanBridgeCmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.DeleteVxlanBridgeCmd.class)
            cmds.add(deleteVxlanBridgeCmd)
            return rsp
        }

        deleteL2Network {
            uuid = pool.uuid
        }

        assert cmds.size()==4
    }

    void testDetachL2VxlanPoolFromCluster(){
        L2VxlanNetworkPoolInventory pool = createL2VxlanNetworkPool {
            name= "vxlan"
            zoneUuid = zone.uuid
        }

        createVniRange {
            startVni = 20000
            endVni = 30000
            l2NetworkUuid = pool.uuid
            name = "range"
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            VxlanKvmAgentCommands.CheckVxlanCidrResponse resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse()
            if (entity.getHeaders().get("X-Resource-UUID")[0].equals(host1.uuid)) {
                resp.vtepIp = "192.168.100.10"
            } else {
                resp.vtepIp = "192.168.100.11"
            }
            resp.setSuccess(true)
            return resp
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = pool.getUuid()
            clusterUuid = cluster.uuid
            systemTags = ["l2NetworkUuid::${pool.getUuid()}::clusterUuid::${cluster.uuid}::cidr::{192.168.100.0/24}".toString()]
        }

        L2NetworkInventory l2_vxlan1 = createL2VxlanNetwork {
            poolUuid = pool.getUuid()
            name = "vxlan1"
            zoneUuid = zone.uuid
        }

        L2NetworkInventory l2_vxlan2 = createL2VxlanNetwork {
            poolUuid = pool.uuid
            name = "vxlan2"
            zoneUuid = zone.uuid
        }


        def cmds = [] as SynchronizedList<VxlanKvmAgentCommands.DeleteVxlanBridgeCmd>
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_DELETE_L2VXLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteVxlanBridgeCmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.DeleteVxlanBridgeCmd.class)
            cmds.add(deleteVxlanBridgeCmd)
            return rsp
        }

        detachL2NetworkFromCluster {
            clusterUuid = cluster.uuid
            l2NetworkUuid = pool.uuid
        }


        assert cmds.size()==4
    }


    void testDeleteL2VOSuccessEvenIfResponseFail(){
        def nicName = "eth0"

        def l2_noVlan = createL2NoVlanNetwork {
            name = "l2-noVlan"
            zoneUuid = zone.uuid
            physicalInterface = nicName
        } as L2NetworkInventory

        attachL2NetworkToCluster {
            l2NetworkUuid = l2_noVlan.uuid
            clusterUuid = cluster.uuid
        }

        env.simulator(KVMConstant.KVM_DELETE_L2NOVLAN_NETWORK_PATH) {
            def resp = new KVMAgentCommands.DeleteBridgeResponse()
            resp.setSuccess(false)
            resp.setError("on purpose")
            return resp
        }

        def cmds = [] as SynchronizedList<KVMAgentCommands.DeleteBridgeCmd>
        env.afterSimulator(KVMConstant.KVM_DELETE_L2NOVLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteBridgeCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DeleteBridgeCmd.class)
            cmds.add(deleteBridgeCmd)
            return rsp
        }


        SQL.New(HostVO.class).eq(HostVO_.uuid, host1.uuid)
                .set(HostVO_.status, HostStatus.Disconnected).update()

        deleteL2Network {
            uuid = l2_noVlan.uuid
        }

        assert cmds.size()==1

        def l2s = queryL2Network {
            conditions=["uuid=${l2_noVlan.uuid}".toString()]
        }

        assert  l2s.size() == 0
    }

}

