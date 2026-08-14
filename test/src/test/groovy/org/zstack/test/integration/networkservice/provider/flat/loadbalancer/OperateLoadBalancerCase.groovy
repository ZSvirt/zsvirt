package org.zstack.test.integration.networkservice.provider.flat.loadbalancer

import org.zstack.core.db.Q
import org.zstack.header.configuration.InstanceOfferingVO
import org.zstack.header.network.l3.L3NetworkVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.vip.VipNetworkServicesRefVO
import org.zstack.network.service.vip.VipNetworkServicesRefVO_
import org.zstack.network.service.vip.VipPeerL3NetworkRefVO
import org.zstack.network.service.vip.VipPeerL3NetworkRefVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VirtualRouterOfferingInventory
import org.zstack.sdk.VirtualRouterVmInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.VipUseForList
import org.zstack.utils.data.SizeUnit
/**
 * @author: zhanyong.miao
 * @date: 2020-04-03
 * */
class OperateLoadBalancerCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
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
                    attachL2Network("l2-1")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

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

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING]
                        }

                        ip {
                            startIp = "12.168.0.3"
                            endIp = "12.168.1.200"
                            netmask = "255.255.0.0"
                            gateway = "12.168.0.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

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
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-2"
                useImage("image")
                useL3Networks("l3-2")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            /* case :
    * vip: create/delete
    *  offering: flat attach/detach offering
    *  lb pub-flat: create, getnics, attachnics, detach nics, delete listener
    *  lb flat 1 - flat 2: create, getnics, attachnics, detach nics, delete listener
    *  lb flat 1 - flat 1:  create, getnics, attachnics, detach nics, delete listener
    *  qos
    *  multi-lb base on same/different vip, attach a same backend network
    *  lb1 flat 1 - flat 2 & lb2 flat 2 - flat1  both work
    * */
            testCreateFlatVipCase()
            testAttachFlatLbOfferCase()
            testOperatePubLbCase()
            testOperateFlatLbCase()
            testOperateSameFlatLbCase()
            testOperateQosFlatLbCase()
            testOperateMultiLbCase()
            testCreate2FlatLbCase()
        }
    }


    /*
    vip: create/delete
     */
    private void testCreateFlatVipCase() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        VipInventory vip = createVip {
            name = "flat-vip"
            l3NetworkUuid = l3.uuid
        }

        deleteVip {
            uuid = vip.uuid
        }
    }

    private String attachOffering2L3(String offerUuid, String L3Uuid) {
        SystemTagInventory tag = createSystemTag {
            tag = "virtualRouterOffering::" + offerUuid
            resourceType = L3NetworkVO.simpleName
            resourceUuid = L3Uuid
        }
        return tag.uuid
    }

    private void detachOfferingFromL3(String resourceUuid) {
        deleteTag {
            uuid = resourceUuid
        }
    }

    /*
    offering: flat attach/detach offering
     */
    private void testAttachFlatLbOfferCase() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def l3_2 = env.inventoryByName("l3-2") as L3NetworkInventory
        def pubOffer = env.inventoryByName("vro") as VirtualRouterOfferingInventory
        SystemTagInventory tag1 = createSystemTag {
            tag = "guestL3Network::" + l3.uuid
            resourceType = InstanceOfferingVO.class.simpleName
            resourceUuid = pubOffer.uuid
        }

        VirtualRouterOfferingInventory flatOffer = createVirtualRouterOffering {
            delegate.name = "flat-vr"
            delegate.memorySize = SizeUnit.MEGABYTE.toByte(512)
            delegate.cpuNum = 2
            delegate.managementNetworkUuid = pubOffer.managementNetworkUuid
            delegate.publicNetworkUuid = l3.uuid
            delegate.imageUuid = pubOffer.imageUuid
            delegate.zoneUuid = pubOffer.zoneUuid
        }

        SystemTagInventory tag2 = createSystemTag {
            tag = "virtualRouterOffering::" + flatOffer.uuid
            resourceType = L3NetworkVO.simpleName
            resourceUuid = l3_2.uuid
        }

        deleteInstanceOffering {
            uuid = flatOffer.uuid
        }
        deleteTag {
            uuid = tag1.uuid
        }
        deleteTag {
            uuid = tag2.uuid
        }
    }

    /*
    lb pub-flat: create, getnics, attachnics, detach nics, delete listener
     */
    private void testOperatePubLbCase() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def publ3 = env.inventoryByName("pubL3") as L3NetworkInventory
        def pubOffer = env.inventoryByName("vro") as VirtualRouterOfferingInventory

        String resource = attachOffering2L3(pubOffer.uuid, l3.uuid)

        VipInventory vip = createVip {
            name = "flat-vip"
            l3NetworkUuid = publ3.uuid
        }

        /* create loadbalancer on this vip */
        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-lb"
            vipUuid = vip.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.LB_NETWORK_SERVICE_TYPE
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 0

        def listener = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            name = "listener"
            instancePort = 22
            loadBalancerPort = 22
            protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
        } as LoadBalancerListenerInventory

        def result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        } as List<VmNicInventory>
        assert result.size() == 1
        assert result.get(0).l3NetworkUuid == l3.uuid

        String nicUuid = result.get(0).uuid
        addVmNicToLoadBalancer {
            vmNicUuids = [nicUuid]
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 1

        result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        }
        assert result.size() == 0

        removeVmNicFromLoadBalancer {
            vmNicUuids = [nicUuid]
            listenerUuid = listener.uuid
        }
        deleteLoadBalancer {
            uuid = lb.uuid
        }
        detachOfferingFromL3(resource)
        VirtualRouterVmInventory vr = queryVirtualRouterVm {}[0]
        destroyVmInstance {
            uuid = vr.uuid
        }
    }

    /*
    lb flat 1 - flat 2: create, getnics, attachnics, detach nics, delete listener
     */
    private void testOperateFlatLbCase() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def l3_2 = env.inventoryByName("l3-2") as L3NetworkInventory
        def pubOffer = env.inventoryByName("vro") as VirtualRouterOfferingInventory

        VirtualRouterOfferingInventory flatOffer = createVirtualRouterOffering {
            delegate.name = "flat-vr"
            delegate.memorySize = SizeUnit.MEGABYTE.toByte(512)
            delegate.cpuNum = 2
            delegate.managementNetworkUuid = pubOffer.managementNetworkUuid
            delegate.publicNetworkUuid = l3_2.uuid
            delegate.imageUuid = pubOffer.imageUuid
            delegate.zoneUuid = pubOffer.zoneUuid
        }

        String resource = attachOffering2L3(flatOffer.uuid, l3.uuid)

        VipInventory vip = createVip {
            name = "flat-vip"
            l3NetworkUuid = l3_2.uuid
        }

        /* create loadbalancer on this vip */
        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-flat-lb"
            vipUuid = vip.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.LB_NETWORK_SERVICE_TYPE
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 0

        def listener = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            name = "listener"
            instancePort = 22
            loadBalancerPort = 22
            protocol = LoadBalancerConstants.LB_PROTOCOL_UDP
        } as LoadBalancerListenerInventory

        def result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        } as List<VmNicInventory>
        assert result.size() == 1
        assert result.get(0).l3NetworkUuid == l3.uuid

        String nicUuid = result.get(0).uuid
        addVmNicToLoadBalancer {
            vmNicUuids = [nicUuid]
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 1

        result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        }
        assert result.size() == 0

        removeVmNicFromLoadBalancer {
            vmNicUuids = [nicUuid]
            listenerUuid = listener.uuid
        }
        deleteLoadBalancer {
            uuid = lb.uuid
        }
        detachOfferingFromL3(resource)

        deleteInstanceOffering {
            uuid = flatOffer.uuid
        }
        VirtualRouterVmInventory vr = queryVirtualRouterVm {}[0]
        destroyVmInstance {
            uuid = vr.uuid
        }
    }

    /*
    *  lb flat 1 - flat 1:  create, getnics, attachnics, detach nics, delete listener
     */
    private void testOperateSameFlatLbCase() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def l3_2 = env.inventoryByName("l3-2") as L3NetworkInventory
        def pubOffer = env.inventoryByName("vro") as VirtualRouterOfferingInventory

        VirtualRouterOfferingInventory flatOffer = createVirtualRouterOffering {
            delegate.name = "flat-vr"
            delegate.memorySize = SizeUnit.MEGABYTE.toByte(512)
            delegate.cpuNum = 2
            delegate.managementNetworkUuid = pubOffer.managementNetworkUuid
            delegate.publicNetworkUuid = l3_2.uuid
            delegate.imageUuid = pubOffer.imageUuid
            delegate.zoneUuid = pubOffer.zoneUuid
        }

        String resource = attachOffering2L3(flatOffer.uuid, l3.uuid)

        VipInventory vip = createVip {
            name = "flat-vip"
            l3NetworkUuid = l3.uuid
        }

        /* create loadbalancer on this vip */
        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-flat-lb"
            vipUuid = vip.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.LB_NETWORK_SERVICE_TYPE
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 0

        def listener = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            name = "listener"
            instancePort = 22
            loadBalancerPort = 22
            protocol = LoadBalancerConstants.LB_PROTOCOL_UDP
        } as LoadBalancerListenerInventory

        def result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        } as List<VmNicInventory>
        assert result.size() == 1
        assert result.get(0).l3NetworkUuid == l3.uuid

        String nicUuid = result.get(0).uuid
        addVmNicToLoadBalancer {
            vmNicUuids = [nicUuid]
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 1

        result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        }
        assert result.size() == 0

        removeVmNicFromLoadBalancer {
            vmNicUuids = [nicUuid]
            listenerUuid = listener.uuid
        }
        deleteLoadBalancer {
            uuid = lb.uuid
        }
        detachOfferingFromL3(resource)

        deleteInstanceOffering {
            uuid = flatOffer.uuid
        }

        /* clear the router*/
        VirtualRouterVmInventory vr = queryVirtualRouterVm {}[0]
        destroyVmInstance {
            uuid = vr.uuid
        }
    }

    /*
    *  qos
     */
    private void testOperateQosFlatLbCase() {

    }

    /*
    *  multi-lb base on same/different vip, attach a same backend network
     */
    private void testOperateMultiLbCase() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def l3_2 = env.inventoryByName("l3-2") as L3NetworkInventory
        def pubOffer = env.inventoryByName("vro") as VirtualRouterOfferingInventory

        VirtualRouterOfferingInventory flatOffer = createVirtualRouterOffering {
            delegate.name = "flat-vr"
            delegate.memorySize = SizeUnit.MEGABYTE.toByte(512)
            delegate.cpuNum = 2
            delegate.managementNetworkUuid = pubOffer.managementNetworkUuid
            delegate.publicNetworkUuid = l3_2.uuid
            delegate.imageUuid = pubOffer.imageUuid
            delegate.zoneUuid = pubOffer.zoneUuid
        }

        String resource = attachOffering2L3(flatOffer.uuid, l3.uuid)

        VipInventory vip = createVip {
            name = "flat-vip"
            l3NetworkUuid = l3_2.uuid
        }

        VipInventory vip2 = createVip {
            name = "flat-vip"
            l3NetworkUuid = l3_2.uuid
        }

        /* create loadbalancer on this vip */
        LoadBalancerInventory lb1 = createLoadBalancer {
            name = "test-flat-lb1"
            vipUuid = vip.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.LB_NETWORK_SERVICE_TYPE
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 0

        LoadBalancerInventory lb2 = createLoadBalancer {
            name = "test-flat-lb2"
            vipUuid = vip.uuid
        }

        LoadBalancerInventory lb3 = createLoadBalancer {
            name = "test-flat-lb3"
            vipUuid = vip2.uuid
        }

        Integer port = 22
        for (LoadBalancerInventory lb : [lb1,lb2,lb3]) {
            def listener = createLoadBalancerListener {
                loadBalancerUuid = lb.uuid
                name = "listener"
                instancePort = port
                loadBalancerPort = port
                protocol = LoadBalancerConstants.LB_PROTOCOL_UDP
            } as LoadBalancerListenerInventory

            def result = getCandidateVmNicsForLoadBalancer {
                listenerUuid = listener.uuid
            } as List<VmNicInventory>
            assert result.size() == 1
            assert result.get(0).l3NetworkUuid == l3.uuid

            String nicUuid = result.get(0).uuid
            addVmNicToLoadBalancer {
                vmNicUuids = [nicUuid]
                listenerUuid = listener.uuid
            }
            assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 1

            result = getCandidateVmNicsForLoadBalancer {
                listenerUuid = listener.uuid
            }
            assert result.size() == 0

            port = port + 1
        }

        deleteLoadBalancer {
            uuid = lb1.uuid
        }

        deleteLoadBalancer {
            uuid = lb2.uuid
        }

        deleteLoadBalancer {
            uuid = lb3.uuid
        }

        detachOfferingFromL3(resource)

        deleteInstanceOffering {
            uuid = flatOffer.uuid
        }

        /* clear the router*/
        VirtualRouterVmInventory vr = queryVirtualRouterVm {}[0]
        destroyVmInstance {
            uuid = vr.uuid
        }
    }

    /*
    *  lb1 flat 1 - flat 2 & lb2 flat 2 - flat1  both work
     */
    private void testCreate2FlatLbCase() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def l3_2 = env.inventoryByName("l3-2") as L3NetworkInventory
        def pubOffer = env.inventoryByName("vro") as VirtualRouterOfferingInventory

        VirtualRouterOfferingInventory flatOffer1 = createVirtualRouterOffering {
            delegate.name = "flat-vr"
            delegate.memorySize = SizeUnit.MEGABYTE.toByte(512)
            delegate.cpuNum = 2
            delegate.managementNetworkUuid = pubOffer.managementNetworkUuid
            delegate.publicNetworkUuid = l3.uuid
            delegate.imageUuid = pubOffer.imageUuid
            delegate.zoneUuid = pubOffer.zoneUuid
        }

        VirtualRouterOfferingInventory flatOffer2 = createVirtualRouterOffering {
            delegate.name = "flat-vr"
            delegate.memorySize = SizeUnit.MEGABYTE.toByte(512)
            delegate.cpuNum = 2
            delegate.managementNetworkUuid = pubOffer.managementNetworkUuid
            delegate.publicNetworkUuid = l3_2.uuid
            delegate.imageUuid = pubOffer.imageUuid
            delegate.zoneUuid = pubOffer.zoneUuid
        }

        attachOffering2L3(flatOffer2.uuid, l3.uuid)
        attachOffering2L3(flatOffer1.uuid, l3_2.uuid)

        VipInventory vip1 = createVip {
            name = "flat-vip"
            l3NetworkUuid = l3.uuid
        }

        VipInventory vip2 = createVip {
            name = "flat-vip"
            l3NetworkUuid = l3_2.uuid
        }

        for (VipInventory vip: [vip1, vip2]) {
            /* create loadbalancer on this vip */
            LoadBalancerInventory lb = createLoadBalancer {
                name = "test-flat-lb"
                vipUuid = vip.uuid
            }
            assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).count() == 1
            assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).select(VipNetworkServicesRefVO_.serviceType).findValue() == VipUseForList.LB_NETWORK_SERVICE_TYPE
            assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 0

            def listener = createLoadBalancerListener {
                loadBalancerUuid = lb.uuid
                name = "listener"
                instancePort = 22
                loadBalancerPort = 22
                protocol = LoadBalancerConstants.LB_PROTOCOL_UDP
            } as LoadBalancerListenerInventory

            def result = getCandidateVmNicsForLoadBalancer {
                listenerUuid = listener.uuid
            } as List<VmNicInventory>
            assert result.size() == 2

            String nicUuid = result.find{ nic -> nic.l3NetworkUuid != vip.uuid }.uuid
            addVmNicToLoadBalancer {
                vmNicUuids = [nicUuid]
                listenerUuid = listener.uuid
            }
            assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 1

            result = getCandidateVmNicsForLoadBalancer {
                listenerUuid = listener.uuid
            }
            assert result.size() == 0

            removeVmNicFromLoadBalancer {
                vmNicUuids = [nicUuid]
                listenerUuid = listener.uuid
            }
            deleteLoadBalancer {
                uuid = lb.uuid
            }
        }
    }


    @Override
    void clean() {
        env.delete()
    }
}
