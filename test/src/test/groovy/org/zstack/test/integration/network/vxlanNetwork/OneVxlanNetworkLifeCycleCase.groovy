package org.zstack.test.integration.network.vxlanNetwork

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.l2.vxlan.vtep.VtepVO
import org.zstack.network.l2.vxlan.vtep.VtepVO_
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.NetworkServiceGlobalConfig
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.*
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static java.util.Arrays.asList

/**
 * Created by weiwang on 17/03/2017.
 */
class OneVxlanNetworkLifeCycleCase extends SubCase {
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
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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

                    attachPrimaryStorage("nfs-ps")
                    attachL2Network("l2-novlan")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs-ps")
                    attachL2Network("l2-novlan")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                nfsPrimaryStorage {
                    name = "nfs-ps"
                    url = "127.0.0.1:/nfs_root"
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
            testVxlanNetwork()
        }
    }

    void testVxlanNetwork() {
        ZoneSpec zone = env.specByName("zone")
        String cuuid1 = zone.getClusters().get(0).inventory.getUuid()
        String cuuid2 = zone.getClusters().get(1).inventory.getUuid()

        L2VxlanNetworkPoolInventory poolinv = createL2VxlanNetworkPool {
            delegate.name = "TestVxlanPool"
            delegate.zoneUuid = zone.inventory.getUuid()
        }


        createVniRange {
            delegate.startVni = 100
            delegate.endVni = 10000
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.name = "TestRange1"
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            VxlanKvmAgentCommands.CheckVxlanCidrResponse resp = new VxlanKvmAgentCommands.CheckVxlanCidrResponse()
            if (entity.getHeaders().get("X-Resource-UUID")[0].equals((env.specByName("kvm1") as HostSpec).inventory.uuid)) {
                resp.vtepIp = "192.168.100.10"
            } else {
                resp.vtepIp = "192.168.100.11"
            }
            resp.physicalInterfaceName = "eth0"
            resp.setSuccess(true)
            return resp
        }

        attachL2NetworkToCluster {
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.clusterUuid = cuuid1
            delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid1}::cidr::{192.168.100.0/24}".toString()]
        }

        List<VtepVO> vteps = Q.New(VtepVO.class)
                .eq(VtepVO_.poolUuid, poolinv.getUuid())
                .list();
        assert vteps[0].physicalInterface == "eth0"

        L2NetworkInventory netinv = createL2VxlanNetwork {
            delegate.poolUuid = poolinv.getUuid()
            delegate.name = "TestVxlan1"
            delegate.zoneUuid = zone.inventory.getUuid()
        }

        Map accountInventories = getResourceAccount {
            delegate.resourceUuids = [netinv.uuid]
        }

        assert accountInventories.size() == 1
        assert netinv.getAttachedClusterUuids().size() == 1

        attachL2NetworkToCluster {
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.clusterUuid = cuuid2
            delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cuuid2}::cidr::{192.168.101.0/24}".toString()]
        }

        netinv = queryL2VxlanNetwork {
            delegate.conditions = ["uuid=${netinv.getUuid()}".toString()]
        }[0]

        queryL2VxlanNetwork {
            delegate.conditions = ["poolUuid=${poolinv.getUuid()}".toString()]
        }[0]

        assert netinv.getAttachedClusterUuids().size() == 2

        L3NetworkInventory l3_1 = createL3Network {
            delegate.name = "TestL3Net1"
            delegate.l2NetworkUuid = netinv.getUuid()
        }

        addIpRange {
            delegate.name = "TestIpRange"
            delegate.l3NetworkUuid = l3_1.getUuid()
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
            delegate.l3NetworkUuid = l3_1.getUuid()
            delegate.networkServices = netServices
        }

        L2NetworkInventory netinv2 = createL2VxlanNetwork {
            delegate.poolUuid = poolinv.getUuid()
            delegate.name = "TestVxlan1"
            delegate.zoneUuid = zone.inventory.getUuid()
        }

        L3NetworkInventory l3_2 = createL3Network {
            delegate.name = "TestL3Net2"
            delegate.l2NetworkUuid = netinv2.getUuid()
        }

        addIpRange {
            delegate.name = "TestIpRange"
            delegate.l3NetworkUuid = l3_2.getUuid()
            delegate.startIp = "192.168.100.2"
            delegate.endIp = "192.168.100.253"
            delegate.gateway = "192.168.100.1"
            delegate.netmask = "255.255.255.0"
        }

        attachNetworkServiceToL3Network {
            delegate.l3NetworkUuid = l3_2.getUuid()
            delegate.networkServices = netServices
        }


        List<String> record = new ArrayList<>()

        NetworkServiceGlobalConfig.DHCP_MTU_VXLAN.updateValue(9600)
        VxlanKvmAgentCommands.CreateVxlanBridgeCmd vxlanCmd = null
        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            vxlanCmd = JSONObjectUtil.toObject(entity.getBody(), VxlanKvmAgentCommands.CreateVxlanBridgeCmd.class)
            record.add(entity.body)
            return new VxlanKvmAgentCommands.CreateVxlanBridgeResponse()
        }

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return new VxlanKvmAgentCommands.PopulateVxlanFdbResponse()
        }

        VmInstanceInventory vm0 = createVmInstance {
            delegate.name = "TestVm0"
            delegate.instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            delegate.imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            delegate.l3NetworkUuids = [(env.specByName("l3-novlan") as L3NetworkSpec).inventory.uuid]
            delegate.hostUuid = (env.specByName("kvm1") as HostSpec).inventory.uuid
        }

        attachL3NetworkToVm {
            delegate.l3NetworkUuid = l3_1.uuid
            delegate.vmInstanceUuid = vm0.uuid
        }

        assert record.get(0).contains("vtep")
        assert vxlanCmd.mtu == 9600
        assert vxlanCmd.dstport == VxlanNetworkPoolConstant.VXLAN_PORT
        createVmInstance {
            delegate.name = "TestVm3"
            delegate.instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            delegate.imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            delegate.l3NetworkUuids = [l3_1.getUuid(), l3_2.getUuid()]
            delegate.defaultL3NetworkUuid = l3_1.getUuid()
            delegate.hostUuid = (env.specByName("kvm2") as HostSpec).inventory.uuid
        }

        VmInstanceInventory vm1 = createVmInstance {
            delegate.name = "TestVm1"
            delegate.instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            delegate.imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            delegate.l3NetworkUuids = [l3_1.getUuid()]
            delegate.hostUuid = (env.specByName("kvm1") as HostSpec).inventory.uuid
        }


        record.clear()

        env.simulator(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            record.add(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH)
            return new VxlanKvmAgentCommands.CreateVxlanBridgeResponse()
        }


        env.simulator(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            record.add(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH)
            return new FlatDhcpBackend.PrepareDhcpRsp()
        }

        attachL3NetworkToVm {
            delegate.l3NetworkUuid = l3_2.uuid
            delegate.vmInstanceUuid = vm1.uuid
        }

        assert record.size() == 2

        createVmInstance {
            delegate.name = "TestVm2"
            delegate.instanceOfferingUuid = (env.specByName("instanceOffering") as InstanceOfferingSpec).inventory.uuid
            delegate.imageUuid = (env.specByName("image1") as ImageSpec).inventory.uuid
            delegate.l3NetworkUuids = [l3_1.getUuid()]
            delegate.hostUuid = (env.specByName("kvm2") as HostSpec).inventory.uuid
        }

        reconnectHost {
            delegate.uuid = (env.specByName("kvm1") as KVMHostSpec).inventory.uuid
        }


        migrateVm {
            delegate.vmInstanceUuid = vm1.getUuid()
            delegate.hostUuid = (env.specByName("kvm2") as HostSpec).inventory.uuid
        }

        assert record.get(2).equals(VxlanNetworkPoolConstant.VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH)
        assert record.get(3).equals(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH)

        poolinv = queryL2VxlanNetworkPool{}[0]

        assert poolinv.getAttachedVtepRefs().size().equals(2)

        detachL2NetworkFromCluster {
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.clusterUuid = cuuid2
        }

        poolinv = queryL2VxlanNetworkPool{}[0]

        assert poolinv.getAttachedVtepRefs().size().equals(1)

        // Same to above, just test queryVtep API

        List<VtepInventory> vtepinvs = queryVtep {
            delegate.conditions = ["poolUuid=${poolinv.getUuid()}".toString()]
        }

        assert vtepinvs.size().equals(1)

        netinv = queryL2VxlanNetwork {
            delegate.conditions = ["uuid=${netinv.getUuid()}".toString()]
        }[0]

        assert netinv.getAttachedClusterUuids().size() == 1

        List<SystemTagInventory> tags1 = querySystemTag {
            delegate.conditions = ["resourceUuid=${poolinv.getUuid()}".toString()]
        }

        assert tags1.size() == 1

        deleteL2Network {
            delegate.uuid = poolinv.getUuid()
        }

        List<L2NetworkInventory> nets = queryL2Network {}
        List<L3NetworkInventory> l3Nets = queryL3Network {}
        List<SystemTagInventory> tags = querySystemTag {
            delegate.conditions = ["resourceUuid=${poolinv.getUuid()}".toString()]
        }

        assert nets.size() == 1
        assert l3Nets.size() == 1
        assert tags.isEmpty()
    }


    @Override
    void clean() {
        env.delete()
    }
}
