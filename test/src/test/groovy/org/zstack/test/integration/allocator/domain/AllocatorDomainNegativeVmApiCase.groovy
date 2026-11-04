package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.header.AllocatorDomainConstant
import org.zstack.allocator.domain.header.AllocatorDomainErrors
import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig
import org.zstack.allocator.domain.header.AllocatorDomainSystemTags
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

class AllocatorDomainNegativeVmApiCase extends SubCase {
    EnvSpec env
    ZoneInventory zone
    L3NetworkInventory l3
    L3NetworkInventory l3Other
    HostInventory host1
    HostInventory host2
    PrimaryStorageInventory local
    PrimaryStorageInventory local2

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
                    attachPrimaryStorage("local2")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps2"
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

                    l3Network {
                        name = "l3-other"

                        ip {
                            startIp = "12.16.11.10"
                            endIp = "12.16.11.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.11.1"
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
            testSpecifiedHostOutOfDomainRejected()
            testSpecifiedPrimaryStorageOutOfDomainRejected()
            testAttachL3OutOfDomainRejected()
            testAttachDataVolumeOutOfDomainRejected()
            testCreateVmWithCurrentValueTagRejected()
        }
    }

    void prepare() {
        zone = env.inventoryByName("zone") as ZoneInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        l3Other = env.inventoryByName("l3-other") as L3NetworkInventory
        host1 = env.inventoryByName("kvm1") as HostInventory
        host2 = env.inventoryByName("kvm2") as HostInventory
        local = env.inventoryByName("local") as PrimaryStorageInventory
        local2 = env.inventoryByName("local2") as PrimaryStorageInventory
    }

    void testSpecifiedHostOutOfDomainRejected() {
        logger.info("Test 001: specified hostUuid not in domain host allowed set, expect create VM ALLOCATOR.DOMAIN.4001")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "include-host1-only"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: host1.uuid, resourceType: "HostVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        expectApiFailure({
            createVmInstance {
                delegate.name = "host-out-vm"
                delegate.cpuNum = 4
                delegate.memorySize = gb(8)
                delegate.l3NetworkUuids = [l3.uuid]
                delegate.hostUuid = host2.uuid
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
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.DOMAIN_UUID] == domain.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.HOST_UUID] == host2.uuid
        }

        deleteAllocatorDomain {
            delegate.uuid = domain.uuid
        }
    }

    void testSpecifiedPrimaryStorageOutOfDomainRejected() {
        logger.info("Test 101: specified diskAO.primaryStorageUuid not in domain PS allowed set, expect create VM ALLOCATOR.DOMAIN.4001")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "include-local-ps"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: local.uuid, resourceType: "PrimaryStorageVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        expectApiFailure({
            createVmInstance {
                delegate.name = "ps-specified-out-vm"
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
                        "architecture": "x86_64",
                        "primaryStorageUuid": local2.uuid
                    ]
                ]
                delegate.systemTags = [
                        ConfigAO.buildResourceConfig(
                                AllocatorDomainGlobalConfig.CATEGORY,
                                AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                                domain.uuid)
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.DOMAIN_UUID] == domain.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID] == local2.uuid
        }

        deleteAllocatorDomain {
            delegate.uuid = domain.uuid
        }
    }

    void testAttachL3OutOfDomainRejected() {
        logger.info("Test 201: attach L3 not in domain allowed set, expect ALLOCATOR.DOMAIN.4001")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "include-l3"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: l3.uuid, resourceType: "L3NetworkVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "attach-l3-vm"
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

        expectApiFailure({
            attachL3NetworkToVm {
                delegate.vmInstanceUuid = vm.uuid
                delegate.l3NetworkUuid = l3Other.uuid
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.DOMAIN_UUID] == domain.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.L3_UUID] == l3Other.uuid
        }

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

    void testAttachDataVolumeOutOfDomainRejected() {
        logger.info("Test 301: attach data volume whose PS is not in domain allowed set, expect ALLOCATOR.DOMAIN.4001")
        AllocatorDomainInventory domain = createAllocatorDomain {
            delegate.name = "include-local-for-attach"
            delegate.zoneUuid = zone.uuid
            delegate.rules = [
                    new AllocatorDomainRuleParam(resourceUuid: local.uuid, resourceType: "PrimaryStorageVO", effect: "Include")
            ]
            delegate.scenes = ["NewCreate", "Start"]
        } as AllocatorDomainInventory

        VmInstanceInventory vm = createVmInstance {
            delegate.name = "attach-vol-vm"
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
                    "architecture": "x86_64",
                    "primaryStorageUuid": local.uuid
                ]
            ]
            delegate.systemTags = [
                    ConfigAO.buildResourceConfig(
                            AllocatorDomainGlobalConfig.CATEGORY,
                            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME,
                            domain.uuid)
            ]
        } as VmInstanceInventory

        VolumeInventory volume = createDataVolume {
            delegate.name = "out-of-domain-vol"
            delegate.diskSize = gb(1)
            delegate.primaryStorageUuid = local2.uuid
            delegate.systemTags = ["localStorage::hostUuid::${host1.uuid}".toString()]
        } as VolumeInventory

        expectApiFailure({
            attachDataVolumeToVm {
                delegate.vmInstanceUuid = vm.uuid
                delegate.volumeUuid = volume.uuid
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.DOMAIN_UUID] == domain.uuid
            assert delegate.opaque[AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID] == local2.uuid
        }

        deleteDataVolume {
            delegate.uuid = volume.uuid
        }
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

    void testCreateVmWithCurrentValueTagRejected() {
        logger.info("Test 401: create VM with schedulingDomain:: system tag, expect ALLOCATOR.DOMAIN.4002")
        expectApiFailure({
            createVmInstance {
                delegate.name = "current-tag-vm"
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
                        AllocatorDomainSystemTags.SCHEDULING_DOMAIN.instantiateTag(
                                [(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN): "11111111111111111111111111111111"])
                ]
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.DOMAIN_UUID_CHANGE_NOT_ALLOWED.toString()
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
        queryAllocatorDomain {}.each { AllocatorDomainInventory domain ->
            if (domain != null) {
                deleteAllocatorDomain {
                    delegate.uuid = domain.uuid
                }
            }
        }
        env.delete()
    }
}
