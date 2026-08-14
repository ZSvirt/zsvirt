package org.zstack.test.integration.network.vxlanNetwork

import org.zstack.header.identity.AccountConstant
import org.zstack.sdk.*
import org.zstack.sdk.identity.role.RoleInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by MaJin on 2017-07-18.
 */
class VxlanQuotaCase extends SubCase{
    EnvSpec env
    AccountInventory accountInventory

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(20)
                    }

                    attachPrimaryStorage("local")

                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")

                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                attachBackupStorage("sftp")
            }

            zone {
                name = "zone2"
                description = "test"
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
            attachPredefineRoles(accountInventory.uuid, "networks")

            testVxlanQuota()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testVxlanQuota() {
        ZoneInventory zone = env.inventoryByName("zone") as ZoneInventory
        L2VxlanNetworkPoolInventory poolinv = createL2VxlanNetworkPool {
            name = "TestVxlanPool"
            zoneUuid = zone.getUuid()

        } as L2VxlanNetworkPoolInventory

        shareResource {
            resourceUuids = [poolinv.uuid]
            toPublic = true
        }

        createVniRange {
            delegate.startVni = 100
            delegate.endVni = 10000
            delegate.l2NetworkUuid = poolinv.getUuid()
            delegate.name = "TestRange1"
        }

        SessionInventory sessionInventory1 = logInByAccount {
            accountName = "test"
            password = "password"
        }

        shareResource {
            resourceUuids = [poolinv.uuid]
            toPublic = true
        }

        /* default vxlan.num is 8 */
        for (int i = 1; i < 9; i++) {
            createL2VxlanNetwork {
                delegate.poolUuid = poolinv.getUuid()
                delegate.name = String.format("TestVxlan-%s", i.toString())
                delegate.zoneUuid = zone.getUuid()
                delegate.sessionId = sessionInventory1.uuid
            }
        }

        /* create 9th vxlan network will fail */
        CreateL2VxlanNetworkAction action1 = new CreateL2VxlanNetworkAction()
        action1.poolUuid = poolinv.getUuid()
        action1.name = "TestVxlan-9";
        action1.zoneUuid = zone.getUuid()
        action1.sessionId = sessionInventory1.uuid
        CreateL2VxlanNetworkAction.Result res1 = action1.call()
        assert res1.error != null

        SessionInventory sessionInventory2 = logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        }

        /* update vxlan.num to 10 */
        UpdateQuotaAction action = new UpdateQuotaAction();
        action.identityUuid = accountInventory.uuid
        action.name = "vxlan.num"
        action.value = 10
        action.sessionId = sessionInventory2.uuid
        UpdateQuotaAction.Result result = action.call();
        assert result.error == null

        SessionInventory sessionInventory3 = logInByAccount {
            accountName = "test"
            password = "password"
        }

        /* default vxlan.num is 8 */
        for (int i = 9; i < 11; i++) {
            createL2VxlanNetwork {
                delegate.poolUuid = poolinv.getUuid()
                delegate.name = String.format("TestVxlan-%s", i.toString())
                delegate.zoneUuid = zone.getUuid()
                delegate.sessionId = sessionInventory3.uuid
            }
        }

        /* create 11th vxlan network will fail */
        CreateL2VxlanNetworkAction action2 = new CreateL2VxlanNetworkAction();
        action2.name = "TestVxlan-11";
        action2.poolUuid = poolinv.getUuid()
        action2.zoneUuid = zone.getUuid()
        action2.sessionId = sessionInventory3.uuid
        CreateL2VxlanNetworkAction.Result res2 = action2.call()
        assert res2.error != null

        deleteL2Network{
            uuid = poolinv.uuid
        }
    }
}
