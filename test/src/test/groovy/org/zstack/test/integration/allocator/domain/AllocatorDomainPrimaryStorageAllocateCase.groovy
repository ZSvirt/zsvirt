package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig
import org.zstack.allocator.domain.header.AllocatorDomainState
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainRuleParam
import org.zstack.header.configuration.ConfigAO
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class AllocatorDomainPrimaryStorageAllocateCase extends SubCase {
    EnvSpec env
    ZoneInventory zone
    L3NetworkInventory l3
    HostInventory host1
    PrimaryStorageInventory nfs1
    PrimaryStorageInventory nfs2

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

                    attachPrimaryStorage("nfs1")
                    attachPrimaryStorage("nfs2")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs1"
                    url = "127.0.0.1:/nfs1"
                    totalCapacity = SizeUnit.TERABYTE.toByte(10)
                    availableCapacity = SizeUnit.TERABYTE.toByte(10)
                }

                nfsPrimaryStorage {
                    name = "nfs2"
                    url = "127.0.0.1:/nfs2"
                    totalCapacity = SizeUnit.TERABYTE.toByte(100)
                    availableCapacity = SizeUnit.TERABYTE.toByte(100)
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
            testUnspecifiedLandsOnIncludedPrimaryStorage()
            testDisabledDoesNotConstrainPrimaryStorage()
            testNoPrimaryStorageRulesDoesNotConstrain()
        }
    }

    void prepare() {
        zone = env.inventoryByName("zone") as ZoneInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        host1 = env.inventoryByName("kvm1") as HostInventory
        nfs1 = env.inventoryByName("nfs1") as PrimaryStorageInventory
        nfs2 = env.inventoryByName("nfs2") as PrimaryStorageInventory
    }

    void testUnspecifiedLandsOnIncludedPrimaryStorage() {
        logger.info("Test 001: unspecified root PS lands on domain Include NFS, not the other shared PS")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "include-nfs1"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: nfs1.uuid, resourceType: "PrimaryStorageVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createUserVm(domain.uuid, "unspecified-ps-vm")
        assert rootPrimaryStorageUuid(vm) == nfs1.uuid

        destroyAndDeleteDomain(vm, domain)
    }

    void testDisabledDoesNotConstrainPrimaryStorage() {
        logger.info("Test 101: Disabled domain with Include nfs1 does not constrain unspecified PS")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "disabled-include-nfs1"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: nfs1.uuid, resourceType: "PrimaryStorageVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory
        domain = updateAllocatorDomain {
            delegate.uuid = domain.uuid
            delegate.state = AllocatorDomainState.Disabled.toString()
        } as AllocatorDomainInventory
        assert domain.state == AllocatorDomainState.Disabled.toString()

        // nfs2 has more capacity, so unconstrained allocate lands there.
        // If Disabled still applied Include nfs1, this would be nfs1 (same as Test 001).
        VmInstanceInventory vm = createUserVm(domain.uuid, "disabled-ps-vm")
        assert rootPrimaryStorageUuid(vm) == nfs2.uuid

        destroyAndDeleteDomain(vm, domain)
    }

    void testNoPrimaryStorageRulesDoesNotConstrain() {
        logger.info("Test 201: host-only domain does not constrain unspecified PS")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "host-only"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        // nfs2 has more capacity; host-only rules must not pin the root disk to nfs1.
        VmInstanceInventory vm = createUserVm(domain.uuid, "no-ps-rule-vm")
        assert rootPrimaryStorageUuid(vm) == nfs2.uuid

        destroyAndDeleteDomain(vm, domain)
    }

    private VmInstanceInventory createUserVm(String domainUuid, String name) {
        return createVmInstance {
            delegate.name = name
            delegate.cpuNum = 4
            delegate.memorySize = gb(8)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                    [
                            "boot"         : true,
                            "size"         : gb(20),
                            "platform"     : "Linux",
                            "guestOsType"  : "Linux",
                            "architecture" : "x86_64"
                    ]
            ]
            delegate.systemTags = [
                    ConfigAO.buildResourceConfig(
                            AllocatorDomainGlobalConfig.CATEGORY,
                            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                            domainUuid)
            ]
        } as VmInstanceInventory
    }

    private String rootPrimaryStorageUuid(VmInstanceInventory vm) {
        List<VolumeInventory> volumes = queryVolume {
            delegate.conditions = ["uuid=${vm.rootVolumeUuid}"]
        } as List<VolumeInventory>
        assert volumes.size() == 1
        return volumes.get(0).primaryStorageUuid
    }

    private void destroyAndDeleteDomain(VmInstanceInventory vm, AllocatorDomainInventory domain) {
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
        if (env != null) {
            env.delete()
        }
    }
}
