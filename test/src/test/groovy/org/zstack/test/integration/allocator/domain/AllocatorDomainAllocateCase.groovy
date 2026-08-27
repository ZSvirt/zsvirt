package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.header.AllocatorDomainConstant
import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig
import org.zstack.allocator.domain.header.AllocatorDomainState
import org.zstack.allocator.domain.header.AllocatorDomainSystemTags
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.allocator.domain.api.GetAllocatorDomainVmsResult
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainRuleParam
import org.zstack.tag.ResourceConfigSystemTag
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AllocatorDomainAllocateCase extends SubCase {
    EnvSpec env
    ZoneInventory zone
    L3NetworkInventory l3
    HostInventory host1
    HostInventory host2

    @Override
    void setup() {
        useSpring(AllocatorDomainTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
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

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testIncludeHostOnCreate()
            testExcludeHostOnCreate()
            testRecommendHostOnCreate()
            testDisabledKeepsTag()
            testSkipNewCreateHasNoTagThenStartWritesTag()
        }
    }

    void prepare() {
        zone = env.inventoryByName("zone") as ZoneInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        host1 = env.inventoryByName("kvm1") as HostInventory
        host2 = env.inventoryByName("kvm2") as HostInventory
    }

    void testIncludeHostOnCreate() {
        logger.info("Test 001: create VM with Include host1, expect VM on host1 and currentVms has VM")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "include-host1"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "include-vm"
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                [
                    "boot": true,
                    "size": gb(20),
                    "platform": "Linux",
                    "guestOsType": "Linux",
                    "architecture": "x86_64"
                ]
            ]
            delegate.systemTags = [
                    ResourceConfigSystemTag.buildResourceConfig(
                            AllocatorDomainGlobalConfig.CATEGORY,
                            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                            domain.uuid)
            ]
        } as VmInstanceInventory
        assert vm.hostUuid == host1.uuid

        GetAllocatorDomainVmsResult vms = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (vms.currentVms ?: []).size() == 1
        assert (vms.currentVms ?: []).contains(vm.uuid)

        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): domain.uuid])
        List<SystemTagInventory> tags = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        destroyVmInstance {
            delegate.uuid = vm.uuid
        }
        expungeVmInstance {
            delegate.uuid = vm.uuid
        }
        deleteAllocatorDomain {
            delegate.uuid = domain.uuid
        }
    }

    void testExcludeHostOnCreate() {
        logger.info("Test 101: create VM with Exclude host2, expect VM not on host2")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "exclude-host2"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host2.uuid, resourceType: "HostVO", effect: "Exclude")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "exclude-vm"
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                [
                    "boot": true,
                    "size": gb(20),
                    "platform": "Linux",
                    "guestOsType": "Linux",
                    "architecture": "x86_64"
                ]
            ]
            delegate.systemTags = [
                    ResourceConfigSystemTag.buildResourceConfig(
                            AllocatorDomainGlobalConfig.CATEGORY,
                            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                            domain.uuid)
            ]
        } as VmInstanceInventory
        assert vm.hostUuid != host2.uuid
        assert vm.hostUuid == host1.uuid

        GetAllocatorDomainVmsResult vms = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (vms.currentVms ?: []).size() == 1
        assert (vms.currentVms ?: []).contains(vm.uuid)

        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): domain.uuid])
        List<SystemTagInventory> tags = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        destroyVmInstance {
            delegate.uuid = vm.uuid
        }
        expungeVmInstance {
            delegate.uuid = vm.uuid
        }
        deleteAllocatorDomain {
            delegate.uuid = domain.uuid
        }
    }

    void testRecommendHostOnCreate() {
        logger.info("Test 201: create VM with Recommend host1 only, expect VM on host1")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "recommend-host1"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Recommend")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "recommend-vm"
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                [
                    "boot": true,
                    "size": gb(20),
                    "platform": "Linux",
                    "guestOsType": "Linux",
                    "architecture": "x86_64"
                ]
            ]
            delegate.systemTags = [
                    ResourceConfigSystemTag.buildResourceConfig(
                            AllocatorDomainGlobalConfig.CATEGORY,
                            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                            domain.uuid)
            ]
        } as VmInstanceInventory
        assert vm.hostUuid == host1.uuid

        GetAllocatorDomainVmsResult vms = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (vms.currentVms ?: []).size() == 1
        assert (vms.currentVms ?: []).contains(vm.uuid)

        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): domain.uuid])
        List<SystemTagInventory> tags = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        destroyVmInstance {
            delegate.uuid = vm.uuid
        }
        expungeVmInstance {
            delegate.uuid = vm.uuid
        }
        deleteAllocatorDomain {
            delegate.uuid = domain.uuid
        }
    }

    void testDisabledKeepsTag() {
        logger.info("Test 301: create VM then disable domain, expect Start still succeeds and tag kept")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "disabled-keep-tag"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "disabled-vm"
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                [
                    "boot": true,
                    "size": gb(20),
                    "platform": "Linux",
                    "guestOsType": "Linux",
                    "architecture": "x86_64"
                ]
            ]
            delegate.systemTags = [
                    ResourceConfigSystemTag.buildResourceConfig(
                            AllocatorDomainGlobalConfig.CATEGORY,
                            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                            domain.uuid)
            ]
        } as VmInstanceInventory
        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): domain.uuid])
        List<SystemTagInventory> tagsAfterCreate = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterCreate.size() == 1

        domain = updateAllocatorDomain {
            delegate.uuid = domain.uuid
            delegate.state = AllocatorDomainState.Disabled.toString()
        } as AllocatorDomainInventory
        assert domain.state == AllocatorDomainState.Disabled.toString()

        stopVmInstance {
            delegate.uuid = vm.uuid
        }
        startVmInstance {
            delegate.uuid = vm.uuid
        }
        List<SystemTagInventory> tagsAfterStart = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterStart.size() == 1

        GetAllocatorDomainVmsResult vms = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (vms.currentVms ?: []).size() == 1
        assert (vms.currentVms ?: []).contains(vm.uuid)

        destroyVmInstance {
            delegate.uuid = vm.uuid
        }
        expungeVmInstance {
            delegate.uuid = vm.uuid
        }
        deleteAllocatorDomain {
            delegate.uuid = domain.uuid
        }
    }

    void testSkipNewCreateHasNoTagThenStartWritesTag() {
        logger.info("Test 401: create VM when scenes omit NewCreate, expect success and no current-value tag")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "start-only"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Recommend")
            ]
            delegate.scenes = ["Start"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "start-only-vm"
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                [
                    "boot": true,
                    "size": gb(20),
                    "platform": "Linux",
                    "guestOsType": "Linux",
                    "architecture": "x86_64"
                ]
            ]
            delegate.systemTags = [
                    ResourceConfigSystemTag.buildResourceConfig(
                            AllocatorDomainGlobalConfig.CATEGORY,
                            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                            domain.uuid)
            ]
        } as VmInstanceInventory
        assert vm.uuid != null

        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): domain.uuid])
        List<SystemTagInventory> tagsAfterCreate = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterCreate.isEmpty()

        GetAllocatorDomainVmsResult afterCreate = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert !(afterCreate.currentVms ?: []).contains(vm.uuid)
        assert (afterCreate.nextStartVms ?: []).contains(vm.uuid)

        logger.info("Test 501: stop then Start with Start in scenes, expect config written to tag")
        stopVmInstance {
            delegate.uuid = vm.uuid
        }
        startVmInstance {
            delegate.uuid = vm.uuid
        }
        List<SystemTagInventory> tagsAfterStart = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterStart.size() == 1

        GetAllocatorDomainVmsResult afterStart = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (afterStart.currentVms ?: []).size() == 1
        assert (afterStart.currentVms ?: []).contains(vm.uuid)

        destroyVmInstance {
            delegate.uuid = vm.uuid
        }
        expungeVmInstance {
            delegate.uuid = vm.uuid
        }
        deleteAllocatorDomain {
            delegate.uuid = domain.uuid
        }
    }

    @Override
    void clean() {
        (queryVmInstance {} as List<VmInstanceInventory>).each { VmInstanceInventory vm ->
            updateResourceConfig {
                delegate.resourceUuid = vm.uuid
                delegate.category = AllocatorDomainGlobalConfig.CATEGORY
                delegate.name = AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
                delegate.value = AllocatorDomainConstant.UNBOUND_DOMAIN_UUID
            }
        }
        env.delete()
    }
}
