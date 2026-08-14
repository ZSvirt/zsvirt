package org.zstack.test.integration.identity.account

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by miao on 17-6-7.
 */
class DelayDeleteResourceCase extends SubCase {
    EnvSpec env
    AccountInventory accountInventory

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        // one base vm, with a data volume
        env = makeEnv {
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

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

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
                cpu = 4
                memoryGB(8)
                useImage("image1")
                useL3Networks("l3")
                disk {
                    boot = true
                }
                disk {
                    sizeGB(20)
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            accountInventory = createAccount {
                name = "test"
                password = "password"
            } as AccountInventory

            VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Delay.toString())

            testAdminAdoptOrphanedResourceAfterDeletedNormalAccount()
        }
    }

    void testAdminAdoptOrphanedResourceAfterDeletedNormalAccount() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        VolumeInventory vol = vm.getAllVolumes().find { i -> i.getUuid() != vm.getRootVolumeUuid() }

        List<String> resourceUuids = new ArrayList<>()
        resourceUuids.add(vm.getUuid())
        resourceUuids.add(vol.getUuid())
        resourceUuids.add(vm.getRootVolumeUuid())

        // check initial owner
        retryInSecs {
            def size = Q.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourceUuid, resourceUuids)
                    .eq(AccountResourceRefVO_.accountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .count()
            assert size == resourceUuids.size() as Long
        }

        changeResourceOwner {
            accountUuid = accountInventory.getUuid()
            resourceUuid = vm.getUuid()
        }

        // make sure change owner success
        retryInSecs {
            def size = Q.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourceUuid, resourceUuids)
                    .eq(AccountResourceRefVO_.accountUuid, accountInventory.getUuid())
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .count()
            assert size == resourceUuids.size() as Long
        }

        deleteAccount {
            uuid = accountInventory.getUuid()
        }

        // check admin adopt all
        retryInSecs {
            def size = Q.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourceUuid, resourceUuids)
                    .eq(AccountResourceRefVO_.accountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .count()
            assert size == resourceUuids.size() as Long
        }
    }
}
