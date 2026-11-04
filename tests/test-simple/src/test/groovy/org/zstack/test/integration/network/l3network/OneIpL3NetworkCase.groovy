package org.zstack.test.integration.network.l3network

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.sdk.ApplianceVmInventory
import org.zstack.sdk.CheckIpAvailabilityAction
import org.zstack.sdk.CheckIpAvailabilityResult
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.FreeIpInventory
import org.zstack.sdk.GetL3NetworkRouterInterfaceIpResult
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.ImageSpec
import org.zstack.testlib.InstanceOfferingSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants

import static java.util.Arrays.asList


/**
 * Created by camile on 2017/4/11.
 */
class OneIpL3NetworkCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)

    }
    @Override
    void environment() {
        env = Env.OneIpL3Network()
    }

    @Override
    void test() {
        env.create {
            createVmSuccessOnOneIpL3Network()
            add3IpRangeToL3Andcreate3VmSuccessButCreateOneMoreVmFailure()
            testSpecifyL3RouterInterfaceIp()
            testIpAddressConflictCase()
        }
    }

    void createVmSuccessOnOneIpL3Network() {
        InstanceOfferingSpec  ioSpec= env.specByName("instanceOffering")
        ImageSpec iSpec = env.specByName("image1")
        L3NetworkSpec l3Spec = env.specByName("l3")
        createVmInstance {
            name = "vm"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3Spec.inventory.uuid))
        }
    }
    void add3IpRangeToL3Andcreate3VmSuccessButCreateOneMoreVmFailure() {
        InstanceOfferingSpec  ioSpec= env.specByName("instanceOffering")
        ImageSpec iSpec = env.specByName("image1")
        L3NetworkSpec l3Spec = env.specByName("l3")
        addIpRange{
            l3NetworkUuid = l3Spec.inventory.uuid
            name = "test2"
            startIp = "192.168.100.11"
            endIp = "192.168.100.11"
            netmask = "255.255.255.0"
            gateway = "192.168.100.1"
        }
        addIpRange{
            l3NetworkUuid = l3Spec.inventory.uuid
            name = "test3"
            startIp = "192.168.100.12"
            endIp = "192.168.100.12"
            netmask = "255.255.255.0"
            gateway = "192.168.100.1"
        }
        addIpRange{
            l3NetworkUuid = l3Spec.inventory.uuid
            name = "test4"
            startIp = "192.168.100.13"
            endIp = "192.168.100.13"
            netmask = "255.255.255.0"
            gateway = "192.168.100.1"
        }

        /* add 1 IpRange which end ip is not in the cidr range */
        expect(AssertionError.class){
            addIpRange{
                l3NetworkUuid = l3Spec.inventory.uuid
                name = "test5"
                startIp = "192.168.101.13"
                endIp = "196.168.100.13"
                netmask = "255.255.255.0"
                gateway = "192.168.100.1"
            }
        }

        createVmInstance {
            name = "vm2"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3Spec.inventory.uuid))
        }
        createVmInstance {
            name = "vm3"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3Spec.inventory.uuid))
        }
        createVmInstance {
            name = "vm4"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3Spec.inventory.uuid))
        }
        CreateVmInstanceAction createVmInstanceAction = new  CreateVmInstanceAction()
        createVmInstanceAction.name ="vm5"
        createVmInstanceAction.instanceOfferingUuid = ioSpec.inventory.uuid
        createVmInstanceAction.imageUuid = iSpec.inventory.uuid
        createVmInstanceAction.l3NetworkUuids = asList((l3Spec.inventory.uuid))
        createVmInstanceAction.sessionId = adminSession()
        CreateVmInstanceAction.Result res = createVmInstanceAction.call()
        res.error != null
    }

    void testSpecifyL3RouterInterfaceIp(){
        InstanceOfferingSpec  ioSpec= env.specByName("instanceOffering")
        ImageSpec iSpec = env.specByName("image1")
        L2NetworkInventory l2Inv = env.inventoryByName("l2")
        L3NetworkInventory l3Inv = env.inventoryByName("l3-1")
        assert (getL3NetworkRouterInterfaceIp {
            l3NetworkUuid = l3Inv.uuid
        } as GetL3NetworkRouterInterfaceIpResult).routerInterfaceIp == null
        expect(AssertionError) {
            setL3NetworkRouterInterfaceIp {
                l3NetworkUuid = l3Inv.uuid
                routerInterfaceIp = "192.168.0.1"
            }
        }

        addIpRange {
            l3NetworkUuid = l3Inv.uuid
            name = "test-ip-range"
            startIp = "192.168.0.3"
            endIp = "192.168.0.254"
            netmask = "255.255.255.0"
            gateway = "192.168.0.1"
        }
        expect(AssertionError) {
            setL3NetworkRouterInterfaceIp {
                l3NetworkUuid = l3Inv.uuid
                routerInterfaceIp = "192.168.0.3"
            }
        }
        setL3NetworkRouterInterfaceIp {
            l3NetworkUuid = l3Inv.uuid
            routerInterfaceIp = "192.168.0.2"
        }
        createVmInstance {
            name = "vm-for-conflict"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3Inv.uuid))
        }
        ApplianceVmInventory vrouterInv = queryApplianceVm {}[0]
        assert vrouterInv.vmNics.stream().filter{vmnic -> vmnic.ip.equals("192.168.0.2")}.findAny().isPresent()
    }

    void testIpAddressConflictCase() {
        VmInstanceInventory vm = queryVmInstance {conditions=["name=vm-for-conflict"]}[0]
        VmNicInventory nic = vm.getVmNics().get(0)

        CheckIpAvailabilityResult res = checkIpAvailability {
            l3NetworkUuid = nic.l3NetworkUuid
            ip = nic.ip
            arpCheck = true
        }
        assert !res.available

        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = nic.l3NetworkUuid
            ipVersion = IPv6Constants.IPv4
            limit = 1
        }

        env.simulator(FlatDhcpBackend.ARPING_NAMESPACE_PATH) { HttpEntity<String> e ->
            FlatDhcpBackend.ArpingRsp rsp = new FlatDhcpBackend.ArpingRsp()
            FlatDhcpBackend.ArpingCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ArpingCmd.class)
            Map<String, List<String>> result = new HashMap<>();
            result.put(cmd.targetIps.get(0), asList("00:00:00:00:00:01"))
            rsp.result = result
            return rsp
        }

        res = checkIpAvailability {
            l3NetworkUuid = nic.l3NetworkUuid
            ip = freeIp4s.get(0).ip
            arpCheck = true
        }
        assert !res.available

        env.simulator(FlatDhcpBackend.ARPING_NAMESPACE_PATH) { HttpEntity<String> e ->
            FlatDhcpBackend.ArpingRsp rsp = new FlatDhcpBackend.ArpingRsp()
            FlatDhcpBackend.ArpingCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ArpingCmd.class)
            Map<String, List<String>> result = new HashMap<>();
            result.put(cmd.targetIps.get(0), new ArrayList<String>())
            rsp.result = result
            return rsp
        }

        res = checkIpAvailability {
            l3NetworkUuid = nic.l3NetworkUuid
            ip = freeIp4s.get(0).ip
            arpCheck = true
        }
        assert res.available

        env.simulator(FlatDhcpBackend.ARPING_NAMESPACE_PATH) { HttpEntity<String> e ->
            FlatDhcpBackend.ArpingRsp rsp = new FlatDhcpBackend.ArpingRsp()
            FlatDhcpBackend.ArpingCmd cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.ArpingCmd.class)
            return rsp
        }

        res = checkIpAvailability {
            l3NetworkUuid = nic.l3NetworkUuid
            ip = freeIp4s.get(0).ip
            arpCheck = true
        }
        assert res.available
    }
}

