package org.zstack.test.integration.network.l3network

import org.zstack.network.l3.L3NetworkSystemTags
import org.zstack.network.l3.L3NetworkGlobalConfig
import org.zstack.tag.PatternedSystemTag
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.sdk.*
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.header.network.l3.L3NetworkConstant

class AscDelayRecycleIpAllocatorStrategyCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
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
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        totalCpu = 40
                        totalMem = SizeUnit.GIGABYTE.toByte(32)
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
                        category = "Private"
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAscDelayRecycleOneIpRange();
            testAscDelayRecycleMultiIpRange();
            testAscDelayRecycleMultiIpRangeWithAddressPool();
            testAscDelayRecycleSwitchStrategy();
        }
    }

    void testAscDelayRecycleOneIpRange() {
        def l2 = env.inventoryByName("l2") as L2NetworkInventory
        ImageInventory image = env.inventoryByName("image1")

        L3NetworkInventory l3 = createL3Network {
            category = "Private"
            system = false
            l2NetworkUuid = l2.uuid
            name = "l3_1"
            systemTags = ["resourceConfig::l3Network::ipAllocateStrategy::AscDelayRecycleIpAllocatorStrategy"]
        }

        def flatProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0] as NetworkServiceProviderInventory

        def netServices = ["${flatProvider.uuid}":["DHCP"]]

        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = netServices
        }

        IpRangeInventory range = addIpRange {
            name = "range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.0.2"
            endIp = "192.168.0.11"
            gateway = "192.168.0.1"
            netmask = "255.255.255.0"
        }

        VmInstanceInventory vm_1 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_1.vmNics[0].usedIps[0].ip == "192.168.0.3")

        VmInstanceInventory vm_2 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_2.vmNics[0].usedIps[0].ip == "192.168.0.4")

        VmInstanceInventory vm_3 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_3.vmNics[0].usedIps[0].ip == "192.168.0.5")

        VmInstanceInventory vm_4 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_4.vmNics[0].usedIps[0].ip == "192.168.0.6")

        deleteVmInstance(vm_4)
        vm_4 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_4.vmNics[0].usedIps[0].ip == '192.168.0.7')

        deleteVmInstance(vm_2)
        deleteVmInstance(vm_3)

        VmInstanceInventory vm_5 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_5.vmNics[0].usedIps[0].ip == '192.168.0.8')

        VmInstanceInventory vm_6 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            systemTags = ["staticIp::"+l3.uuid+"::192.168.0.10"]
        }
        assert(vm_6.vmNics[0].usedIps[0].ip == '192.168.0.10')

        VmInstanceInventory vm_7 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_7.vmNics[0].usedIps[0].ip == '192.168.0.9')

        VmInstanceInventory vm_8 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_8.vmNics[0].usedIps[0].ip == '192.168.0.11')

        VmInstanceInventory vm_9 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_9.vmNics[0].usedIps[0].ip == '192.168.0.4')

        deleteVmInstance(vm_1)
        deleteVmInstance(vm_4)
        deleteVmInstance(vm_5)
        deleteVmInstance(vm_6)
        deleteVmInstance(vm_7)
        deleteVmInstance(vm_8)
        deleteVmInstance(vm_9)

        List<Thread> threads = new ArrayList<>()
        List<VmInstanceInventory> vms = []
        for (int i = 3; i < 13; i++) {
            threads.add(Thread.start {
                try {
                    def vm = createVmInstance {
                        name = "vm_${i}"
                        cpuNum = 1
                        memorySize = gb(1)
                        imageUuid = image.uuid
                        l3NetworkUuids = [l3.uuid]
                    } as VmInstanceInventory
                    vms.add(vm)
                } catch (AssertionError e) {
                    // Rethrow the assertion error as a RuntimeException
                    throw new RuntimeException("Assertion failed in thread ${Thread.currentThread().name}", e)
                }
            })
        }
        threads.forEach({it -> it.join()})

        deleteIpRange { uuid = range.uuid }
        deleteL3Network { uuid = l3.uuid }

        vms.each {
            deleteVmInstance(it)
        }
    }

    void testAscDelayRecycleMultiIpRange() {
        def l2 = env.inventoryByName("l2") as L2NetworkInventory
        ImageInventory image = env.inventoryByName("image1")

        L3NetworkInventory l3 = createL3Network {
            category = "Private"
            system = false
            l2NetworkUuid = l2.uuid
            name = "l3_1"
            systemTags = ["resourceConfig::l3Network::ipAllocateStrategy::AscDelayRecycleIpAllocatorStrategy"]
        }

        def flatProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0] as NetworkServiceProviderInventory

        def netServices = ["${flatProvider.uuid}":["DHCP"]]

        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = netServices
        }

        IpRangeInventory range_1 = addIpRange {
            name = "range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.0.5"
            endIp = "192.168.0.11"
            gateway = "192.168.0.1"
            netmask = "255.255.255.0"
        }

        IpRangeInventory range_2 = addIpRange {
            name = "range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.0.65"
            endIp = "192.168.0.80"
            gateway = "192.168.0.1"
            netmask = "255.255.255.0"
        }

        IpRangeInventory range_3 = addIpRange {
            name = "range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.0.35"
            endIp = "192.168.0.50"
            gateway = "192.168.0.1"
            netmask = "255.255.255.0"
        }

        VmInstanceInventory vm_1 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_1.vmNics[0].usedIps[0].ip == "192.168.0.6")

        VmInstanceInventory vm_2 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_2.vmNics[0].usedIps[0].ip == "192.168.0.7")

        VmInstanceInventory vm_3 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            systemTags = ["staticIp::"+l3.uuid+"::192.168.0.10"]
        }
        assert(vm_3.vmNics[0].usedIps[0].ip == "192.168.0.10")

        VmInstanceInventory vm_4 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_4.vmNics[0].usedIps[0].ip == "192.168.0.8")

        VmInstanceInventory vm_5 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            systemTags = ["staticIp::"+l3.uuid+"::192.168.0.37"]
        }
        assert(vm_5.vmNics[0].usedIps[0].ip == "192.168.0.37")

        VmInstanceInventory vm_6 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_6.vmNics[0].usedIps[0].ip == "192.168.0.9")

        VmInstanceInventory vm_7 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_7.vmNics[0].usedIps[0].ip == "192.168.0.11")

        deleteVmInstance(vm_2)

        VmInstanceInventory vm_8 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_8.vmNics[0].usedIps[0].ip == "192.168.0.35")

        VmInstanceInventory vm_9 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_9.vmNics[0].usedIps[0].ip == "192.168.0.36")

        VmInstanceInventory vm_10 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_10.vmNics[0].usedIps[0].ip == "192.168.0.38")

        deleteIpRange { uuid = range_3.uuid }
        VmInstanceInventory vm_11 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_11.vmNics[0].usedIps[0].ip == "192.168.0.65")

        deleteIpRange { uuid = range_2.uuid }
        VmInstanceInventory vm_12 = createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert(vm_12.vmNics[0].usedIps[0].ip == "192.168.0.7")

        deleteVmInstance(vm_1)
        deleteVmInstance(vm_3)
        deleteVmInstance(vm_4)
        deleteVmInstance(vm_5)
        deleteVmInstance(vm_6)
        deleteVmInstance(vm_7)
        deleteVmInstance(vm_8)
        deleteVmInstance(vm_9)
        deleteVmInstance(vm_10)
        deleteVmInstance(vm_11)
        deleteIpRange { uuid = range_1.uuid }
        deleteIpRange { uuid = range_3.uuid }
        deleteL3Network { uuid = l3.uuid }
    }

    void testAscDelayRecycleMultiIpRangeWithAddressPool() {
        def l2 = env.inventoryByName("l2") as L2NetworkInventory

        L3NetworkInventory l3 = createL3Network {
            category = "Public"
            system = false
            l2NetworkUuid = l2.uuid
            name = "l3_1"
            systemTags = ["resourceConfig::l3Network::ipAllocateStrategy::AscDelayRecycleIpAllocatorStrategy"]
        }

        def flatProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0] as NetworkServiceProviderInventory

        def netServices = ["${flatProvider.uuid}":["DHCP"]]

        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = netServices
        }

        IpRangeInventory range_1 = addIpRange {
            name = "range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.6.4"
            endIp = "192.168.6.6"
            gateway = "192.168.6.1"
            netmask = "255.255.255.0"
        }

        IpRangeInventory range_2 = addIpRange {
            name = "range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.6.35"
            endIp = "192.168.6.36"
            gateway = "192.168.6.1"
            netmask = "255.255.255.0"
        }

        IpRangeInventory range_3 = addIpRange {
            name = "address-pool"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.4.5"
            endIp = "192.168.4.8"
            netmask = "255.255.255.0"
            ipRangeType = IpRangeType.AddressPool.toString()
        }

        IpRangeInventory range_4 = addIpRange {
            name = "address-pool"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.8.5"
            endIp = "192.168.8.8"
            netmask = "255.255.255.0"
            ipRangeType = IpRangeType.AddressPool.toString()
        }

        /* dhcp server ip is the first ip */
        VipInventory vip_1 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }
        assert(vip_1.ip == '192.168.6.5')

        VipInventory vip_2 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }
        assert(vip_2.ip == '192.168.6.6')

        deleteVip { uuid = vip_1.uuid }

        VipInventory vip_3 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }
        assert(vip_3.ip == '192.168.6.35')

        deleteIpRange { uuid = range_2.uuid }

        VipInventory vip_4 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }
        assert(vip_4.ip == '192.168.6.5')

        deleteVip { uuid = vip_4.uuid }

        VipInventory vip_6 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }
        assert(vip_6.ip == '192.168.6.5')

        VipInventory vip_7 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = range_3.uuid
            requiredIp = "192.168.4.8"
        }
        assert(vip_7.ip == '192.168.4.8')

        VipInventory vip_8 = createVip {
            name = "vip"
            ipRangeUuid = range_3.uuid
            l3NetworkUuid = l3.uuid
        }
        assert(vip_8.ip == '192.168.4.5')

        deleteIpRange { uuid = range_3.uuid }

        //no noraml ip available
        expect(AssertionError.class) {
            VipInventory vip_9 = createVip {
                name = "vip"
                l3NetworkUuid = l3.uuid
            }
        }

        deleteVip { uuid = vip_8.uuid }

        range_2 = addIpRange {
            name = "range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.6.35"
            endIp = "192.168.6.35"
            gateway = "192.168.6.1"
            netmask = "255.255.255.0"
        }

        VipInventory vip_11 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }
        assert(vip_11.ip == '192.168.6.35')

        range_3 = addIpRange {
            name = "address-pool"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.4.5"
            endIp = "192.168.4.8"
            netmask = "255.255.255.0"
            ipRangeType = IpRangeType.AddressPool.toString()
        }

        VipInventory vip_12 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = range_4.uuid
        }
        assert(vip_12.ip == '192.168.8.5')

        VipInventory vip_13 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = range_4.uuid
        }
        assert(vip_13.ip == '192.168.8.6')

        deleteVip { uuid = vip_12.uuid }

        vip_12 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = range_4.uuid
        }
        assert(vip_12.ip == '192.168.8.7')

        deleteIpRange { uuid = range_4.uuid }

        VipInventory vip_14 = createVip {
            name = "vip"
            ipRangeUuid = range_3.uuid
            l3NetworkUuid = l3.uuid
        }
        assert(vip_14.ip == '192.168.4.5')

        expect(AssertionError.class) {
            VipInventory vip_15 = createVip {
                name = "vip"
                l3NetworkUuid = l3.uuid
                requiredIp = "192.168.4.6"
            }
        }

        deleteVip { uuid = vip_2.uuid }
        deleteVip { uuid = vip_3.uuid }
        deleteVip { uuid = vip_4.uuid }
        deleteVip { uuid = vip_6.uuid }
        deleteVip { uuid = vip_11.uuid }
        deleteVip { uuid = vip_12.uuid }
        deleteVip { uuid = vip_13.uuid }
        deleteVip { uuid = vip_14.uuid }

        deleteIpRange { uuid = range_4.uuid }
        deleteIpRange { uuid = range_3.uuid }
        deleteIpRange { uuid = range_2.uuid }
        deleteIpRange { uuid = range_1.uuid }
        deleteL3Network { uuid = l3.uuid }
    }

    void testAscDelayRecycleSwitchStrategy() {
        def l2 = env.inventoryByName("l2") as L2NetworkInventory

        def l3 = createL3Network {
            category = "Public"
            system = false
            l2NetworkUuid = l2.uuid
            name = "l3_1"
            systemTags = ["resourceConfig::l3Network::ipAllocateStrategy::AscDelayRecycleIpAllocatorStrategy"]
        } as L3NetworkInventory

        def flatProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0] as NetworkServiceProviderInventory

        def netServices = ["${flatProvider.uuid}":["DHCP"]]

        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = netServices
        }

        def range_1 = addIpRange {
            name = "range"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.0.4"
            endIp = "192.168.0.11"
            gateway = "192.168.0.1"
            netmask = "255.255.255.0"
        } as IpRangeInventory

        /* dhcp server is the first ip */
        def vip_1 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        } as VipInventory
        assert(vip_1.ip == '192.168.0.5')

        updateResourceConfig {
            name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
            category= L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
            resourceUuid= l3.uuid
            value = L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY
        }

        def vip_2 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        } as VipInventory

        def pst = L3NetworkSystemTags.NETWORK_ASC_DELAY_NORMAL_NEXT_IP as PatternedSystemTag;
        assert pst.getTagInventory(l3.uuid) == null

        def vip_3 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        } as VipInventory

        updateResourceConfig {
            name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
            category= L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
            resourceUuid= l3.uuid
            value = L3NetworkConstant.ASC_DELAY_RECYCLE_IP_ALLOCATOR_STRATEGY
        }

        def vip_4 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        } as VipInventory

        assert pst.getTagInventory(l3.uuid) != null

        updateResourceConfig {
            name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
            category= L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
            resourceUuid= l3.uuid
            value = L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY
        }

        def vip_5 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        } as VipInventory

        assert pst.getTagInventory(l3.uuid) == null

        updateResourceConfig {
            name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
            category= L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
            resourceUuid= l3.uuid
            value = L3NetworkConstant.ASC_DELAY_RECYCLE_IP_ALLOCATOR_STRATEGY
        }

        def vip_6 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        } as VipInventory

        assert pst.getTagInventory(l3.uuid) != null

        def range_2 = addIpRange {
            name = "address-pool"
            l3NetworkUuid = l3.uuid
            startIp = "192.168.4.5"
            endIp = "192.168.4.11"
            netmask = "255.255.255.0"
            ipRangeType = IpRangeType.AddressPool.toString()
        } as IpRangeInventory

        def vip_7 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = range_2.uuid
        } as VipInventory
        assert(vip_7.ip == '192.168.4.5')

        updateResourceConfig {
            name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
            category= L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
            resourceUuid= l3.uuid
            value = L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY
        }

        def vip_8 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = range_2.uuid
        } as VipInventory

        pst = L3NetworkSystemTags.NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP as PatternedSystemTag;
        assert pst.getTagInventory(l3.uuid) == null

        def vip_9 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = range_2.uuid
        } as VipInventory

        updateResourceConfig {
            name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
            category= L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
            resourceUuid= l3.uuid
            value = L3NetworkConstant.ASC_DELAY_RECYCLE_IP_ALLOCATOR_STRATEGY
        }

        def vip_10 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = range_2.uuid
        } as VipInventory

        assert pst.getTagInventory(l3.uuid) != null

        updateResourceConfig {
            name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
            category= L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
            resourceUuid= l3.uuid
            value = L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY
        }

        def vip_11 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = range_2.uuid
        } as VipInventory

        assert pst.getTagInventory(l3.uuid) == null

        updateResourceConfig {
            name = L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.name
            category= L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.category
            resourceUuid= l3.uuid
            value = L3NetworkConstant.ASC_DELAY_RECYCLE_IP_ALLOCATOR_STRATEGY
        }

        def vip_12 = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
            ipRangeUuid = range_2.uuid
        } as VipInventory

        assert pst.getTagInventory(l3.uuid) != null

        deleteVip { uuid = vip_1.uuid }
        deleteVip { uuid = vip_2.uuid }
        deleteVip { uuid = vip_3.uuid }
        deleteVip { uuid = vip_4.uuid }
        deleteVip { uuid = vip_5.uuid }
        deleteVip { uuid = vip_6.uuid }
        deleteVip { uuid = vip_7.uuid }
        deleteVip { uuid = vip_8.uuid }
        deleteVip { uuid = vip_9.uuid }
        deleteVip { uuid = vip_10.uuid }
        deleteVip { uuid = vip_11.uuid }
        deleteVip { uuid = vip_12.uuid }
        deleteIpRange { uuid = range_2.uuid }
        deleteIpRange { uuid = range_1.uuid }
        deleteL3Network { uuid = l3.uuid}
    }

	void deleteVmInstance(vm) {
        destroyVmInstance {
            delegate.uuid = vm.uuid
        }
        expungeVmInstance {
            delegate.uuid = vm.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
