package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.header.AllocatorDomainConstant
import org.zstack.allocator.domain.header.AllocatorDomainErrors
import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig
import org.zstack.sdk.GetResourceConfigResult
import org.zstack.sdk.GlobalConfigInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AllocatorDomainNegativeBindCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    String missingUuid = "11111111111111111111111111111111"

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
            vm = env.inventoryByName("vm") as VmInstanceInventory
            testBindMissingDomain()
            testUpdateGlobalConfigRejected()
        }
    }

    void testBindMissingDomain() {
        logger.info("Test 001: stop VM, expect Stopped")
        stopVmInstance {
            delegate.uuid = vm.uuid
        }
        def stopped = queryVmInstance {
            delegate.conditions = ["uuid=${vm.uuid}"]
        } as List<VmInstanceInventory>
        assert stopped.size() == 1
        assert stopped[0].state == "Stopped"

        logger.info("Test 002: bind Stopped VM to missing domain uuid, expect API failure ALLOCATOR.DOMAIN.2001")
        expectApiFailure({
            updateResourceConfig {
                delegate.resourceUuid = vm.uuid
                delegate.category = AllocatorDomainGlobalConfig.CATEGORY
                delegate.name = AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
                delegate.value = missingUuid
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.DOMAIN_NOT_FOUND.toString()
            assert delegate.opaque
            assert delegate.opaque[AllocatorDomainConstant.Opaque.DOMAIN_UUID] == missingUuid
        }

        logger.info("Test 003: get resource config after failed bind, expect value still None")
        GetResourceConfigResult cfg = getResourceConfig {
            delegate.resourceUuid = vm.uuid
            delegate.category = AllocatorDomainGlobalConfig.CATEGORY
            delegate.name = AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
        } as GetResourceConfigResult
        assert cfg.value == AllocatorDomainConstant.UNBOUND_DOMAIN_UUID
    }

    void testUpdateGlobalConfigRejected() {
        logger.info("Test 101: update vm.scheduling.domainUuid as global config, expect API failure ALLOCATOR.DOMAIN.2003")
        expectApiFailure({
            updateGlobalConfig {
                delegate.category = AllocatorDomainGlobalConfig.CATEGORY
                delegate.name = AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
                delegate.value = missingUuid
            }
        }) {
            assert delegate.code == AllocatorDomainErrors.GLOBAL_CONFIG_UPDATE_NOT_ALLOWED.toString()
        }

        logger.info("Test 102: query global config after failed update, expect value still None")
        def configs = queryGlobalConfig {
            delegate.conditions = [
                    "category=${AllocatorDomainGlobalConfig.CATEGORY}",
                    "name=${AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME}"
            ]
        } as List<GlobalConfigInventory>
        assert configs.size() == 1
        assert configs[0].value == AllocatorDomainConstant.UNBOUND_DOMAIN_UUID
        assert configs[0].defaultValue == AllocatorDomainConstant.UNBOUND_DOMAIN_UUID
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
