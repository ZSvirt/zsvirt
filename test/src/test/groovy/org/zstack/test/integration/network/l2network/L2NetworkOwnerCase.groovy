package org.zstack.test.integration.network.l2network

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2019-02-22.
 */
class L2NetworkOwnerCase extends SubCase{

    EnvSpec env
    DatabaseFacade dbf
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

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }
            
            account {
                name = "normalAccount"
                password = "password"
                predefineRole("networks")
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
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2NoVlan")
                    attachL2Network("l2Vlan")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2NoVlan"
                    physicalInterface = "eth0"

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

                l2VlanNetwork {
                    name = "l2Vlan"
                    vlan = 3001
                    physicalInterface = "eth0"

                    l3Network {
                        name = "pubL3_2"
                        category = "Public"
                        ip {
                            startIp = "11.168.200.10"
                            endIp = "11.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.200.1"
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
            testL2NetworkOwner()
            changeL2NetworkOwner()
        }

    }

    void testL2NetworkOwner() {
        L2NetworkInventory l2Vlan = env.inventoryByName("l2Vlan")
        L2NetworkInventory l2NoVlan = env.inventoryByName("l2NoVlan")

        AccountResourceRefVO ref = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, l2Vlan.uuid)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .find()
        assert Test.currentEnvSpec?.session?.accountUuid == ref.accountUuid

        ref = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, l2NoVlan.uuid)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .find()
        assert Test.currentEnvSpec?.session?.accountUuid == ref.accountUuid
    }

    void changeL2NetworkOwner() {
        L2NetworkInventory l2Vlan = env.inventoryByName("l2Vlan")
        L2NetworkInventory l2NoVlan = env.inventoryByName("l2NoVlan")
        AccountInventory account = env.inventoryByName("normalAccount")

        SessionInventory sessionAccount = logInByAccount {
            accountName = "normalAccount"
            password = "password"
        }

        changeResourceOwner {
            resourceUuid = l2Vlan.uuid
            accountUuid = account.uuid
        }

        AccountResourceRefVO ref = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, l2Vlan.uuid)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .find()
        assert account.uuid == ref.accountUuid

        List result = queryL2Network {
            sessionId = sessionAccount.uuid
            conditions = [
                    "uuid=${l2Vlan.uuid}".toString()
            ]
        }
        assert 1 == result.size()

        changeResourceOwner {
            resourceUuid = l2NoVlan.uuid
            accountUuid = account.uuid
        }

        ref = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, l2NoVlan.uuid)
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .find()
        assert account.uuid == ref.accountUuid

        result = queryL2Network {
            sessionId = sessionAccount.uuid
            conditions = [
                    "uuid=${l2NoVlan.uuid}".toString()
            ]
        }
        assert 1 == result.size()
    }
    
    @Override
    void clean() {
        env.delete()
    }
}
