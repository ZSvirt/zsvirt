package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.header.AllocatorDomainConstant
import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig
import org.zstack.allocator.domain.header.AllocatorDomainState
import org.zstack.allocator.domain.header.AllocatorDomainSystemTags
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.vm.HaStartVmInstanceMsg
import org.zstack.header.vm.HaStartVmInstanceReply
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.sdk.GetResourceConfigResult
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.allocator.domain.api.GetAllocatorDomainVmsResult
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainRuleParam
import org.zstack.header.configuration.ConfigAO
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AllocatorDomainMigrateAllocateCase extends SubCase {
    EnvSpec env
    ZoneInventory zone
    L3NetworkInventory l3
    HostInventory host1
    HostInventory host2
    CloudBus bus

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
                        managementIp = "127.0.0.1"
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
            testUnbindStillUsesTagOnMigrateCandidates()
            testRunningConfigWithoutTagDoesNotConstrainMigrate()
            testRunningConfigWithoutTagDoesNotConstrainHa()
            testMigrateWithoutSceneDeletesTag()
            testDisabledMigrateKeepsTag()
        }
    }

    void prepare() {
        zone = env.inventoryByName("zone") as ZoneInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        host1 = env.inventoryByName("kvm1") as HostInventory
        host2 = env.inventoryByName("kvm2") as HostInventory
        bus = bean(CloudBus.class)
    }

    void testUnbindStillUsesTagOnMigrateCandidates() {
        logger.info("Test 001: create VM on host1 with Include host1, expect current-value tag")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "migrate-tag-after-unbind"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start", "Migrate"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "migrate-unbind-vm"
            delegate.hostUuid = host1.uuid
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
                    ConfigAO.buildResourceConfig(
                            AllocatorDomainGlobalConfig.CATEGORY,
                            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                            domain.uuid)
            ]
        } as VmInstanceInventory
        assert vm.hostUuid == host1.uuid

        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): domain.uuid])
        List<SystemTagInventory> tagsAfterCreate = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterCreate.size() == 1

        logger.info("Test 002: unbind resource config to None, expect config None and tag kept")
        updateResourceConfig {
            delegate.resourceUuid = vm.uuid
            delegate.category = AllocatorDomainGlobalConfig.CATEGORY
            delegate.name = AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
            delegate.value = AllocatorDomainConstant.UNBOUND_DOMAIN_UUID
        }
        GetResourceConfigResult unbound = getResourceConfig {
            delegate.resourceUuid = vm.uuid
            delegate.category = AllocatorDomainGlobalConfig.CATEGORY
            delegate.name = AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
        } as GetResourceConfigResult
        assert unbound.value == AllocatorDomainConstant.UNBOUND_DOMAIN_UUID
        List<SystemTagInventory> tagsAfterUnbind = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterUnbind.size() == 1

        logger.info("Test 003: getVmMigrationCandidateHosts after unbind, expect host2 excluded and tag unchanged")
        List<HostInventory> candidates = getVmMigrationCandidateHosts {
            delegate.vmInstanceUuid = vm.uuid
        } as List<HostInventory>
        assert (candidates ?: []).every { it.uuid != host2.uuid }
        List<SystemTagInventory> tagsAfterDryRun = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterDryRun.size() == 1

        logger.info("Test 099: destroy VM and delete domain")
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

    void testRunningConfigWithoutTagDoesNotConstrainMigrate() {
        logger.info("Test 101: create running VM on host1 without domain, expect no current-value tag")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "running-config-no-tag-migrate"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start", "Migrate", "Ha"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "running-config-migrate-vm"
            delegate.hostUuid = host1.uuid
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
        } as VmInstanceInventory
        assert vm.hostUuid == host1.uuid
        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): domain.uuid])
        List<SystemTagInventory> tagsAfterCreate = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterCreate.size() == 0

        logger.info("Test 102: bind Running VM to domain via resource config, expect config set and no tag")
        updateResourceConfig {
            delegate.resourceUuid = vm.uuid
            delegate.category = AllocatorDomainGlobalConfig.CATEGORY
            delegate.name = AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
            delegate.value = domain.uuid
        }
        GetResourceConfigResult bound = getResourceConfig {
            delegate.resourceUuid = vm.uuid
            delegate.category = AllocatorDomainGlobalConfig.CATEGORY
            delegate.name = AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
        } as GetResourceConfigResult
        assert bound.value == domain.uuid

        List<SystemTagInventory> tagsAfterBind = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterBind.size() == 0
        GetAllocatorDomainVmsResult vmsAfterBind = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (vmsAfterBind.currentVms ?: []).isEmpty()
        assert (vmsAfterBind.nextStartVms ?: []).contains(vm.uuid)

        logger.info("Test 103: migrate VM to host2 with config but no tag, expect success and still no tag")
        migrateVm {
            delegate.vmInstanceUuid = vm.uuid
            delegate.hostUuid = host2.uuid
        }
        List<VmInstanceInventory> afterMigrate = queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        } as List<VmInstanceInventory>
        assert afterMigrate.size() == 1
        assert afterMigrate[0].hostUuid == host2.uuid
        List<SystemTagInventory> tagsAfterMigrate = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterMigrate.size() == 0
        GetAllocatorDomainVmsResult vmsAfterMigrate = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (vmsAfterMigrate.currentVms ?: []).isEmpty()
        assert (vmsAfterMigrate.nextStartVms ?: []).contains(vm.uuid)

        logger.info("Test 199: destroy VM and delete domain")
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

    void testRunningConfigWithoutTagDoesNotConstrainHa() {
        logger.info("Test 201: create running VM on host1 without domain, expect no current-value tag")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "running-config-no-tag-ha"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host2.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start", "Migrate", "Ha"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "running-config-ha-vm"
            delegate.hostUuid = host1.uuid
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
        } as VmInstanceInventory
        assert vm.hostUuid == host1.uuid
        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): domain.uuid])
        List<SystemTagInventory> tagsAfterCreate = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterCreate.size() == 0

        logger.info("Test 202: bind Running VM to domain via resource config, expect config set and no tag")
        updateResourceConfig {
            delegate.resourceUuid = vm.uuid
            delegate.category = AllocatorDomainGlobalConfig.CATEGORY
            delegate.name = AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
            delegate.value = domain.uuid
        }
        GetResourceConfigResult bound = getResourceConfig {
            delegate.resourceUuid = vm.uuid
            delegate.category = AllocatorDomainGlobalConfig.CATEGORY
            delegate.name = AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
        } as GetResourceConfigResult
        assert bound.value == domain.uuid

        List<SystemTagInventory> tagsAfterBind = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterBind.size() == 0
        GetAllocatorDomainVmsResult vmsAfterBind = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (vmsAfterBind.currentVms ?: []).isEmpty()
        assert (vmsAfterBind.nextStartVms ?: []).contains(vm.uuid)

        logger.info("Test 203: stop VM then HA start avoiding host1, expect success and still no tag")
        stopVmInstance {
            delegate.uuid = vm.uuid
        }
        List<VmInstanceInventory> afterStop = queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        } as List<VmInstanceInventory>
        assert afterStop.size() == 1
        assert afterStop[0].state == VmInstanceState.Stopped.toString()

        HaStartVmInstanceMsg haMsg = new HaStartVmInstanceMsg()
        haMsg.vmInstanceUuid = vm.uuid
        haMsg.judgerClassName = AlwaysHaStartVmJudger.class.name
        haMsg.softAvoidHostUuids = [host1.uuid]
        haMsg.haReason = "for Test 203"
        bus.makeTargetServiceIdByResourceUuid(haMsg, VmInstanceConstant.SERVICE_ID, vm.uuid)
        HaStartVmInstanceReply haReply = bus.call(haMsg) as HaStartVmInstanceReply
        assert haReply.success
        assert haReply.inventory != null
        assert haReply.inventory.state == VmInstanceState.Running.toString()

        List<VmInstanceInventory> afterHa = queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        } as List<VmInstanceInventory>
        assert afterHa.size() == 1
        assert afterHa[0].state == VmInstanceState.Running.toString()
        List<SystemTagInventory> tagsAfterHa = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterHa.size() == 0
        GetAllocatorDomainVmsResult vmsAfterHa = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (vmsAfterHa.currentVms ?: []).isEmpty()
        assert (vmsAfterHa.nextStartVms ?: []).contains(vm.uuid)

        logger.info("Test 299: destroy VM and delete domain")
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

    void testMigrateWithoutSceneDeletesTag() {
        logger.info("Test 301: create VM with NewCreate/Start only, expect current-value tag")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "migrate-leave-domain"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "migrate-leave-vm"
            delegate.hostUuid = host1.uuid
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
                    ConfigAO.buildResourceConfig(
                            AllocatorDomainGlobalConfig.CATEGORY,
                            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                            domain.uuid)
            ]
        } as VmInstanceInventory
        assert vm.hostUuid == host1.uuid

        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): domain.uuid])
        List<SystemTagInventory> tagsAfterCreate = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterCreate.size() == 1

        logger.info("Test 302: migrate VM to host2 when Migrate is not in scenes, expect success and tag deleted")
        migrateVm {
            delegate.vmInstanceUuid = vm.uuid
            delegate.hostUuid = host2.uuid
        }
        List<VmInstanceInventory> afterMigrate = queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        } as List<VmInstanceInventory>
        assert afterMigrate.size() == 1
        assert afterMigrate[0].hostUuid == host2.uuid
        List<SystemTagInventory> tagsAfterMigrate = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterMigrate.size() == 0

        logger.info("Test 399: destroy VM and delete domain")
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

    void testDisabledMigrateKeepsTag() {
        logger.info("Test 401: create VM then disable domain, expect current-value tag")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "migrate-disabled"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start", "Migrate"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "migrate-disabled-vm"
            delegate.hostUuid = host1.uuid
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
                    ConfigAO.buildResourceConfig(
                            AllocatorDomainGlobalConfig.CATEGORY,
                            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                            domain.uuid)
            ]
        } as VmInstanceInventory
        assert vm.hostUuid == host1.uuid

        String tag = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): domain.uuid])
        List<SystemTagInventory> tagsAfterCreate = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterCreate.size() == 1

        logger.info("Test 402: disable domain then migrate to host2, expect success and tag kept")
        domain = updateAllocatorDomain {
            delegate.uuid = domain.uuid
            delegate.state = AllocatorDomainState.Disabled.toString()
        } as AllocatorDomainInventory
        assert domain.state == AllocatorDomainState.Disabled.toString()

        migrateVm {
            delegate.vmInstanceUuid = vm.uuid
            delegate.hostUuid = host2.uuid
        }
        List<VmInstanceInventory> afterMigrate = queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        } as List<VmInstanceInventory>
        assert afterMigrate.size() == 1
        assert afterMigrate[0].hostUuid == host2.uuid
        List<SystemTagInventory> tagsAfterMigrate = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterMigrate.size() == 1

        logger.info("Test 499: destroy VM and delete domain")
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
