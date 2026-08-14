package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend


class LoadBalancerServerGroupLifeCycleCase extends SubCase{
    DatabaseFacade dbf
    EnvSpec env
    LoadBalancerServerGroupInventory servergroup1
    LoadBalancerServerGroupInventory servergroup2
    LoadBalancerServerGroupInventory servergroup3
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {

            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 1
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

                lb {
                    name = "lb"
                    useVip("pubL3")

                    listener {
                        name = "listener-22"
                        protocol = "tcp"
                        loadBalancerPort = 22
                        instancePort = 22
                    }

                    listener {
                        name = "listener-33"
                        protocol = "tcp"
                        loadBalancerPort = 33
                        instancePort = 33
                    }

                    listener {
                        name = "listener-44"
                        protocol = "tcp"
                        loadBalancerPort = 44
                        instancePort = 44
                    }
                }
            }

            vm {
                name = "vm-1"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-2"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-3"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-4"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-5"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            createServerGroup()
            TestUpdateLoadBalancerServerGroup()
            TestServerGroupWithBackendServer()
            TestDeleteLoadBalancerServerGroup()
        }
    }

    void createServerGroup(){
        def lb = env.inventoryByName("lb") as LoadBalancerInventory

        servergroup1 = createLoadBalancerServerGroup{
            loadBalancerUuid =  lb.uuid
            name = "lb-group-1"
        }
        assert servergroup1.name == "lb-group-1"
        LoadBalancerServerGroupInventory servergroup = queryLoadBalancerServerGroup { conditions = ["uuid=${servergroup1.uuid}".toString()]}[0]
        assert servergroup.name == "lb-group-1"

        servergroup2 = createLoadBalancerServerGroup {
            loadBalancerUuid =  lb.uuid
            name = "lb-group-2"
        }
        assert servergroup2.name == "lb-group-2"

        servergroup3 = createLoadBalancerServerGroup {
            loadBalancerUuid =  lb.uuid
            name = "null-group"
        }


    }

    void TestUpdateLoadBalancerServerGroup(){
        updateLoadBalancerServerGroup {
            uuid = servergroup1.uuid
            name = "updated name"
        }
        LoadBalancerServerGroupInventory servergroup = queryLoadBalancerServerGroup { conditions = ["uuid=${servergroup1.uuid}".toString()]}[0]
        assert servergroup.name == "updated name"
    }

    void TestServerGroupWithBackendServer(){
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def lbl1 = env.inventoryByName("listener-22") as LoadBalancerListenerInventory

        VmInstanceInventory vm1 = queryVmInstance {conditions = ["name=vm-1"]} [0]
        VmNicInventory nic1 = vm1.vmNics.get(0)

        VmInstanceInventory vm2 = queryVmInstance {conditions = ["name=vm-2"]} [0]
        VmNicInventory nic2 = vm2.vmNics.get(0)

        VmInstanceInventory vm3 = queryVmInstance {conditions = ["name=vm-3"]} [0]
        VmNicInventory nic3 = vm3.vmNics.get(0)

        VmInstanceInventory vm4 = queryVmInstance {conditions = ["name=vm-4"]} [0]
        VmNicInventory nic4 = vm4.vmNics.get(0)

        /* shared load balancer can not add server group */
        expect(AssertionError.class) {
            addBackendServerToServerGroup {
                vmNics = [['uuid':nic1.uuid,'weight':'20']]
                servers  = [['ipAddress':"20.20.20.1",'weight':'30']]
                serverGroupUuid = servergroup1.uuid
            }
        }
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'10']]
            serverGroupUuid = servergroup1.uuid
        }
        LoadBalancerServerGroupInventory servergroup = queryLoadBalancerServerGroup{ conditions = ["uuid=${servergroup1.uuid}".toString()]}[0]
        assert servergroup.vmNicRefs.size() == 1
        assert servergroup.vmNicRefs.get(0).vmNicUuid == nic1.uuid
        assert servergroup.vmNicRefs[0].weight == 10

        /* shared load balancer can not change server ip  */
        expect(AssertionError.class) {
            changeLoadBalancerBackendServer {
                vmNics = [['uuid': nic1.uuid, 'weight': '40']]
                servers = [['ipAddress': "20.20.20.1", 'weight': '50']]
                serverGroupUuid = servergroup1.uuid
            }
        }

        changeLoadBalancerBackendServer {
            vmNics = [['uuid': nic1.uuid, 'weight': '40']]
            serverGroupUuid = servergroup1.uuid
        }

        changeLoadBalancerBackendServer {
            vmNics = [['uuid': nic1.uuid, 'weight': '40']]
            serverGroupUuid = servergroup1.uuid
        }

        expect(AssertionError.class) {
            changeLoadBalancerBackendServer {
                serverGroupUuid = servergroup1.uuid
            }
        }

        addBackendServerToServerGroup {
            vmNics = [['uuid':nic2.uuid,'weight':'20'],['uuid':nic3.uuid,'weight':'30']]
            serverGroupUuid = servergroup1.uuid
        }

        expect(AssertionError.class) {
            addBackendServerToServerGroup {
                vmNics = [['uuid':vm1.uuid,'weight':'20']]
                serverGroupUuid = servergroup1.uuid
            }
        }

        addServerGroupToLoadBalancerListener {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup1.uuid
        }

        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'10'], ['uuid':nic4.uuid,'weight':'40']]
            serverGroupUuid = servergroup2.uuid
        }

        /* nic1, has been added to server group1 and server group2, so server group2 can not be added to listener1 */
        expect(AssertionError.class) {
            addServerGroupToLoadBalancerListener {
                listenerUuid = lbl1.uuid
                serverGroupUuid = servergroup2.uuid
            }
        }

        removeBackendServerFromServerGroup {
            serverGroupUuid = servergroup2.uuid
            vmNicUuids = [nic1.uuid]
        }

        addServerGroupToLoadBalancerListener {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup2.uuid
        }
        /* nic1 has been added to server group1 which is attached to listener1, so nic1 can not be added to server group2 */
        expect(AssertionError.class) {
            addBackendServerToServerGroup {
                vmNics = [['uuid':nic1.uuid]]
                serverGroupUuid = servergroup2.uuid
            }
        }

        /* delete server group refresh backend */
        removeServerGroupFromLoadBalancerListener {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup1.uuid
        }

        servergroup = queryLoadBalancerServerGroup{ conditions = ["uuid=${servergroup1.uuid}".toString()]}[0]
        assert servergroup.vmNicRefs.size() == 3

        removeBackendServerFromServerGroup{
            vmNicUuids = [nic1.uuid]
            serverGroupUuid = servergroup1.uuid
        }
        servergroup = queryLoadBalancerServerGroup{ conditions = ["uuid=${servergroup1.uuid}".toString()]}[0]
        assert servergroup.vmNicRefs.size() == 2


        def lbl3 = env.inventoryByName("listener-44") as LoadBalancerListenerInventory
        addServerGroupToLoadBalancerListener {
            listenerUuid = lbl3.uuid
            serverGroupUuid = servergroup3.uuid
        }
        // can remove servergroup which don not have vmnic and serverip
        removeServerGroupFromLoadBalancerListener {
            listenerUuid = lbl3.uuid
            serverGroupUuid = servergroup3.uuid
        }




    }

    void TestDeleteLoadBalancerServerGroup(){
        deleteLoadBalancerServerGroup{
            uuid = servergroup1.uuid
        }

        def lb = env.inventoryByName("lb") as LoadBalancerInventory

        expect(AssertionError.class) {
            deleteLoadBalancerServerGroup {
                uuid = lb.serverGroupUuid
            }
        }

        def lbl2 = env.inventoryByName("listener-33") as LoadBalancerListenerInventory

        VmInstanceInventory vm5 = queryVmInstance {conditions = ["name=vm-5"]} [0]
        VmNicInventory nic5 = vm5.vmNics.get(0)

        lbl2 =  addVmNicToLoadBalancer {
            vmNicUuids = [nic5.uuid]
            listenerUuid = lbl2.uuid
        }

        assert lbl2.vmNicRefs[0].vmNicUuid == nic5.uuid


        removeVmNicFromLoadBalancer {
            vmNicUuids = [nic5.uuid]
            listenerUuid = lbl2.uuid
        }
        lbl2 = queryLoadBalancerListener { conditions = ["uuid=${lbl2.uuid}".toString()]}[0]
        assert lbl2.vmNicRefs.size()==0

        deleteLoadBalancerServerGroup {
            uuid = lbl2.serverGroupUuid
        }

    }


    @Override
    void clean() {
        env.delete()
    }


}