package org.zstack.test.integration.networkservice.provider

import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.appliancevm.ApplianceVmVO_
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.network.l2.L2NetworkClusterRefVO
import org.zstack.header.network.l2.L2NetworkClusterRefVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by heathhose on 17-5-14.
 */
class GetCandidateVmNicsForEipInVirtualRouterCase extends SubCase{

    def DOC = """
1.There are three l3: fakePubL3 pubL3 flatL3
2.pubL3 for creating virtual router vm, flat l3 for creating vm and fakePubL3 for nothing
3.create eip for vmInVirtualRouter and only use pubL3
4.create eip for vmInFlat and only use pubL3 and flatL3
"""
    
    EnvSpec env
    DatabaseFacade dbf
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
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
                    name = "image"
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
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                    attachL2Network("l2-vlan")
                    attachL2Network("l2-flat")
                }

                cluster {
                    name = "cluster-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local-1")
                    attachL2Network("l2-vlan-100")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                localPrimaryStorage {
                    name = "local-1"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"
                        category = "Public"
                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-vlan"
                    physicalInterface = "eth0"
                    vlan = 3001

                    l3Network {
                        name = "fakePubL3"
                        category = "Public"
                        ip {
                            startIp = "11.168.200.10"
                            endIp = "11.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.200.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-flat"
                    physicalInterface = "eth0"
                    vlan = 3002

                    l3Network {
                        name = "flatL3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }
                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING]
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
                    name = "l2-vlan-100"
                    physicalInterface = "eth0"
                    vlan = 100

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-1"
                        category = "Public"
                        ip {
                            startIp = "11.168.200.10"
                            endIp = "11.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.200.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip1"
                    useVip("pubL3")
                }

                eip {
                    name = "eip2"
                    useVip("fakePubL3")
                }

                eip {
                    name = "eip3"
                    useVip("pubL3-1")
                }

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }

            vm {
                name = "vmInVirtualRouter"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInFlat"
                useImage("image")
                useL3Networks("flatL3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInFlat-1"
                useImage("image")
                useL3Networks("flatL3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInCluster-2"
                useImage("image")
                useL3Networks("l3-2")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testGetCandidateVmNicsForEipInVirtualRouter()
        }
    }

    void testGetCandidateVmNicsForEipInVirtualRouter(){
        def eip = env.inventoryByName("eip1") as EipInventory
        def eip2 = env.inventoryByName("eip2") as EipInventory
        def eip3 = env.inventoryByName("eip3") as EipInventory
        def pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory
        def fakePubL3 = env.inventoryByName("fakePubL3") as L3NetworkInventory
        def l2Flat = env.inventoryByName("l2-flat") as L2NetworkInventory
        def l2vlan_100 = env.inventoryByName("l2-vlan-100") as L2NetworkInventory
        def cluster_1 = env.inventoryByName("cluster-1") as ClusterInventory
        def vmInFlat_1 = env.inventoryByName("vmInFlat-1") as VmInstanceInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory

        GetEipAttachableVmNicsAction action = new GetEipAttachableVmNicsAction()
        action.eipUuid = eip.uuid
        action.limit = 2
        action.start = 0
        action.sessionId = adminSession()
        GetEipAttachableVmNicsAction.Result res = action.call()
        assert res.error == null
        assert res.value.start == 2
        assert res.value.more
        assert res.value.inventories.size() == 2

        action = new GetEipAttachableVmNicsAction()
        action.eipUuid = eip.uuid
        action.limit = 2
        action.start = 2
        action.sessionId = adminSession()
        res = action.call()
        assert res.error == null
        assert !res.value.more
        assert res.value.inventories.size() == 1

        action = new GetEipAttachableVmNicsAction()
        action.eipUuid = eip.uuid
        action.vmName = "vmInFlat"
        action.limit = 100
        action.start = 0
        action.sessionId = adminSession()
        res = action.call()
        assert res.error == null
        assert res.value.inventories.size() == 2

        action = new GetEipAttachableVmNicsAction()
        action.eipUuid = eip.uuid
        action.vmUuid = vmInFlat_1.uuid
        action.limit = 100
        action.start = 0
        action.sessionId = adminSession()
        res = action.call()
        assert res.error == null
        assert res.value.inventories.size() == 1

        stopVmInstance {
            uuid = vmInFlat_1.uuid
        }
        /* after APIPrimaryStorageMigrateVmMsg, clusterUuid in vmInstanceVO is null
        * this vm is excluded from eip candidate nic */
        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInFlat_1.uuid).set(VmInstanceVO_.clusterUuid, null).update()

        //vmInVirtualRouter vmInFlat should be listed.
        def nics = getEipAttachableVmNics {
            eipUuid = eip.uuid
        } as List<VmNicInventory>
        assert nics.size() == 2

        //vmInFlat should be listed
        def nics2 = getEipAttachableVmNics {
            eipUuid = eip2.uuid
        } as List<VmNicInventory>
        assert nics2.size() == 1

        //after vrouter attached fakePublicL3, vmInVR should be listed
        String vrVmUuid = Q.New(ApplianceVmVO.class).select(ApplianceVmVO_.uuid)
                .eq(ApplianceVmVO_.managementNetworkUuid, pubL3.uuid).findValue()
        attachL3NetworkToVm {
            l3NetworkUuid = fakePubL3.uuid
            vmInstanceUuid = vrVmUuid
        }

        def nics3 = getEipAttachableVmNics {
            eipUuid = eip2.uuid
        } as List<VmNicInventory>
        assert nics3.size() == 2

        //vmInCluster-2 should be listed
        VmInstanceInventory vm = queryVmInstance {
            conditions=["name=vmInCluster-2"]
        }[0]

        def nics4 = getEipAttachableVmNics {
            eipUuid = eip3.uuid
        } as List<VmNicInventory>
        assert nics4.size() == 1
        assert nics4.get(0).uuid == vm.getVmNics().get(0).uuid

        attachL2NetworkToCluster {
            l2NetworkUuid = l2Flat.uuid
            clusterUuid = cluster_1.uuid
        }

        //vmInCluster-2 should be listed
        VmInstanceInventory vm_flat = queryVmInstance {
            conditions=["name=vmInFlat"]
        }[0]

        def nics5 = getEipAttachableVmNics {
            eipUuid = eip3.uuid
        } as List<VmNicInventory>
        assert nics5.size() == 2
        assert nics5.get(0).uuid == vm.getVmNics().get(0).uuid || nics5.get(0).uuid == vm_flat.getVmNics().get(0).uuid
        assert nics5.get(1).uuid == vm.getVmNics().get(0).uuid || nics5.get(1).uuid == vm_flat.getVmNics().get(0).uuid
        assert nics5.get(0).uuid != nics5.get(1).uuid

        SQL.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2vlan_100.uuid)
                .eq(L2NetworkClusterRefVO_.clusterUuid, cluster.uuid).delete()
        SQL.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2vlan_100.uuid)
                .eq(L2NetworkClusterRefVO_.clusterUuid, cluster_1.uuid).delete()
        def nics6 = getEipAttachableVmNics {
            eipUuid = eip3.uuid
        } as List<VmNicInventory>
        assert nics6.size() == 0

        /* add ref back to make env clean happy */
        L2NetworkClusterRefVO ref = new L2NetworkClusterRefVO()
        ref.l2NetworkUuid = l2vlan_100.uuid
        ref.clusterUuid = cluster.uuid
        dbf.persist(ref)

        ref = new L2NetworkClusterRefVO()
        ref.l2NetworkUuid = l2vlan_100.uuid
        ref.clusterUuid = cluster_1.uuid
        dbf.persist(ref)
    }

    @Override
    void clean() {
        env.delete()
    }

}
