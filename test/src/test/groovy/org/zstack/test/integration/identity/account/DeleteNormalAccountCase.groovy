package org.zstack.test.integration.identity.account

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by miao on 17-6-7.
 */
class DeleteNormalAccountCase extends SubCase {
    EnvSpec env
    AccountInventory accountInventory, toConcurrentDeleteAccount

    def DOC = """
1. create an environment with admin account and normal account and other resources
2. delete all AccountResourceRefVO purposely
3. delete normal account to trigger admin adopting all orphaned resources
4. check
"""

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

            toConcurrentDeleteAccount = createAccount {
                name = "test1"
                password = "password"
            } as AccountInventory

            VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Delay.toString())

            testAdminAdoptOrphanedResourceAfterDeletedNormalAccount()
        }
    }

    void testAdminAdoptOrphanedResourceAfterDeletedNormalAccount() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        // gather resources which should be referenced in AccountResourceRefVO and belong to admin
        // these should later belong to admin again
        List<String> resourceUuids = Q.New(AccountResourceRefVO.class)
                .select(AccountResourceRefVO_.resourceUuid)
                .eq(AccountResourceRefVO_.accountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .listValues()

        // change one vm's owner to normal account, leave an appliance vm to admin
        changeResourceOwner {
            accountUuid = accountInventory.getUuid()
            resourceUuid = vm.getUuid()
        }

        // remove all AccountResourceRefVO records purposely
        Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .list()
                .stream()
                .forEach { it ->
            logger.debug(String.format("resourceUuid:%s, resourceType:%s",
                    (it as AccountResourceRefVO).getResourceUuid(),
                    (it as AccountResourceRefVO).getResourceType()))
        }

        SQL.New(AccountResourceRefVO.class).hardDelete()

        // delete normal account to trigger adoption

        AtomicInteger success = new AtomicInteger(0)
        for (String toDeleteUuid : [accountInventory.uuid, toConcurrentDeleteAccount.uuid]) {
            String tempUuid = toDeleteUuid
            Thread.start {
                deleteAccount {
                    uuid = tempUuid
                }
                success.addAndGet(1)
            }
        }


        // check admin adopt all
        retryInSecs {
            assert success.get() == 2
            def size = Q.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourceUuid, resourceUuids)
                    .eq(AccountResourceRefVO_.accountUuid, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .count()
            assert size == resourceUuids.size() as Long
        }
    }
}
