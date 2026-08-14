package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class LoadBalancerServerGroupListenerCase extends SubCase{
    DatabaseFacade dbf
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
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            TestAddVmNicThenAttachListener()
            TestAttachListenerThenAddVmNic()
            TestDeleteLoadBalancer()
        }
    }

    void TestAddVmNicThenAttachListener(){
        VmInstanceInventory vm1 = env.inventoryByName("vm-1")
        VmInstanceInventory vm2 = env.inventoryByName("vm-2")
        VmInstanceInventory vm3 = env.inventoryByName("vm-3")
        LoadBalancerListenerInventory lb22 = env.inventoryByName("listener-22")
        def lb = env.inventoryByName("lb") as LoadBalancerInventory

        LoadBalancerServerGroupInventory sg = createLoadBalancerServerGroup{
            loadBalancerUuid =  lb.uuid
            name = "lb-group-1"
        }

        VirtualRouterLoadBalancerBackend.RefreshLbCmd refreshLbCmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            refreshLbCmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        /* add vm nic to server group with listener will not refresh backend */
        VmNicInventory nic1 = vm1.vmNics.get(0)
        VmNicInventory nic2 = vm2.vmNics.get(0)
        VmNicInventory nic3 = vm3.vmNics.get(0)
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'20'], ['uuid':nic2.uuid,weight:'30']]
            serverGroupUuid = sg.uuid
        }
        assert refreshLbCmd == null

        /* attach server group to listener will refresh backend */
        addServerGroupToLoadBalancerListener {
            listenerUuid = lb22.uuid
            serverGroupUuid = sg.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 1
        VirtualRouterLoadBalancerBackend.LbTO to = refreshLbCmd.lbs.get(0)
        assert to.nicIps.size() == 2
        for (String ip : to.nicIps) {
            assert nic1.ip == ip || nic2.ip == ip
        }

        /* same server group and listener can not added again */
        expect(AssertionError.class) {
            addServerGroupToLoadBalancerListener {
                listenerUuid = lb22.uuid
                serverGroupUuid = sg.uuid
            }
        }

        /* remove vmnic will refresh backend */
        refreshLbCmd = null
        removeBackendServerFromServerGroup {
            serverGroupUuid = sg.uuid
            vmNicUuids = [nic1.uuid, nic2.uuid]
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 1
        to = refreshLbCmd.lbs.get(0)
        assert to.nicIps.size() == 0

        /* add vmnic will refresh backend */
        refreshLbCmd = null
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'20']]
            serverGroupUuid = sg.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 1
        to = refreshLbCmd.lbs.get(0)
        assert to.nicIps.size() == 1
        assert to.nicIps.get(0) == nic1.ip

        LoadBalancerServerGroupInventory sg2 = createLoadBalancerServerGroup{
            loadBalancerUuid =  lb.uuid
            name = "lb-group-2"
        }

        LoadBalancerListenerInventory lb33 = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 33
            instancePort = 33
            name = "test-33"
        }

        /* attach server group without listener to listener will not refresh backend */
        refreshLbCmd = null
        addServerGroupToLoadBalancerListener {
            listenerUuid = lb33.uuid
            serverGroupUuid = sg.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 2
        for (VirtualRouterLoadBalancerBackend.LbTO t : refreshLbCmd.lbs) {
            assert t.nicIps.size() == 1
            assert t.nicIps.get(0) == nic1.ip
            assert t.listenerUuid == lb22.uuid || t.listenerUuid == lb33.uuid
        }

        refreshLbCmd = null
        addServerGroupToLoadBalancerListener {
            listenerUuid = lb33.uuid
            serverGroupUuid = sg2.uuid
        }
        assert refreshLbCmd == null
        /* same vm nic can not be attached to listener multiple time */
        expect(AssertionError.class) {
            addBackendServerToServerGroup {
                vmNics = [['uuid':nic1.uuid,'weight':'30']]
                serverGroupUuid = sg2.uuid
            }
        }
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic3.uuid,'weight':'30']]
            serverGroupUuid = sg2.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 2
        for (VirtualRouterLoadBalancerBackend.LbTO t : refreshLbCmd.lbs) {
            if (t.listenerUuid == lb33.uuid) {
                to = t
            } else {
                assert t.listenerUuid == lb22.uuid
                assert t.nicIps.size() == 1
                assert t.nicIps.get(0) == nic1.ip
            }
        }
        assert to != null
        assert to.nicIps.size() == 2
        assert to.nicIps.get(0) == nic3.ip || to.nicIps.get(0) == nic1.ip

        LoadBalancerServerGroupInventory sg3 = createLoadBalancerServerGroup{
            loadBalancerUuid =  lb.uuid
            name = "server-group-3"
        }
        /* add nic to server group which is not attached to listener will not refresh backend  */
        refreshLbCmd = null
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'30'],['uuid':nic2.uuid,'weight':'30']]
            serverGroupUuid = sg3.uuid
        }
        assert refreshLbCmd == null

        /* delete server group which is not attached to listener will not refresh backend  */
        deleteLoadBalancerServerGroup {
            uuid = sg3.uuid
        }
        assert refreshLbCmd == null

        /* delete server group which is attached to listener will refresh backend  */
        refreshLbCmd = null
        deleteLoadBalancerServerGroup {
            uuid = sg.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 2
        for (VirtualRouterLoadBalancerBackend.LbTO t : refreshLbCmd.lbs) {
            if (t.listenerUuid == lb22.uuid) {
                assert t.nicIps.size() == 0
            } else {
                assert t.nicIps.size() == 1
                assert t.nicIps.get(0) == nic3.ip
            }
        }

        /* delete server group which is attached to listener will refresh backend  */
        refreshLbCmd = null
        deleteLoadBalancerServerGroup {
            uuid = sg2.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 2
        for (VirtualRouterLoadBalancerBackend.LbTO t : refreshLbCmd.lbs) {
            assert t.nicIps.size() == 0
        }

        deleteLoadBalancerListener {
            uuid = lb22.uuid
        }

        deleteLoadBalancerListener {
            uuid = lb33.uuid
        }
    }

    void TestAttachListenerThenAddVmNic(){
        VmInstanceInventory vm1 = env.inventoryByName("vm-1")
        VmInstanceInventory vm2 = env.inventoryByName("vm-2")
        VmInstanceInventory vm3 = env.inventoryByName("vm-3")
        LoadBalancerListenerInventory lb22 = env.inventoryByName("listener-22")
        def lb = env.inventoryByName("lb") as LoadBalancerInventory

        LoadBalancerServerGroupInventory sg1 = createLoadBalancerServerGroup{
            loadBalancerUuid =  lb.uuid
            name = "server-group-1"
        }
        LoadBalancerServerGroupInventory sg2 = createLoadBalancerServerGroup{
            loadBalancerUuid =  lb.uuid
            name = "server-group-2"
        }

        VirtualRouterLoadBalancerBackend.RefreshLbCmd refreshLbCmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            refreshLbCmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        /* create listener will not refresh backend */
        LoadBalancerListenerInventory tcp80 = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 80
            instancePort = 80
            name = "tcp-80"
        }
        assert refreshLbCmd == null

        /* attach empty server group to listener will not refresh backend */
        addServerGroupToLoadBalancerListener {
            listenerUuid = tcp80.uuid
            serverGroupUuid = sg1.uuid
        }
        assert refreshLbCmd == null

        VmNicInventory nic1 = vm1.vmNics.get(0)
        VmNicInventory nic2 = vm2.vmNics.get(0)
        VmNicInventory nic3 = vm3.vmNics.get(0)
        /* add vm nic to server group will refresh backend */
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'30'],['uuid':nic2.uuid,'weight':'30']]
            serverGroupUuid = sg1.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 1
        VirtualRouterLoadBalancerBackend.LbTO to = refreshLbCmd.lbs.get(0)
        assert to.nicIps.size() == 2
        for (String ip : to.nicIps) {
            assert nic1.ip == ip || nic2.ip == ip
        }

        refreshLbCmd = null
        /* create listener will not refresh backend */
        LoadBalancerListenerInventory tcp443 = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 443
            instancePort = 80
            name = "tcp-80"
        }
        assert refreshLbCmd == null
        /* attach empty server group to listener will not refresh backend */
        addServerGroupToLoadBalancerListener {
            listenerUuid = tcp443.uuid
            serverGroupUuid = sg2.uuid
        }
        assert refreshLbCmd == null

        /* add vm nic to server group will refresh backend */
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'30'],['uuid':nic3.uuid,'weight':'30']]
            serverGroupUuid = sg2.uuid
        }
        assert refreshLbCmd != null
        assert to.nicIps.size() == 2
        for (VirtualRouterLoadBalancerBackend.LbTO t : refreshLbCmd.lbs) {
            if (t.listenerUuid == tcp80.uuid) {
                assert t.nicIps.size() == 2
                assert t.nicIps.get(0) == nic1.ip || t.nicIps.get(0) == nic2.ip
                assert t.nicIps.get(1) == nic1.ip || t.nicIps.get(1) == nic2.ip
                assert t.nicIps.get(0) != t.nicIps.get(1)
            } else {
                assert t.nicIps.size() == 2
                assert t.nicIps.get(0) == nic1.ip || t.nicIps.get(0) == nic3.ip
                assert t.nicIps.get(1) == nic1.ip || t.nicIps.get(1) == nic3.ip
                assert t.nicIps.get(0) != t.nicIps.get(1)
            }
        }

        /* create listener will not refresh backend */
        refreshLbCmd = null
        LoadBalancerListenerInventory tcp8080 = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 8080
            instancePort = 80
            name = "tcp-8080"
        }
        assert refreshLbCmd == null
        /* delete listener without server group will not refresh backend */
        deleteLoadBalancerListener {
            uuid = tcp8080.uuid
        }
        assert refreshLbCmd == null

        /* delete listener without server group will not refresh backend */
        deleteLoadBalancerListener {
            uuid = tcp80.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 2
        for (VirtualRouterLoadBalancerBackend.LbTO t : refreshLbCmd.lbs) {
            if (t.listenerUuid == tcp80.uuid) {
                assert t.nicIps.size() == 0
            } else {
                assert t.nicIps.size() == 2
                assert t.nicIps.get(0) == nic1.ip || t.nicIps.get(0) == nic3.ip
                assert t.nicIps.get(1) == nic1.ip || t.nicIps.get(1) == nic3.ip
                assert t.nicIps.get(0) != t.nicIps.get(1)
            }
        }

        /* delete listener without server group will not refresh backend */
        deleteLoadBalancerListener {
            uuid = tcp443.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 1
        for (VirtualRouterLoadBalancerBackend.LbTO t : refreshLbCmd.lbs) {
            assert t.nicIps.size() == 0
        }

        deleteLoadBalancerServerGroup {
            uuid = sg1.uuid
        }
        deleteLoadBalancerServerGroup {
            uuid = sg2.uuid
        }
    }

    void TestDeleteLoadBalancer(){
        VmInstanceInventory vm1 = env.inventoryByName("vm-1")
        VmInstanceInventory vm2 = env.inventoryByName("vm-2")
        VmInstanceInventory vm3 = env.inventoryByName("vm-3")
        LoadBalancerListenerInventory lb22 = env.inventoryByName("listener-22")
        def lb = env.inventoryByName("lb") as LoadBalancerInventory

        VmNicInventory nic1 = vm1.vmNics.get(0)
        VmNicInventory nic2 = vm2.vmNics.get(0)
        VmNicInventory nic3 = vm3.vmNics.get(0)

        LoadBalancerServerGroupInventory sg1 = createLoadBalancerServerGroup{
            loadBalancerUuid =  lb.uuid
            name = "server-group-1"
        }
        LoadBalancerServerGroupInventory sg2 = createLoadBalancerServerGroup{
            loadBalancerUuid =  lb.uuid
            name = "server-group-2"
        }

        LoadBalancerListenerInventory tcp80 = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 80
            instancePort = 80
            name = "tcp-80"
        }
        addServerGroupToLoadBalancerListener {
            listenerUuid = tcp80.uuid
            serverGroupUuid = sg1.uuid
        }
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'30'],['uuid':nic2.uuid,'weight':'30']]
            serverGroupUuid = sg1.uuid
        }

        LoadBalancerListenerInventory tcp443 = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 443
            instancePort = 80
            name = "tcp-80"
        }
        addServerGroupToLoadBalancerListener {
            listenerUuid = tcp443.uuid
            serverGroupUuid = sg2.uuid
        }
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'30'],['uuid':nic3.uuid,'weight':'30']]
            serverGroupUuid = sg2.uuid
        }

        VirtualRouterLoadBalancerBackend.RefreshLbCmd refreshLbCmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            refreshLbCmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        VirtualRouterLoadBalancerBackend.DeleteLbCmd delLbCmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.DELETE_LB_PATH) { rsp, HttpEntity<String> e ->
            delLbCmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.DeleteLbCmd.class)
            return rsp
        }
        deleteLoadBalancer {
            uuid = lb.uuid
        }
        assert refreshLbCmd == null
        assert delLbCmd != null
        assert delLbCmd.lbs.size() == 2
        assert delLbCmd.lbs.get(0).listenerUuid == tcp80.uuid || delLbCmd.lbs.get(0).listenerUuid == tcp443.uuid
        assert delLbCmd.lbs.get(1).listenerUuid == tcp80.uuid || delLbCmd.lbs.get(1).listenerUuid == tcp443.uuid
        assert delLbCmd.lbs.get(0).listenerUuid != delLbCmd.lbs.get(1).listenerUuid
    }

    @Override
    void clean() {
        env.delete()
    }


}