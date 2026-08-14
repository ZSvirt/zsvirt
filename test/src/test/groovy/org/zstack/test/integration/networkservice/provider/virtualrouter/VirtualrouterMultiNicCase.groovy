package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SimpleQuery
import org.zstack.header.network.service.NetworkServiceProviderVO
import org.zstack.header.network.service.NetworkServiceProviderVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmNicParam
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg
import org.zstack.network.securitygroup.SecurityGroupRuleProtocolType
import org.zstack.network.securitygroup.SecurityGroupRuleType
import org.zstack.network.securitygroup.VmNicSecurityGroupRefInventory
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_
import org.zstack.sdk.*
import org.zstack.network.service.vip.*
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefVO
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefVO_
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.VipUseForList
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.utils.CollectionDSL.list
/**
 * @author: zhanyong.miao
 * @date: 2019-08-06
 * */
class VirtualrouterMultiNicCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    int MAX_NIC_COUNT = 12

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        // This environment contains vr-offering but no VM.
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnServicesEnv()
    }

    @Override
    void test() {
        env.create {
            /*
            1. prepare testbed 10 port
            2. global configure case
            3. EIP, attach,detach, GetCandidateVmNics, delete
            4. lB, attach,detach, GetCandidateVmNics, delete
            5. PF, attach,detach, GetCandidateVmNics, delete
            6. SG
             */
            dbf = bean(DatabaseFacade.class)
            testMultiNicConfig()
            prepareTestBed()
            testMultiNicDHCPFlatCase()
            testMultiNicDHCPVyosCase()
            testMultiNicEipCase()
            testMultiNicLBCase()
            testMultiNicPFCase()
            testMultiNicSGCase()
        }
    }

    private VmNicInventory getEarliestNic(List<VmNicInventory> vmNics, String networkUuid) {
        VmNicInventory earliestNic = null

        for (VmNicInventory nic : vmNics) {
            if (!nic.l3NetworkUuid.equals(networkUuid)) {
                continue
            }
            if (earliestNic == null || earliestNic.getCreateDate().after(nic.getCreateDate()) ) {
                earliestNic = nic
            } else if (earliestNic.getCreateDate().equals(nic.getCreateDate()) && earliestNic.getDeviceId() > nic.getDeviceId()) {
                earliestNic = nic;
            }
        }
        return earliestNic
    }
    void testMultiNicDHCPVyosCase() {
        /*
        * 1. add the DHCP service with vyos provider
        * 2. attach defalult l3 to vm
        * 3. verify just only one nic is default nic in dhcp cmd
        * */
        L3NetworkInventory l3 = queryL3Network {
            conditions = ["name=l3"]
        }[0]

        VmInstanceInventory vm = queryVmInstance {
            conditions = ["name=vm"]
        }[0]

        VmNicInventory earliestNic = getEarliestNic(vm.vmNics, vm.defaultL3NetworkUuid)

        Map<String, List<String>> services = new HashMap<String, List<String>>()
        for (NetworkServiceL3NetworkRefInventory ref : l3.getNetworkServices()) {
            List<String> types = services.get(ref.getNetworkServiceProviderUuid())
            if (types == null) {
                types = new ArrayList<String>()
            }
            if ("DHCP".equals(ref.networkServiceType)) {
                types.add(ref.getNetworkServiceType())
                services.put(ref.getNetworkServiceProviderUuid(), types)
            }
        }

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = services
        }

        SimpleQuery<NetworkServiceProviderVO> q = dbf.createQuery(NetworkServiceProviderVO.class);
        q.add(NetworkServiceProviderVO_.type, SimpleQuery.Op.EQ, VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
        NetworkServiceProviderVO vo = q.find();

        Map<String, List<String>> ntypes = new HashMap<String, List<String>>(1)
        ntypes.put(vo.uuid, list(NetworkServiceType.DHCP.toString()))
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = ntypes
        }

        VirtualRouterCommands.RefreshDHCPServerCmd cmd = null
        env.afterSimulator(VirtualRouterConstant.VR_REFRESH_DHCP_SERVER_PATH) { VirtualRouterCommands.RefreshDHCPServerRsp rsp, HttpEntity<String> e ->
            cmd = json(e.body, VirtualRouterCommands.RefreshDHCPServerCmd.class)
            assert !cmd.dhcpServers.isEmpty()
            /*if (cmd.dhcpServers.get(0).dhcpInfos.stream().filter({dhcp -> dhcp.ip.equals(earliestNic.ip)}).count()>0) {
                assert cmd.dhcpServers.get(0).dhcpInfos.stream().filter({ dhcp -> dhcp.isDefaultL3Network }).count() == 1
                assert cmd.dhcpServers.get(0).dhcpInfos.stream().filter({ dhcp -> dhcp.isDefaultL3Network && dhcp.ip.equals(earliestNic.ip) }).count() == 1
            }*/
            return rsp
        }

        def l3_pub = env.inventoryByName("pubL3") as L3NetworkInventory
        def vr = queryVmInstance { conditions=["type=ApplianceVm"] }[0] as VmInstanceInventory

        rebootVmInstance {
            uuid = vr.uuid
        }

        assert null != cmd
    }

    void testMultiNicDHCPFlatCase() {
        /*
        * 1. add the DHCP service with flat provider
        * 2. attach defalult l3 to vm
        * 3. verify just only one nic is default nic in dhcp cmd
        * */
        L3NetworkInventory l3 = queryL3Network { conditions = ["name=l3"] }[0]
        VmInstanceInventory vm = queryVmInstance {
            conditions = ["name=vm"]
        }[0]

        VmNicInventory earliestNic = getEarliestNic(vm.vmNics, vm.defaultL3NetworkUuid)
        assert earliestNic != null

        Map<String, List<String>> services = new HashMap<String, List<String>>()
        for (NetworkServiceL3NetworkRefInventory ref : l3.getNetworkServices()) {
            List<String> types = services.get(ref.getNetworkServiceProviderUuid())

            if (types == null) {
                types = new ArrayList<String>()
            }
            if ("DHCP".equals(ref.networkServiceType)) {
                types.add(ref.getNetworkServiceType())
                services.put(ref.getNetworkServiceProviderUuid(), types)
            }
        }

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = services
        }

        SimpleQuery<NetworkServiceProviderVO> q = dbf.createQuery(NetworkServiceProviderVO.class);
        q.add(NetworkServiceProviderVO_.type, SimpleQuery.Op.EQ, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        NetworkServiceProviderVO vo = q.find();

        Map<String, List<String>> ntypes = new HashMap<String, List<String>>(1)
        ntypes.put(vo.uuid, list(NetworkServiceType.DHCP.toString()))
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = ntypes
        }

        FlatDhcpBackend.ApplyDhcpCmd acmd
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.BatchApplyDhcpCmd bcmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            assert bcmd.dhcpInfos.size() == 1
            acmd = bcmd.dhcpInfos.get(0)
            assert !acmd.dhcp.isEmpty()
            if (acmd.dhcp.stream().filter({dhcp -> dhcp.ip.equals(earliestNic.ip)}).count()>0) {
                assert acmd.dhcp.stream().filter({ dhcp -> dhcp.isDefaultL3Network }).count() == 1
                assert acmd.dhcp.stream().filter({ dhcp -> dhcp.isDefaultL3Network && dhcp.ip.equals(earliestNic.ip) }).count() == 1
            }
            return rsp
        }

        HostInventory host = env.inventoryByName("kvm")

        reconnectHost {
            uuid = host.uuid
        }

        assert null != acmd
    }

    void testMultiNicSGCase() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        def sg = createSecurityGroup{
            name = "sg"
        } as SecurityGroupInventory

        attachSecurityGroupToL3Network {
                securityGroupUuid = sg.uuid
                l3NetworkUuid = l3.uuid
        }

        def vmNics = getCandidateVmNicForSecurityGroup { securityGroupUuid = sg.uuid } as List<VmNicInventory>
        assert MAX_NIC_COUNT == vmNics.size()

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO rule = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO()
        rule.allowedCidr = "192.168.0.1/24"
        rule.type = SecurityGroupRuleType.Ingress.toString()
        rule.protocol = SecurityGroupRuleProtocolType.TCP.toString()
        rule.startPort = 100
        rule.endPort = 200

        addSecurityGroupRule {
            delegate.securityGroupUuid = sg.uuid
            delegate.rules = [rule]
        }

        addVmNicToSecurityGroup{
            securityGroupUuid = sg.uuid
            vmNicUuids = vmNics.collect {it.uuid}
        }

        def vmNicsInSg = queryVmNicInSecurityGroup{ conditions=[String.format("securityGroupUuid=%s", sg.uuid)] } as List<VmNicSecurityGroupRefInventory>
        assert MAX_NIC_COUNT == vmNicsInSg.size()
        assert vmNicsInSg.collect{it.vmNicUuid}.toSet() == vmNics.collect{it.uuid}.toSet()

        vmNics = getCandidateVmNicForSecurityGroup { securityGroupUuid = sg.uuid } as List<VmNicInventory>
        assert vmNics == null || vmNics.isEmpty()

        deleteVmNicFromSecurityGroup{
            securityGroupUuid = sg.uuid
            vmNicUuids = vmNicsInSg.collect {it.vmNicUuid}
        }

        deleteSecurityGroup {
            uuid = sg.uuid
        }
    }
    
    void testMultiNicPFCase() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        def vm = queryVmInstance {
            conditions = ["name=vm"]
        }[0] as VmInstanceInventory
        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = pubL3.uuid
        }

        PortForwardingRuleInventory portForwarding = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 22
            vipPortEnd = 22
            privatePortEnd = 100
            privatePortStart = 100
            protocolType = PortForwardingProtocolType.TCP.toString()
        }

        GetVipAvailablePortAction getVipAvailablePortAction = new GetVipAvailablePortAction()
        getVipAvailablePortAction.vipUuid = vip.uuid
        getVipAvailablePortAction.protocolType = "TCP"
        getVipAvailablePortAction.start = 20
        getVipAvailablePortAction.limit = 5
        getVipAvailablePortAction.sessionId = adminSession()
        GetVipAvailablePortAction.Result getRes = getVipAvailablePortAction.call()
        assert getRes.error == null
        List<Integer> availablePort = getRes.value.availablePort
        assert !availablePort.contains(22.0)         

        CheckVipPortAvailabilityAction checkVipPortAvailabilityAction = new CheckVipPortAvailabilityAction()
        checkVipPortAvailabilityAction.vipUuid = vip.uuid
        checkVipPortAvailabilityAction.port = 22
        checkVipPortAvailabilityAction.protocolType = "UDP"
        checkVipPortAvailabilityAction.sessionId = adminSession()
        CheckVipPortAvailabilityAction.Result chkRes = checkVipPortAvailabilityAction.call()
        assert chkRes.error == null
        boolean avail = chkRes.value.available
        assert avail

        CheckVipPortAvailabilityAction checkVipPortAvailabilityAction1 = new CheckVipPortAvailabilityAction()
        checkVipPortAvailabilityAction1.vipUuid = vip.uuid
        checkVipPortAvailabilityAction1.port = 22
        checkVipPortAvailabilityAction1.protocolType = "TCP"
        checkVipPortAvailabilityAction1.sessionId = adminSession()
        CheckVipPortAvailabilityAction.Result chkRes1 = checkVipPortAvailabilityAction1.call()
        assert chkRes1.error == null
        boolean avail1 = chkRes1.value.available
        assert !avail1

        List<VmNicInventory> vmNics = getPortForwardingAttachableVmNics {
            ruleUuid = portForwarding.uuid
        }
        assert MAX_NIC_COUNT == vmNics.size()

        def cmd = null
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING){ rsp, HttpEntity<String> entity ->
            cmd = json(entity.body,VirtualRouterCommands.CreatePortForwardingRuleCmd.class)
            return rsp
        }

        attachPortForwardingRule {
            vmNicUuid = vm.getVmNics().get(0).uuid
            ruleUuid = portForwarding.uuid
        }
        assert cmd != null

        vmNics = getPortForwardingAttachableVmNics {
            ruleUuid = portForwarding.uuid
        }
        assert vmNics.isEmpty()

        VirtualRouterCommands.RemoveVipCmd removeVipCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_VIP) { VirtualRouterCommands.RemoveVipRsp rsp, HttpEntity<String> e ->
            removeVipCmd = JSONObjectUtil.toObject(e.body,  VirtualRouterCommands.RemoveVipCmd.class)
            return rsp
        }

        VirtualRouterVmInventory vr = queryVirtualRouterVm {}[0]
        detachPortForwardingRule {
            uuid = portForwarding.uuid
        }
        assert removeVipCmd != null

        vmNics = getPortForwardingAttachableVmNics {
            ruleUuid = portForwarding.uuid
        }
        assert MAX_NIC_COUNT == vmNics.size()

        deletePortForwardingRule {
            uuid = portForwarding.uuid
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 0
        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1

        deleteVip {
            uuid = vip.uuid
        }
        assert !Q.New(VipVO.class).eq(VipVO_.uuid, vip.uuid).isExists()
        assert Q.New(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.uuid, portForwarding.uuid).select(PortForwardingRuleVO_.guestIp).findValue() == null
        assert Q.New(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.uuid, portForwarding.uuid).select(PortForwardingRuleVO_.vmNicUuid).findValue() == null
        assert Q.New(VirtualRouterPortForwardingRuleRefVO.class).eq(VirtualRouterPortForwardingRuleRefVO_.uuid, portForwarding.uuid).list().size() == 0
    }

    void testMultiNicLBCase() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def l3_pub = env.inventoryByName("pubL3") as L3NetworkInventory
        def vm = queryVmInstance {
            conditions = ["name=vm"]
        }[0] as VmInstanceInventory

        VipInventory vip = createVip {
            name = "vip-lb"
            l3NetworkUuid = l3_pub.uuid
        }

        /* create loadbalancer on this vip */
        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-lb"
            vipUuid = vip.getUuid()
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
        assert result.size() == MAX_NIC_COUNT

        addVmNicToLoadBalancer {
            vmNicUuids = [vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid]
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 1

        result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        }
        assert result.size() == (MAX_NIC_COUNT - 1)
        addVmNicToLoadBalancer {
            vmNicUuids = result.collect { it.uuid }
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 1

        removeVmNicFromLoadBalancer {
            vmNicUuids = [vm.vmNics[0].uuid ]
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 1

        removeVmNicFromLoadBalancer {
            vmNicUuids = vm.vmNics.collect { it.uuid }
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 0
        result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        }
        assert result.size() == MAX_NIC_COUNT

        addVmNicToLoadBalancer {
            vmNicUuids = [vm.vmNics.get(0).uuid]
            listenerUuid = listener.uuid
        }
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.getUuid()).count() == 1
        result = getCandidateVmNicsForLoadBalancer {
            listenerUuid = listener.uuid
        }
        assert result.size() == (MAX_NIC_COUNT - 1)

        /* delete loadbalancer */
        deleteLoadBalancer {
            uuid = lb.getUuid()
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).eq(VipNetworkServicesRefVO_.serviceType, VipUseForList.LB_NETWORK_SERVICE_TYPE).count() == 0

        deleteVip {
            uuid = vip.getUuid()
        }
        assert Q.New(VipVO.class).eq(VipVO_.uuid, vip.getUuid()).count() == 0
    }

    void testMultiNicEipCase() {
        def l3_pub = env.inventoryByName("pubL3") as L3NetworkInventory
        VipInventory vip = createVip {
            name = "vip-eip"
            l3NetworkUuid = l3_pub.uuid
        }
        def eip = createEip {
            name = "eip"
            vipUuid = vip.uuid
        } as EipInventory

        boolean called = false
        List <VmNicInventory> nics = getEipAttachableVmNics {
            eipUuid = eip.uuid
        }
        assert nics.size() == MAX_NIC_COUNT

        env.simulator(VirtualRouterConstant.VR_CREATE_EIP) {
            called = true
            return new VirtualRouterCommands.CreateEipRsp()
        }

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = nics.get(0).uuid
        }

        assert called
        nics = getEipAttachableVmNics {
            eipUuid = eip.uuid
        }
        assert nics.size() == 0

        detachEip {
            uuid = eip.uuid
        }

        nics = getEipAttachableVmNics {
            eipUuid = eip.uuid
        }
        assert nics.size() == MAX_NIC_COUNT
        deleteEip {
            uuid = eip.uuid
        }
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.getUuid()).eq(VipNetworkServicesRefVO_.serviceType, VipUseForList.EIP_NETWORK_SERVICE_TYPE).count() == 0

        deleteVip {
            uuid = vip.getUuid()
        }
        assert Q.New(VipVO.class).eq(VipVO_.uuid, vip.getUuid()).count() == 0
    }

    void prepareTestBed() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def image = env.inventoryByName("image") as ImageInventory
        def offering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = VmGlobalConfig.MULTI_VNIC_SUPPORT.name
            value = "true"
        }

        for (int i = 0; i < MAX_NIC_COUNT; i++) {
            vm = attachL3NetworkToVm {
                l3NetworkUuid = l3.uuid
                vmInstanceUuid = vm.uuid
            }
        }

        assert vm.vmNics.size() == MAX_NIC_COUNT

        List<VmNicParam> nicParams = new ArrayList<>()
        VmNicParam nicParam = new VmNicParam()
        nicParam.setL3NetworkUuid(l3.uuid)
        nicParam.setDriverType("virtio")
        nicParam.setMultiQueueNum(4)
        nicParams.add(nicParam)
        nicParams.add(nicParam)
        nicParams.add(nicParam)

        def vm2 = createVmInstance {
            name = "vm2"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid, l3.uuid, l3.uuid]
            defaultL3NetworkUuid = l3.uuid
            instanceOfferingUuid = offering.uuid
            vmNicParams = JSONObjectUtil.toJsonString(nicParams)
        } as VmInstanceInventory

        assert vm2.vmNics.size() == 3
        assert vm2.vmNics.stream().allMatch {VmNicInventory nic -> nic.ip != null}

        vm2 = attachL3NetworkToVm {
            vmInstanceUuid = vm2.uuid
            l3NetworkUuid = l3.uuid
        } as VmInstanceInventory

        assert vm2.vmNics.size() == 4

        destroyVmInstance {
            uuid = vm2.uuid
        }
    }

    void testMultiNicConfig() {
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def l3_pub = env.inventoryByName("pubL3") as L3NetworkInventory
        def vr = queryVmInstance { conditions=["type=ApplianceVm"] }[0] as VmInstanceInventory

        attachL3NetworkToVm {
            l3NetworkUuid = l3_pub.uuid
            vmInstanceUuid = vm.uuid
        }

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = VmGlobalConfig.MULTI_VNIC_SUPPORT.name
            value = "false"
        }

        expect(AssertionError.class) {
            attachL3NetworkToVm {
                l3NetworkUuid = l3.uuid
                vmInstanceUuid = vm.uuid
            }
        }

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = VmGlobalConfig.MULTI_VNIC_SUPPORT.name
            value = "true"
        }

        expect(AssertionError.class) {
            attachL3NetworkToVm {
                l3NetworkUuid = l3_pub.uuid
                vmInstanceUuid = vm.uuid
            }
        }

        expect(AssertionError.class) {
            attachL3NetworkToVm {
                l3NetworkUuid = l3.uuid
                vmInstanceUuid = vr.uuid
            }
        }

        vm = attachL3NetworkToVm {
            l3NetworkUuid = l3.uuid
            vmInstanceUuid = vm.uuid
        }

        assert vm.vmNics.size() == 3

        VmNicInventory nic = createVmNic {
            l3NetworkUuid = l3.uuid
        }

        KVMAgentCommands.NicTO nicTo;
        env.afterSimulator(KVMConstant.KVM_ATTACH_NIC_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.AttachNicCommand.class)
            if (cmd.vmUuid == vm.uuid) {
                nicTo = cmd.nic
            }
            return rsp
        }
        attachVmNicToVm {
            vmInstanceUuid = vm.uuid
            vmNicUuid = nic.uuid
        }
        assert nicTo != null && nicTo.uuid.equals(nic.uuid)

        expect(AssertionError.class) {
            updateGlobalConfig {
                category = VmGlobalConfig.CATEGORY
                name = VmGlobalConfig.MULTI_VNIC_SUPPORT.name
                value = "false"
            }
        }

        vm = detachL3NetworkFromVm {
            vmNicUuid = vm.vmNics[1].uuid
        }
        assert vm.vmNics.size() == 3

        vm = detachL3NetworkFromVm {
            vmNicUuid = vm.vmNics[1].uuid
        }
        assert vm.vmNics.size() == 2

        vm = detachL3NetworkFromVm {
            vmNicUuid = vm.vmNics[1].uuid
        }
        assert vm.vmNics.size() == 1

        updateGlobalConfig {
            category = VmGlobalConfig.CATEGORY
            name = VmGlobalConfig.MULTI_VNIC_SUPPORT.name
            value = "false"
        }
        detachL3NetworkFromVm {
            vmNicUuid = vm.vmNics[0].uuid
        }
    }
}
