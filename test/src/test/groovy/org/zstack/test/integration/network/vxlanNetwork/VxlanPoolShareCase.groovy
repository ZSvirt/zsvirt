package org.zstack.test.integration.network.vxlanNetwork

import org.zstack.core.db.Q
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.sdk.L2VxlanNetworkPoolInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by MaJin on 2017-07-18.
 */
class VxlanPoolShareCase extends SubCase{
    EnvSpec env

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
            testShareVxlanPool()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testShareVxlanPool(){
        ZoneInventory zone = env.inventoryByName("zone") as ZoneInventory
        L2VxlanNetworkPoolInventory poolinv = createL2VxlanNetworkPool {
            name = "TestVxlanPool"
            zoneUuid = zone.getUuid()
        } as L2VxlanNetworkPoolInventory

        shareResource {
            resourceUuids = [poolinv.uuid]
            toPublic = true
        }

        assert Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, poolinv.uuid)
                .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                .isExists()

        deleteL2Network {
            uuid = poolinv.uuid
        }

        assert !Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, poolinv.uuid)
                .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                .isExists()
    }
}
