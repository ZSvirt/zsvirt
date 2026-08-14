package org.zstack.test.integration.longjob


import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.cluster.UpdateClusterOSMsg
import org.zstack.header.longjob.LongJobState
import org.zstack.header.longjob.LongJobVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.LongJobInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by GuoYi on 2019/12/15
 */
class FailedLongJobCase extends SubCase {
    EnvSpec env

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
            }
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
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

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster3"
                    hypervisorType = "Simulator"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "127.0.0.3:/nfs_root"
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

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm1"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useHost("kvm1")
            }

            vm {
                name = "vm2"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useHost("kvm3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testUpdateClusterWithError()
        }
    }

    void testUpdateClusterWithError() {
        ClusterInventory cls = env.inventoryByName("cluster1") as ClusterInventory

        env.message(UpdateClusterOSMsg.class) { UpdateClusterOSMsg msg, CloudBus bus ->
            bus.replyErrorByMessageType(msg, "on purpose")
        }

        // try to update cluster os
        def jobInv = updateClusterOS {
            uuid = cls.uuid
            excludePackages = ["kernel", "systemd*"]
        } as LongJobInventory

        assert jobInv.getJobName() == "APIUpdateClusterOSMsg"
        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Failed
        }

        env.revokeMessage(UpdateClusterOSMsg.class, null)
    }
}
