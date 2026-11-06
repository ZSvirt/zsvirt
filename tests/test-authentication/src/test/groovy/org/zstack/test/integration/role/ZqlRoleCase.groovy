package org.zstack.test.integration.role

import org.zstack.sdk.VmInstanceInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class ZqlRoleCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm1

    @Override
    void setup() {
        useSpring(RoleTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            cephBackupStorage {
                name="cephBs"
                description="Test"
                totalCapacity = gb(100)
                availableCapacity= gb(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.11"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.12"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("cephPs")
                    attachL2Network("l2")
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                cephPrimaryStorage {
                    name="cephPs"
                    description="Test"
                    totalCapacity = gb(100)
                    availableCapacity= gb(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]
                }

                attachBackupStorage("cephBs")
            }

            vm {
                name = "vm1"
                cpu = 4
                memoryGB(8)
                useCluster("cluster")
                useHost("kvm1")
                useL3Networks("l3")
                useImage("image")
            }

            account {
                name = "accountAdmin"
                password = "password"
                asSystemAdmin()
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testZql()
        }
    }

    void prepare() {
        vm1 = env.inventoryByName("vm1") as VmInstanceInventory
    }

    void testZql() {
        logger.info("Test 001")

        withAccountSession("accountAdmin", "password") {
            def result001 = zqlQueryFull("""\
query PrimaryStorage where (
    uuid not in ('') and
    type not in ('SharedMountPoint') and
    uuid in (getapi(
        api='GetPrimaryStorageCandidatesForVmMigration',output='inventories.uuid',vmInstanceUuid='${vm1.uuid}',withDataVolumes=false,migrateStorageOnly=false)
    )
) return with (total)""".toString())

            logger.info(jsonString(result001))
            assert result001.total == 1
            assert result001.inventories.size() == 1
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
