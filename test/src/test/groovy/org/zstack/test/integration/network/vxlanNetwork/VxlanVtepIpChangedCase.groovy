package org.zstack.test.integration.network.vxlanNetwork

import org.apache.commons.collections.list.SynchronizedList
import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.network.l2.vxlan.vtep.VtepVO
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.L2VxlanNetworkInventory
import org.zstack.sdk.L2VxlanNetworkPoolInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.NetworkServiceProviderInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class VxlanVtepIpChangedCase extends SubCase {
    EnvSpec env

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
            if (entity.getHeaders().get("X-Resource-UUID")[0] == host1.uuid) {
                resp.vtepIp = "192.168.100.10"
            } else {
                resp.vtepIp = "192.168.100.11"
            }
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

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.CreateVxlanBridgeResponse()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.PopulateVxlanFdbResponse()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd()
        }

        def vm1 = createVmInstance {
            name = "TestVm1"
            instanceOfferingUuid = (env.inventoryByName("instanceOffering") as InstanceOfferingInventory).uuid
            imageUuid = (env.inventoryByName("image1") as ImageInventory).uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
        } as VmInstanceInventory

        createVmInstance {
            name = "TestVm1"
            instanceOfferingUuid = (env.inventoryByName("instanceOffering") as InstanceOfferingInventory).uuid
            imageUuid = (env.inventoryByName("image1") as ImageInventory).uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host2.uuid
        }

        def vteps = Q.New(VtepVO.class).list() as List<VtepVO>
        assert vteps.size() == 2
        assert vteps.vtepIp.toSet() == ["192.168.100.10", "192.168.100.11"].toSet()

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            if (entity.getHeaders().get("X-Resource-UUID")[0] == host1.uuid) {
                resp.vtepIp = "192.168.101.10"
            } else {
                resp.vtepIp = "192.168.100.11"
            }
            resp.setSuccess(true)
            return resp
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.CreateVxlanBridgesCmd()
        }

        reconnectHost { uuid = host1.uuid }

        vteps = Q.New(VtepVO.class).list() as List<VtepVO>
        assert vteps.size() == 2
        assert vteps.vtepIp.toSet() == ["192.168.101.10", "192.168.100.11"].toSet()


        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse() as VxlanKvmAgentCommands.CheckVxlanCidrResponse
            if (entity.getHeaders().get("X-Resource-UUID")[0] == host1.uuid) {
                resp.vtepIp = "192.168.102.10"
            } else {
                resp.vtepIp = "192.168.100.11"
            }
            resp.setSuccess(true)
            return resp
        }

        def records = [] as SynchronizedList<VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd>
        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(entity.body, VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd.class)
            records.add(cmd.networkUuids)
            return new VxlanKvmAgentCommands.PopulateVxlanNetworksFdbCmd()
        }

        def realizeRecords = [] as SynchronizedList<Integer>
        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORKS_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(entity.body, VxlanKvmAgentCommands.CreateVxlanBridgesCmd.class)
            for (VxlanKvmAgentCommands.CreateVxlanBridgeCmd bcmd : cmd.bridgeCmds) {
                realizeRecords.add(bcmd.vni)
            }
            return new VxlanKvmAgentCommands.CreateVxlanBridgesCmd()
        }

        reconnectHost { uuid = host1.uuid }

        vteps = Q.New(VtepVO.class).list() as List<VtepVO>
        assert vteps.size() == 2
        assert vteps.vtepIp.toSet() == ["192.168.102.10", "192.168.100.11"].toSet()

        retryInSecs() {
            assert records.size() == 2
            assert records[0] == [vxlan.uuid]
        }

        assert realizeRecords.size() == 1
        assert realizeRecords.get(0) == vxlan.vni

        rebootVmInstance { uuid = vm1.uuid }
    }
}
