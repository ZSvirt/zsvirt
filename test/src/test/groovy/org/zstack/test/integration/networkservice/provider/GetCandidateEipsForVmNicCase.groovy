package org.zstack.test.integration.networkservice.provider

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
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

import static java.util.Arrays.asList

class GetCandidateEipsForVmNicCase extends SubCase{

    def DOC = """
1.There are three l3: fakePubL3 pubL3 flatL3
2.pubL3 for creating virtual router vm, flat l3 for creating vm and fakePubL3 for nothing
3.create eip for vmInVirtualRouter and only use pubL3
4.create eip for vmInFlat and only use pubL3 and flatL3

1.There are three vmNic: pubVmNic flatVmNic vpcVmNic
2.attach eip for flatVmNic use pubL3 and flatL3 
3.attach eip for vpcVmNic only use pubL3
4.cannot attach eip for pubVmNic
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
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 2
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
                        name = "kvm-1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(1000)
                        totalCpu = 1000
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                    attachL2Network("l2-vlan-100")
                }

                cluster {
                    name = "cluster-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(1000)
                        totalCpu = 1000
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-vlan-101")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "vr-eth0"

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
                        name = "pubL3-eth0"
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-eth0-ipv6"
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ipv6 {
                            name = "ipv6-Statefull-DHCP"
                            networkCidr = "2001:2001::/64"
                            addressMode = "Stateful-DHCP"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-vlan-100"
                    physicalInterface = "eth0"
                    vlan = 100

                    l3Network {
                        name = "flatL3-vlan-100-1"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "flatL3-vlan-100-1-ipv6"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ipv6 {
                            name = "ipv6-Statefull-DHCP"
                            networkCidr = "2001:2004::/64"
                            addressMode = "Stateful-DHCP"
                        }
                    }

                    l3Network {
                        name = "flatL3-vlan-100-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }
                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING]
                        }
                        ip {
                            startIp = "192.168.101.10"
                            endIp = "192.168.101.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.101.1"
                        }
                    }

                    l3Network {
                        name = "flatL3-vlan-100-2-ipv6"
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ipv6 {
                            name = "ipv6-Statefull-DHCP"
                            networkCidr = "2001:2003::/64"
                            addressMode = "Stateful-DHCP"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-vlan-101"
                    physicalInterface = "eth0"
                    vlan = 101

                    l3Network {
                        name = "flat-vlan-101"

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
                        name = "pub-vlan-101"
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "11.168.200.10"
                            endIp = "11.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.200.1"
                        }
                    }

                    l3Network {
                        name = "flatL3-vlan-101-ipv6"
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ipv6 {
                            name = "ipv6-Statefull-DHCP"
                            networkCidr = "2001:2002::/64"
                            addressMode = "Stateful-DHCP"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip1-pubL3-eth0"
                    useVip("pubL3-eth0")
                }

                eip {
                    name = "eip2"
                    useVip("pub-vlan-101")
                }

                eip {
                    name = "eip3"
                    useVip("flatL3-vlan-100-2")
                }

                eip {
                    name = "eip4-pubL3-eth0-ipv6"
                    useVip("pubL3-eth0-ipv6")
                }

                eip {
                    name = "eip5"
                    useVip("flatL3-vlan-101-ipv6")
                }

                eip {
                    name = "eip6"
                    useVip("flatL3-vlan-100-2-ipv6")
                }

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3-eth0")
                    usePublicL3Network("pubL3-eth0")
                    useImage("vr")
                }
            }

            vm {
                name = "vmInVirtualRouter"
                useImage("image")
                useL3Networks("vr-eth0")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInFlat"
                useImage("image")
                useL3Networks("flatL3-vlan-100-1")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInFlat-ipv6"
                useImage("image")
                useL3Networks("flatL3-vlan-100-1-ipv6")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmInCluster-2"
                useImage("image")
                useL3Networks("flat-vlan-101")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vmPubL3-eth0"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("pubL3-eth0")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testGetCandidateEipsForFlatVmNic()
            testGetCandidateEipsForVirtualRouterVmNic()
            testGetCandidateEipsForPubVmNic()
            testGetCandidateEipsForIpv6VmNic()
            testGetCandidateEipsForDualStackVmNic()
        }
    }

    void testGetCandidateEipsForFlatVmNic() {
        //cluster: l2: pubL3-eth0,vr-eth0  l2-vlan-100:flatL3-vlan-100-1,flatL3-vlan-100-2
        //cluster-1: l2-vlan-101:pub-vlan-101,flat-vlan-101
        def eip1_pubL3_eth0 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory   //pubL3-eth0
        def eip2 = env.inventoryByName("eip2") as EipInventory                         //pub-vlan-101
        def eip3 = env.inventoryByName("eip3") as EipInventory                         //flatL3-vlan-100-2

        def pubL3 = env.inventoryByName("pubL3-eth0") as L3NetworkInventory            //pubL3-eth0

        // 1: ipv4 flat VmNic attachable eips
        def vm1 = env.inventoryByName("vmInFlat")                //flatL3-vlan-100-1
        def eips = getVmNicAttachableEips {
            vmNicUuid = vm1.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips.size() ==  2                                 //pub-eip: eip1   flat-eip: eip3

        // 2: ipv4 exclude l3Network's eips which has attached to the vmInstance
        VmNicInventory nic = createVmNic {
            l3NetworkUuid = pubL3.uuid
        }
        attachVmNicToVm {
            vmInstanceUuid = vm1.uuid
            vmNicUuid = nic.uuid
            requestIp = nic.ip
        }
        def eips1 = getVmNicAttachableEips {
            vmNicUuid = vm1.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips1.size() == 1                                  //pub-eip: null   flat-eip: eip3
        deleteVmNic {
            uuid = nic.uuid
        }

        // 3: ipv4 exclude l3Network's eips which has attached to the vmNic
        attachEip {
            eipUuid = eip3.uuid
            vmNicUuid = vm1.vmNics.get(0).uuid
        }
        def eips2 = getVmNicAttachableEips {
            vmNicUuid = vm1.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips2.size() == 0                                 //can not bound more than 1 eip to a vm nic of flat
        detachEip {
            uuid = eip3.uuid
        }
    }

    void testGetCandidateEipsForVirtualRouterVmNic(){
        //cluster: l2: pubL3-eth0,vr-eth0    l2-vlan-100: flatL3-vlan-100-1,flatL3-vlan-100-2
        //cluster-1: l2-vlan-101:pub-vlan-101,flat-vlan-101
        def eip1_pubL3_eth0 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory   //pubL3-eth0
        def eip2 = env.inventoryByName("eip2") as EipInventory                         //pub-vlan-101
        def eip3 = env.inventoryByName("eip3") as EipInventory                         //flatL3-vlan-100-2

        def pubL3 = env.inventoryByName("pubL3-eth0") as L3NetworkInventory            //pubL3-eth0


        //1.1: ipv4 flat VmNic attachable eips
        def vm1 = env.inventoryByName("vmInVirtualRouter")     //vr-eth0
        VmNicInventory nic1 = vm1.vmNics.get(0)
        def eips = getVmNicAttachableEips {
            vmNicUuid = nic1.uuid
        } as List<EipInventory>
        assert eips.size() == 1                                //pub-eip: eip1-pubL3-eth0

        //1.2: ipv4 exclude l3Network's eips which has attached to the vmInstance
        VmNicInventory nic = createVmNic {
            l3NetworkUuid = pubL3.uuid
        }
        attachVmNicToVm {
            vmInstanceUuid = vm1.uuid
            vmNicUuid = nic.uuid
        }
        def eips1 = getVmNicAttachableEips {
            vmNicUuid = nic1.uuid
        } as List<EipInventory>
        assert eips1.size() == 0                               //pub-eip: null
        deleteVmNic {
            uuid = nic.uuid
        }

        //1.3: ipv4 exclude l3Network's eips which has attached to the vmNic
        attachEip {
            eipUuid = eip1_pubL3_eth0.uuid
            vmNicUuid = vm1.vmNics.get(0).uuid
        }
        def eips2 = getVmNicAttachableEips {
            vmNicUuid = nic1.uuid
        } as List<EipInventory>
        assert eips2.size() == 0                              //pub-eip: null
        detachEip {
            uuid = eip1_pubL3_eth0.uuid
        }
    }

    void testGetCandidateEipsForPubVmNic() {
        //cluster: l2:pubL3-eth0,vr-eth0  l2-vlan-100:flatL3,flatL3-vlan-100-2
        //cluster-1: l2-vlan-101:pub-vlan-101,flat-vlan-101
        def eip1_pubL3_eth0 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory   //pubL3-eth0
        def eip2 = env.inventoryByName("eip2") as EipInventory                         //pub-vlan-101
        def eip3 = env.inventoryByName("eip3") as EipInventory                         //flatL3-vlan-100-2

        def pubL3 = env.inventoryByName("pubL3-eth0") as L3NetworkInventory             //pubL3-eth0

        // pub VmNic attachable eips
        def vm1 = env.inventoryByName("vmPubL3-eth0")
        def eips = getVmNicAttachableEips {
            vmNicUuid = vm1.vmNics.get(0).uuid
        } as List<EipInventory>

        assert eips.size() == 0
    }

    void testGetCandidateEipsForIpv6VmNic(){
        //cluster: l2:pubL3-eth0-ipv6  l2-vlan-100:flatL3-vlan-100-1-ipv6,flatL3-vlan-100-2-ipv6
        //cluster-1: l2-vlan-101:flatL3-vlan-101-ipv6
        def eip1_pubL3_eth0 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory   //pubL3-eth0
        def eip2 = env.inventoryByName("eip2") as EipInventory                         //pub-vlan-101
        def eip3 = env.inventoryByName("eip3") as EipInventory                         //flatL3-vlan-100-2
        def eip4_pubL3_eth0_ipv6 = env.inventoryByName("eip4-pubL3-eth0-ipv6") as EipInventory   //pubL3-eth0-ipv6
        def eip5 = env.inventoryByName("eip5") as EipInventory                                   //pub-vlan-101-ipv6
        def eip6 = env.inventoryByName("eip6") as EipInventory                                   //flatL3-vlan-100-2-ipv6

        def pubL3_ipv6 = env.inventoryByName("pubL3-eth0-ipv6") as L3NetworkInventory            //pubL3-eth0-ipv6

        // 1: ipv6 flat VmNic attachable eips
        def vm2 = env.inventoryByName("vmInFlat-ipv6")        //flatL3-vlan-100-1-ipv6
        def eips = getVmNicAttachableEips {
            vmNicUuid = vm2.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips.size() == 2                               //pub-eip: eip4  flat-eip: eip6

        // 2: ipv6 exclude l3Network's eips which has attached to the vmInstance
        VmNicInventory nic1 = createVmNic {
            l3NetworkUuid = pubL3_ipv6.uuid
        }
        attachVmNicToVm {
            vmInstanceUuid = vm2.uuid
            vmNicUuid = nic1.uuid
        }
        def eips1 = getVmNicAttachableEips {
            vmNicUuid = vm2.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips1.size() == 1                              //pub-eip: null  flat-eip: eip6
        deleteVmNic {
            uuid = nic1.uuid
        }

        // 3: ipv6 exclude l3Network's eips which has attached to the vmNic
        attachEip {
            eipUuid = eip6.uuid
            vmNicUuid = vm2.vmNics.get(0).uuid
        }
        def eips2 = getVmNicAttachableEips {
            vmNicUuid = vm2.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips2.size() == 0                              //can not bound more than 1 eip to a vm nic of flat
        detachEip {
            uuid = eip6.uuid
        }
    }

    void testGetCandidateEipsForDualStackVmNic(){
        //cluster: l2: pubL3-eth0,vr-eth0  l2-vlan-100:flatL3-vlan-100-1,flatL3-vlan-100-2
        //cluster-1: l2-vlan-101:pub-vlan-101,flat-vlan-101
        //cluster: l2:pubL3-eth0-ipv6  l2-vlan-100:flatL3-vlan-100-1-ipv6,flatL3-vlan-100-2-ipv6
        //cluster-1: l2-vlan-101:flatL3-vlan-101-ipv6
        def image = env.inventoryByName("image")
        def instanceOffering = env.inventoryByName("instanceOffering")

        def eip1_pubL3_eth0 = env.inventoryByName("eip1-pubL3-eth0") as EipInventory   //pubL3-eth0
        def eip2 = env.inventoryByName("eip2") as EipInventory                         //pub-vlan-101
        def eip3 = env.inventoryByName("eip3") as EipInventory                         //flatL3-vlan-100-2
        def eip4_pubL3_eth0_ipv6 = env.inventoryByName("eip4-pubL3-eth0-ipv6") as EipInventory   //pubL3-eth0-ipv6
        def eip5 = env.inventoryByName("eip5") as EipInventory                                   //pub-vlan-101-ipv6
        def eip6 = env.inventoryByName("eip6") as EipInventory                                   //flatL3-vlan-100-2-ipv6

        def flatL3_vlan_100_2 = env.inventoryByName("flatL3-vlan-100-2") as L3NetworkInventory            //flatL3-vlan-100-2
        def flatL3_vlan_100_1 = env.inventoryByName("flatL3-vlan-100-1") as L3NetworkInventory            //flatL3-vlan-100-1

        // add ipr6 to ipv4 network -> dual stack network
        IpRangeInventory ipr6 = addIpv6Range {
            name = "ipr-6"
            l3NetworkUuid = flatL3_vlan_100_2.uuid
            startIp = "2003:2001::0010"
            endIp = "2003:2001::0020"
            gateway = "2003:2001::2"
            prefixLen = 64
            addressMode = "Stateful-DHCP"
        }

        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = flatL3_vlan_100_2.uuid
        }

        createEip {
            name = "eip-46"
            vipUuid = vip.getUuid()
        }

        def vm3 = env.inventoryByName("vmInFlat")                    //flatL3-vlan-100-1
        def eips = getVmNicAttachableEips {
            vmNicUuid = vm3.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips.size() == 3                                       //pub-eip: eip1,eip-46   flat-eip: eip3

        deleteVip {
            uuid = vip.uuid
        }

        // add ipr6 to ipv4 network which has vmNic -> dual stack vmNic
        IpRangeInventory ipr6_1 = addIpv6Range {
            name = "ipr-6-1"
            l3NetworkUuid = flatL3_vlan_100_1.uuid
            startIp = "2003:2002::0010"
            endIp = "2003:2002::0020"
            gateway = "2003:2002::2"
            prefixLen = 64
            addressMode = "Stateful-DHCP"
        }

        VmInstanceInventory vm4 = createVmInstance {
            name = "vmInFlat_dual_stack"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(flatL3_vlan_100_1.uuid)
        } as VmInstanceInventory

        def eips1 = getVmNicAttachableEips {
            vmNicUuid = vm4.vmNics.get(0).uuid
        } as List<EipInventory>
        assert eips1.size() == 4                                      //eip1/eip3/eip4/eip6

        VmInstanceInventory vm5 = createVmInstance {
            name = "vmInFlat_dual_stack"
            instanceOfferingUuid = instanceOffering.uuid
            l3NetworkUuids = [flatL3_vlan_100_1.uuid]
            imageUuid = image.uuid
        } as VmInstanceInventory

        def eips2 = getVmNicAttachableEips {
            vmNicUuid = vm5.vmNics.get(0).uuid
            ipVersion = 4
        } as List<EipInventory>
        assert eips2.size() == 2                                      //eip1/eip3

        VmInstanceInventory vm6 = createVmInstance {
            name = "vmInFlat_dual_stack"
            instanceOfferingUuid = instanceOffering.uuid
            l3NetworkUuids = [flatL3_vlan_100_1.uuid]
            imageUuid = image.uuid
        } as VmInstanceInventory
        def eips3 = getVmNicAttachableEips {
            vmNicUuid = vm6.vmNics.get(0).uuid
            ipVersion = 6
        } as List<EipInventory>
        assert eips3.size() == 2                                      //eip4/eip6
    }

    @Override
    void clean() {
        env.delete()
    }
}
