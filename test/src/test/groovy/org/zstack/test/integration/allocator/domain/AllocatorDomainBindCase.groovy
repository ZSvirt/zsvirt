package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.header.AllocatorDomainConstant
import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig
import org.zstack.header.vm.DiskAO
import org.zstack.sdk.GetResourceConfigResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.allocator.domain.api.GetAllocatorDomainVmsResult
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AllocatorDomainBindCase extends SubCase {
    EnvSpec env
    ZoneInventory zone
    L3NetworkInventory l3
    VmInstanceInventory vm
    AllocatorDomainInventory domain

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

            vm {
                name = "vm"
                memoryGB(8)
                cpu = 4
                disk {
                    sizeGB(20)
                    platform = "Linux"
                    guestOsType = "Linux"
                    architecture = "x86_64"
                }
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testBindAndUnbindStoppedVm()
        }
    }

    void prepare() {
        zone = env.inventoryByName("zone") as ZoneInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        vm = env.inventoryByName("vm") as VmInstanceInventory
    }

    void testBindAndUnbindStoppedVm() {
        logger.info("Test 001: create allocator domain, expect success")
        domain = createAllocatorDomain {
            delegate.name = "bind-domain"
            delegate.zoneUuid = zone.uuid
        } as AllocatorDomainInventory
        assert domain.uuid != null
        assert queryAllocatorDomain {
            delegate.conditions = ["uuid=${domain.uuid}"]
        }.size() == 1

        logger.info("Test 002: create extra VM with DiskAO then stop both VMs, expect Stopped")
        DiskAO rootDisk = DiskAO.rootDisk()
        rootDisk.setSize(gb(20))
        rootDisk.setPlatform("Linux")
        rootDisk.setGuestOsType("Linux")
        rootDisk.setArchitecture("x86_64")
        VmInstanceInventory created = createVmInstance {
            delegate.name = "disk-ao-vm"
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [rootDisk]
        } as VmInstanceInventory
        assert created.uuid != null

        stopVmInstance {
            delegate.uuid = vm.uuid
        }
        stopVmInstance {
            delegate.uuid = created.uuid
        }
        def stopped = queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        } as List<VmInstanceInventory>
        assert stopped.size() == 1
        assert stopped[0].state == "Stopped"
        def createdStopped = queryVmInstance {
            delegate.conditions = ["uuid=${created.uuid}"]
        } as List<VmInstanceInventory>
        assert createdStopped.size() == 1
        assert createdStopped[0].state == "Stopped"

        logger.info("Test 003: bind Stopped VM to domain via resource config, expect nextStartVms has VM and currentVms empty")
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

        GetAllocatorDomainVmsResult vms = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (vms.currentVms ?: []).isEmpty()
        assert (vms.nextStartVms ?: []).contains(vm.uuid)

        logger.info("Test 004: unbind VM by setting resource config to None, expect config None and GetVms empty")
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

        GetAllocatorDomainVmsResult afterUnbind = getAllocatorDomainVms {
            delegate.uuid = domain.uuid
        } as GetAllocatorDomainVmsResult
        assert (afterUnbind.currentVms ?: []).isEmpty()
        assert (afterUnbind.nextStartVms ?: []).isEmpty()

        logger.info("Test 099: delete allocator domain, expect it gone")
        deleteAllocatorDomain {
            delegate.uuid = domain.uuid
        }
        assert queryAllocatorDomain {
            delegate.conditions = ["uuid=${domain.uuid}"]
        }.isEmpty()
    }

    @Override
    void clean() {
        env.delete()
    }
}
