package org.zstack.test.integration.network.l2network

import org.apache.commons.collections.list.SynchronizedList
import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanKvmAgentCommands
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L2NetworkInventory
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

import static java.util.Arrays.asList

/**
 * Created by heathhose on 17-5-6.
 */
public class AttachL2NetworkWithTwoClustersCase extends SubCase{

    EnvSpec env
    DatabaseFacade dbf
    @Override
    public void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    public void environment() {
        env = env {
            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "127.0.0.1"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2-1")
                    attachL2Network("l2-2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2-1")
                    attachL2Network("l2-2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth0"
                    vlan = 100

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.102.10"
                            endIp = "192.168.102.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.102.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    public void test() {
        env.create {
            testDetachNoVlanL2NetworkFromCluster()
            testDetachVlanL2NetworkFromCluster()
            testDetachVxlanPoolFromCluster()
        }

    }

    void testDetachNoVlanL2NetworkFromCluster(){
        L2NetworkInventory l21 = env.inventoryByName("l2-1")
        ClusterInventory cluster1 = env.inventoryByName("cluster1")
        ClusterInventory cluster2 = env.inventoryByName("cluster2")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        ImageInventory image = env.inventoryByName("image1")
        HostInventory host1 = env.inventoryByName("kvm-1")
        HostInventory host2 = env.inventoryByName("kvm-2")

        assert l21.virtualNetworkId == 0

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm-1"
            cpuNum = 2
            memorySize = gb(2)
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_1.uuid)
            hostUuid = host1.uuid
        }
        assert vm1.hostUuid == host1.uuid

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm-2"
            cpuNum = 2
            memorySize = gb(2)
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_1.uuid)
            hostUuid = host2.uuid
        }
        assert vm2.hostUuid == host2.uuid

        def cmds = [] as SynchronizedList<KVMAgentCommands.DeleteBridgeCmd>
        env.afterSimulator(KVMConstant.KVM_DELETE_L2NOVLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteBridgeCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DeleteBridgeCmd.class)
            cmds.add(deleteBridgeCmd)
            return rsp
        }

        detachL2NetworkFromCluster {
            l2NetworkUuid = l21.uuid
            clusterUuid = cluster1.uuid
        }
        assert cmds.size() == 1

        l21 = queryL2Network {conditions = ["name=l2-1"]} [0]
        assert l21.attachedClusterUuids.size() == 1
        assert l21.attachedClusterUuids.contains(cluster2.uuid)

        vm1 = queryVmInstance {conditions=["uuid=${vm1.uuid}"]} [0]
        assert vm1.vmNics.size() == 0

        vm2 = queryVmInstance {conditions=["uuid=${vm2.uuid}"]} [0]
        assert vm2.vmNics.size() == 1
    }

    void testDetachVlanL2NetworkFromCluster(){
        L2NetworkInventory l21 = env.inventoryByName("l2-2")
        ClusterInventory cluster1 = env.inventoryByName("cluster1")
        ClusterInventory cluster2 = env.inventoryByName("cluster2")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-2")
        ImageInventory image = env.inventoryByName("image1")
        HostInventory host1 = env.inventoryByName("kvm-1")
        HostInventory host2 = env.inventoryByName("kvm-2")

        assert l21.virtualNetworkId == 100

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm-21"
            cpuNum = 2
            memorySize = gb(2)
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_1.uuid)
            hostUuid = host1.uuid
        }
        assert vm1.hostUuid == host1.uuid

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm-22"
            cpuNum = 2
            memorySize = gb(2)
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_1.uuid)
            hostUuid = host2.uuid
        }
        assert vm2.hostUuid == host2.uuid

        def cmds = [] as SynchronizedList<KVMAgentCommands.DeleteVlanBridgeCmd>
        env.afterSimulator(KVMConstant.KVM_DELETE_L2VLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteBridgeCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DeleteVlanBridgeCmd.class)
            cmds.add(deleteBridgeCmd)
            return rsp
        }

        detachL2NetworkFromCluster {
            l2NetworkUuid = l21.uuid
            clusterUuid = cluster1.uuid
        }
        assert cmds.size() == 1

        l21 = queryL2Network {conditions = ["name=l2-1"]} [0]
        assert l21.attachedClusterUuids.size() == 1
        assert l21.attachedClusterUuids.contains(cluster2.uuid)

        vm1 = queryVmInstance {conditions=["uuid=${vm1.uuid}"]} [0]
        assert vm1.vmNics.size() == 0

        vm2 = queryVmInstance {conditions=["uuid=${vm2.uuid}"]} [0]
        assert vm2.vmNics.size() == 1
    }

    void testDetachVxlanPoolFromCluster(){
        ZoneInventory zone = env.inventoryByName("zone")
        ClusterInventory cluster1 = env.inventoryByName("cluster1")
        ClusterInventory cluster2 = env.inventoryByName("cluster2")
        ImageInventory image = env.inventoryByName("image1")
        HostInventory host1 = env.inventoryByName("kvm-1")
        HostInventory host2 = env.inventoryByName("kvm-2")

        L2VxlanNetworkPoolInventory poolinv = createL2VxlanNetworkPool {
            delegate.name = "TestVxlanPool"
            delegate.zoneUuid = zone.uuid
        }

        assert poolinv.virtualNetworkId == 0

        createVniRange {
            delegate.startVni = 100
            delegate.endVni = 10000
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.name = "TestRange1"
        }

        attachL2NetworkToCluster {
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.clusterUuid = cluster1.uuid
            delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cluster1.uuid}::cidr::{127.0.0.0/8}".toString()]
        }

        attachL2NetworkToCluster {
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.clusterUuid = cluster2.uuid
            delegate.systemTags = ["l2NetworkUuid::${poolinv.getUuid()}::clusterUuid::${cluster2.uuid}::cidr::{127.0.0.0/8}".toString()]
        }

        L2NetworkInventory vxlanL2 = createL2VxlanNetwork {
            delegate.poolUuid = poolinv.getUuid()
            delegate.name = "vxlanL2"
            delegate.zoneUuid = zone.uuid
        }

        def vniRange = 100..10000
        assert vniRange.contains(vxlanL2.virtualNetworkId)

        L3NetworkInventory vxlanL3 = createL3Network {
            delegate.name = "vxlanL3"
            delegate.l2NetworkUuid = vxlanL2.getUuid()
        }

        addIpRange {
            delegate.name = "TestIpRange"
            delegate.l3NetworkUuid = vxlanL3.getUuid()
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
            delegate.l3NetworkUuid = vxlanL3.getUuid()
            delegate.networkServices = netServices
        }

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm-31"
            cpuNum = 2
            memorySize = gb(2)
            imageUuid = image.uuid
            l3NetworkUuids = asList(vxlanL3.uuid)
            hostUuid = host1.uuid
        }
        assert vm1.hostUuid == host1.uuid

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm-22"
            cpuNum = 2
            memorySize = gb(2)
            imageUuid = image.uuid
            l3NetworkUuids = asList(vxlanL3.uuid)
            hostUuid = host2.uuid
        }
        assert vm2.hostUuid == host2.uuid

        def cmds = [] as SynchronizedList<VxlanKvmAgentCommands.DeleteVxlanBridgeCmd>
        env.afterSimulator(VxlanNetworkPoolConstant.VXLAN_KVM_DELETE_L2VXLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            def deleteBridgeCmd = JSONObjectUtil.toObject(e.body, VxlanKvmAgentCommands.DeleteVxlanBridgeCmd.class)
            cmds.add(deleteBridgeCmd)
            return rsp
        }

        detachL2NetworkFromCluster {
            l2NetworkUuid = poolinv.uuid
            clusterUuid = cluster1.uuid
        }
        assert cmds.size() == 1

        vxlanL2 = queryL2Network {conditions = ["name=vxlanL2"]} [0]
        assert vxlanL2.attachedClusterUuids.size() == 1
        assert vxlanL2.attachedClusterUuids.contains(cluster2.uuid)

        vm1 = queryVmInstance {conditions=["uuid=${vm1.uuid}"]} [0]
        assert vm1.vmNics.size() == 0

        vm2 = queryVmInstance {conditions=["uuid=${vm2.uuid}"]} [0]
        assert vm2.vmNics.size() == 1
    }

    @Override
    public void clean() {
        env.delete()
    }
}
