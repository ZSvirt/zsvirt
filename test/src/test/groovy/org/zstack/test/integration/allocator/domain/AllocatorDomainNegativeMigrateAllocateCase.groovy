package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.header.AllocatorDomainConstant
import org.zstack.allocator.domain.header.AllocatorDomainErrors
import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig
import org.zstack.allocator.domain.header.AllocatorDomainSystemTags
import org.zstack.header.allocator.HostAllocatorError
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainRuleParam
import org.zstack.header.configuration.ConfigAO
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AllocatorDomainNegativeMigrateAllocateCase extends SubCase {
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
            testMigrateToHostOutsideDomainFails()
        }
    }

    void prepare() {
        zone = env.inventoryByName("zone") as ZoneInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        host1 = env.inventoryByName("kvm1") as HostInventory
        host2 = env.inventoryByName("kvm2") as HostInventory
    }

    void testMigrateToHostOutsideDomainFails() {
        logger.info("Test 001: create VM on host1 with Include host1, expect current-value tag")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "migrate-outside-domain"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start", "Migrate"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "migrate-outside-vm"
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

        logger.info("Test 002: migrate VM to host2 outside domain, expect API failure and tag kept")
        expectApiFailure({
            migrateVm {
                delegate.vmInstanceUuid = vm.uuid
                delegate.hostUuid = host2.uuid
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN.toString()
        }

        List<VmInstanceInventory> afterFail = queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        } as List<VmInstanceInventory>
        assert afterFail.size() == 1
        assert afterFail[0].hostUuid == host1.uuid
        List<SystemTagInventory> tagsAfterFail = querySystemTag {
            delegate.conditions = ["resourceUuid=${vm.uuid}", "tag=${tag}"]
        } as List<SystemTagInventory>
        assert tagsAfterFail.size() == 1

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
