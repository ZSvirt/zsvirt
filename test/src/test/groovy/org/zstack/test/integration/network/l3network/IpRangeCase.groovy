package org.zstack.test.integration.network.l3network

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.network.IpAllocatedReason
import org.zstack.header.network.l3.L3NetworkCategory
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.network.l3.IpNotAvailabilityReason
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.*
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.network.IPv6NetworkUtils

import java.util.stream.Collectors

import static java.util.Arrays.asList


/**
 * Created by shixin on 2018/05/29.
 */
class IpRangeCase extends SubCase {
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
            testAddIpRangeToDifferentL3ButSameL2()
            testReserveIpAddress()
            testReturnIpAddressInReserveIpRange()
        }
    }

    void testAddIpRangeToDifferentL3ButSameL2() {
        InstanceOfferingSpec  ioSpec= env.specByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L3NetworkInventory l3_1 = env.inventoryByName("l3-1")
        ImageSpec iSpec = env.specByName("image1")

        expect (AssertionError.class) {
            addIpRangeByNetworkCidr {
                name = "cidr-1"
                l3NetworkUuid = l3_1.getUuid()
                networkCidr = "192.168.100.0/24"
            }
        }

        expect (AssertionError.class) {
            addIpRangeByNetworkCidr {
                name = "cidr-2"
                l3NetworkUuid = l3_1.getUuid()
                networkCidr = "192.168.0.0/16"
            }
        }

        expect (AssertionError.class) {
            addIpRangeByNetworkCidr {
                name = "cidr-3"
                l3NetworkUuid = l3_1.getUuid()
                networkCidr = "192.168.100.0/25"
            }
        }

        addIpRange {
            name = "ipr-3"
            l3NetworkUuid = l3_1.getUuid()
            startIp = "192.168.100.101"
            endIp = "192.168.100.200"
            gateway = "192.168.100.1"
            netmask = "255.255.255.0"
        }

        L3NetworkInventory l3_2 = createL3Network {
            name = "l3-2"
            l2NetworkUuid = l3_1.l2NetworkUuid
            category = L3NetworkCategory.Private
        }

        addIpRange {
            name = "ipr-4"
            l3NetworkUuid = l3_2.getUuid()
            startIp = "192.168.101.101"
            endIp = "192.168.101.200"
            gateway = "192.168.101.1"
            netmask = "255.255.255.0"
        }

        L3NetworkInventory l3_3 = createL3Network {
            name = "l3-3"
            l2NetworkUuid = l3_1.l2NetworkUuid
            category = L3NetworkCategory.Private
        }
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3_3.uuid
            networkServices = ['Flat':['DHCP']]
        }

        addIpRange {
            name = "ipr-5"
            l3NetworkUuid = l3_3.getUuid()
            startIp = "10.0.1.101"
            endIp = "10.0.1.200"
            gateway = "10.0.1.1"
            netmask = "255.0.0.0"
        }

        expect (AssertionError.class) {
            addIpRange {
                name = "ipr-6"
                l3NetworkUuid = l3_3.getUuid()
                startIp = "10.0.2.101"
                endIp = "10.0.2.200"
                gateway = "10.0.2.1"
                netmask = "255.0.0.0"
            }
        }

        CheckIpAvailabilityAction check = new CheckIpAvailabilityAction()
        check.ip = "10.0.1.1"
        check.l3NetworkUuid = l3_3.getUuid()
        check.sessionId = adminSession()
        CheckIpAvailabilityAction.Result res = check.call()
        assert res.value.available == false
        assert res.value.reason == IpNotAvailabilityReason.GATEWAY.toString()

        check = new CheckIpAvailabilityAction()
        check.ip = "10.0.1.2"
        check.l3NetworkUuid = l3_3.getUuid()
        check.sessionId = adminSession()
        res = check.call()
        assert res.value.available == false
        assert res.value.reason == IpNotAvailabilityReason.NO_IN_RANGE.toString()

        check = new CheckIpAvailabilityAction()
        check.ip = "10.0.1.120"
        check.l3NetworkUuid = l3_3.getUuid()
        check.sessionId = adminSession()
        res = check.call()
        assert res.value.available == true

        VmInstanceInventory vm = createVmInstance {
            name = "vm-l3_3"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3_3.uuid))
        }
        VmNicInventory nic = vm.getVmNics().get(0)

        check = new CheckIpAvailabilityAction()
        check.ip = nic.ip
        check.l3NetworkUuid = l3_3.getUuid()
        check.sessionId = adminSession()
        res = check.call()
        assert res.value.available == false
        assert res.value.reason == IpNotAvailabilityReason.USED.toString()

        L3NetworkInventory l3_4 = createL3Network {
            name = "l3-4"
            l2NetworkUuid = l3_1.l2NetworkUuid
            category = L3NetworkCategory.Private
        }
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3_4.uuid
            networkServices = ['Flat':['DHCP']]
        }

        addIpRangeByNetworkCidr {
            name = "cidr-4"
            l3NetworkUuid = l3_4.getUuid()
            networkCidr = "192.168.211.0/24"
            gateway = "192.168.211.254"
        }

        L3NetworkInventory l3_5 = createL3Network {
            name = "l3-5"
            l2NetworkUuid = l3_1.l2NetworkUuid
            category = L3NetworkCategory.Private
        }
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3_5.uuid
            networkServices = ['Flat':['DHCP']]
        }

        addIpRangeByNetworkCidr {
            name = "cidr-5"
            l3NetworkUuid = l3_5.getUuid()
            networkCidr = "192.168.212.0/24"
            gateway = "192.168.212.1"
        }

        L3NetworkInventory l3_6 = createL3Network {
            name = "l3-6"
            l2NetworkUuid = l3_1.l2NetworkUuid
            category = L3NetworkCategory.Private
        }
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3_6.uuid
            networkServices = ['Flat':['DHCP']]
        }

        addIpRangeByNetworkCidr {
            name = "cidr-6"
            l3NetworkUuid = l3_6.getUuid()
            networkCidr = "192.168.213.0/24"
        }

        L3NetworkInventory l3_7 = createL3Network {
            name = "l3-7"
            l2NetworkUuid = l3_1.l2NetworkUuid
            category = L3NetworkCategory.Private
        }

        AddIpRangeByNetworkCidrAction addIpRangeByNetworkCidrActionAction = new AddIpRangeByNetworkCidrAction()
        addIpRangeByNetworkCidrActionAction.name = "cidr-7"
        addIpRangeByNetworkCidrActionAction.l3NetworkUuid = l3_7.getUuid()
        addIpRangeByNetworkCidrActionAction.networkCidr = "192.168.214.0/24"
        addIpRangeByNetworkCidrActionAction.gateway = "192.168.214.100"
        addIpRangeByNetworkCidrActionAction.sessionId = adminSession()
        AddIpRangeByNetworkCidrAction.Result ret = addIpRangeByNetworkCidrActionAction.call()

        assert ret.error == null
        assert ret.value.inventory.startIp == "192.168.214.1"
        assert ret.value.inventory.endIp == "192.168.214.99"
        assert ret.value.inventory.gateway == "192.168.214.100"

        assert ret.value.inventories.get(0).startIp == "192.168.214.1"
        assert ret.value.inventories.get(0).endIp == "192.168.214.99"
        assert ret.value.inventories.get(0).gateway == "192.168.214.100"

        assert ret.value.inventories.get(1).startIp == "192.168.214.101"
        assert ret.value.inventories.get(1).endIp == "192.168.214.254"
        assert ret.value.inventories.get(1).gateway == "192.168.214.100"
    }

    void testReserveIpAddress() {
        L3NetworkInventory l3_2 = env.inventoryByName("l3-2")
        IpRangeInventory ipr = l3_2.ipRanges.get(0)

        expect(AssertionError.class)  {
            addReservedIpRange {
                l3NetworkUuid = l3_2.uuid
                startIp = "193.0.0.2"
                endIp = "193.0.3.255"
            }
        }

        ReservedIpRangeInventory reservedIpRange = addReservedIpRange {
            l3NetworkUuid = l3_2.uuid
            startIp = "10.0.0.2"
            endIp = "10.0.3.255"
        }

        List<String> reservedUuids = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3_2.uuid)
                .eq(UsedIpVO_.usedFor, IpAllocatedReason.Reserved.toString())
                .select(UsedIpVO_.uuid).listValues()
        assert reservedUuids.size() == 253

        deleteIpAddress {
            l3NetworkUuid = l3_2.uuid
            usedIpUuids = reservedUuids.stream().limit(100).collect(Collectors.toList())
        }
        reservedUuids = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3_2.uuid)
                .eq(UsedIpVO_.usedFor, IpAllocatedReason.Reserved.toString())
                .select(UsedIpVO_.uuid).listValues()
        assert reservedUuids.size() == 153

        deleteReservedIpRange {
            uuid = reservedIpRange.uuid
        }
        reservedUuids = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3_2.uuid)
                .eq(UsedIpVO_.usedFor, IpAllocatedReason.Reserved.toString())
                .select(UsedIpVO_.uuid).listValues()
        assert reservedUuids.size() == 0

        reservedIpRange = addReservedIpRange {
            l3NetworkUuid = l3_2.uuid
            startIp = "10.0.0.100"
            endIp = "10.0.0.200"
        }

        expect(AssertionError.class) {
            addReservedIpRange {
                l3NetworkUuid = l3_2.uuid
                startIp = "10.0.0.100"
                endIp = "10.0.0.150"
            }
        }

        expect(AssertionError.class) {
            addReservedIpRange {
                l3NetworkUuid = l3_2.uuid
                startIp = "10.0.0.2"
                endIp = "10.0.3.0"
            }
        }

        ReservedIpRangeInventory reservedIpRange1 = addReservedIpRange {
            l3NetworkUuid = l3_2.uuid
            startIp = "10.0.1.0"
            endIp = "10.0.1.10"
        }
        reservedUuids = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3_2.uuid)
                .eq(UsedIpVO_.usedFor, IpAllocatedReason.Reserved.toString())
                .select(UsedIpVO_.uuid).listValues()
        assert reservedUuids.size() == 101

        addIpRange {
            name = "TestIpRange"
            l3NetworkUuid = l3_2.getUuid()
            startIp = "10.0.1.0"
            endIp = "10.0.1.100"
            gateway = "10.0.0.1"
            netmask = "255.0.0.0"
        }

        reservedUuids = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3_2.uuid)
                .eq(UsedIpVO_.usedFor, IpAllocatedReason.Reserved.toString())
                .select(UsedIpVO_.uuid).listValues()
        assert reservedUuids.size() == 112

        deleteIpRange {
            uuid = ipr.uuid
        }

        retryInSecs {
            reservedUuids = Q.New(UsedIpVO.class)
                    .eq(UsedIpVO_.l3NetworkUuid, l3_2.uuid)
                    .eq(UsedIpVO_.usedFor, IpAllocatedReason.Reserved.toString())
                    .select(UsedIpVO_.uuid).listValues()
            assert reservedUuids.size() == 11
        }

        addIpv6Range {
            name = "ipr-6"
            l3NetworkUuid = l3_2.uuid
            startIp = "2024:05:27::30"
            endIp = "2024:05:27::40"
            gateway = "2024:05:27::1"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        ReservedIpRangeInventory reservedIpRange2 = addReservedIpRange {
            l3NetworkUuid = l3_2.uuid
            startIp = "2024:05:27::30"
            endIp = "2024:05:27::33"
        }

        deleteReservedIpRange {
            uuid = reservedIpRange1.uuid
        }

        deleteReservedIpRange {
            uuid = reservedIpRange.uuid
        }

        deleteReservedIpRange {
            uuid = reservedIpRange2.uuid
        }
    }

    void testReturnIpAddressInReserveIpRange() {
        /* l3-2 has ip range:
           10.0.1.0, 10.0.1.100,
           2024:05:27::30~2024:05:27::40 added in last step */
        L3NetworkInventory l3_2 = env.inventoryByName("l3-2")
        InstanceOfferingSpec  ioSpec= env.specByName("instanceOffering")
        ImageSpec iSpec = env.specByName("image1")

        /* delete old dhcp server ip  */
        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3_2.uuid
            service = 'DHCP'
        }

        VmInstanceInventory vm_l3_3 = queryVmInstance {conditions=["name=vm-l3_3"]} [0]
        VmNicInventory nicOld = vm_l3_3.vmNics.get(0)
        FreeIpInventory ipv6 = getFreeIp {
            l3NetworkUuid = l3_2.uuid
            ipVersion = IPv6Constants.IPv6
            limit = 1} [0]
        FreeIpInventory ipv4 = getFreeIp {
            l3NetworkUuid = l3_2.uuid
            ipVersion = IPv6Constants.IPv4
            limit = 1} [0]

        attachL3NetworkToVm {
            l3NetworkUuid = l3_2.uuid
            vmInstanceUuid = vm_l3_3.uuid
            systemTags = [String.format("staticIp::%s::%s", l3_2.uuid, ipv4.ip),
                          String.format("staticIp::%s::%s", l3_2.uuid, IPv6NetworkUtils.ipv6AddessToTagValue(ipv6.ip))]
        }
        VmNicInventory nic1 = queryVmNic {
            conditions = ["l3NetworkUuid=${l3_2.uuid}", "vmInstance.uuid=${vm_l3_3.uuid}"]
        }[0]
        assert nic1.usedIps.size() == 2
        for (UsedIpInventory ip : nic1.usedIps) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                assert ip.ip == ipv4.ip
            } else {
                assert ip.ip == ipv6.ip
            }
        }

        detachL3NetworkFromVm {
            vmNicUuid = nic1.uuid
        }

        VmNicInventory nic2 = queryVmNic {
            conditions = ["l3NetworkUuid=${nicOld.l3NetworkUuid}", "vmInstance.uuid=${vm_l3_3.uuid}"]
        }[0]
        changeVmNicNetwork {
            vmNicUuid = nic2.uuid
            destL3NetworkUuid = l3_2.uuid
            systemTags = [String.format("staticIp::%s::%s", l3_2.uuid, ipv4.ip),
                          String.format("staticIp::%s::%s", l3_2.uuid, IPv6NetworkUtils.ipv6AddessToTagValue(ipv6.ip))]
        }
        nic2 = queryVmNic {
            conditions = ["l3NetworkUuid=${l3_2.uuid}", "vmInstance.uuid=${vm_l3_3.uuid}"]
        }[0]
        assert nic2.usedIps.size() == 2
        for (UsedIpInventory ip : nic2.usedIps) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                assert ip.ip == ipv4.ip
            } else {
                assert ip.ip == ipv6.ip
            }
        }

        FreeIpInventory ipv61 = getFreeIp {
            l3NetworkUuid = l3_2.uuid
            ipVersion = IPv6Constants.IPv6
            limit = 1} [0]
        FreeIpInventory ipv41 = getFreeIp {
            l3NetworkUuid = l3_2.uuid
            ipVersion = IPv6Constants.IPv4
            limit = 1} [0]
        setVmStaticIp {
            vmInstanceUuid = vm_l3_3.uuid
            l3NetworkUuid = l3_2.uuid
            ip = ipv41.ip
            ip6 = ipv61.ip
        }
        nic2 = queryVmNic {
            conditions = ["l3NetworkUuid=${l3_2.uuid}", "vmInstance.uuid=${vm_l3_3.uuid}"]
        }[0]
        assert nic2.usedIps.size() == 2
        for (UsedIpInventory ip : nic2.usedIps) {
            if (ip.ipVersion == IPv6Constants.IPv4) {
                assert ip.ip == ipv41.ip
                assert ip.netmask == ipv41.netmask
                assert ip.gateway == ipv41.gateway
            } else {
                assert ip.ip == ipv61.ip
                assert ip.netmask == ipv61.netmask
                assert ip.gateway == ipv61.gateway
            }
        }

        // ip and ip6 can not be null at the same time
        expectError {
            setVmStaticIp {
                vmInstanceUuid = vm_l3_3.uuid
                l3NetworkUuid = l3_2.uuid
            }
        }

        detachL3NetworkFromVm {
            vmNicUuid = nic2.uuid
        }

        /* enable dhcp to occupy an ip address  */
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3_2.uuid
            networkServices = ["Flat":["DHCP"]]
        }

        /* create vm to occupy an ip address */
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = ioSpec.inventory.uuid
            imageUuid = iSpec.inventory.uuid
            l3NetworkUuids = asList((l3_2.uuid))
        }
        VmNicInventory nic = vm.getVmNics().get(0)

        /* because ip allocate type: FirstAvailableIpAllocatorStrategy:
        * dhcp server ip and vm nic ip is first 2 ip address of the range */
        ReservedIpRangeInventory reservedIpRange = addReservedIpRange {
            l3NetworkUuid = l3_2.uuid
            startIp = "10.0.1.0"
            endIp = "10.0.1.9"
        }

        ReservedIpRangeInventory reservedIpRange2 = addReservedIpRange {
            l3NetworkUuid = l3_2.uuid
            startIp = "2024:05:27::30"
            endIp = "2024:05:27::33"
        }

        /* 8 ipv4 + 2 ipv6 */
        List<String> reservedUuids = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3_2.uuid)
                .eq(UsedIpVO_.usedFor, IpAllocatedReason.Reserved.toString())
                .select(UsedIpVO_.uuid).listValues()
        assert reservedUuids.size() == 10

        /* delete dhcp server ip  */
        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3_2.uuid
            service = 'DHCP'
        }

        /* delete vm ip */
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString());
        destroyVmInstance {
            uuid = vm.uuid
        }

        /* dhcp server ip and vm nic ip change to reserve ip
        * 10 ipv4 + 4 ipv6 */
        reservedUuids = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, l3_2.uuid)
                .eq(UsedIpVO_.usedFor, IpAllocatedReason.Reserved.toString())
                .select(UsedIpVO_.uuid).listValues()
        assert reservedUuids.size() == 14
    }
}

