package org.zstack.test.integration.kvm.host

import org.zstack.core.db.Q
import org.zstack.core.thread.ThreadGlobalProperty
import org.zstack.header.host.HostVO
import org.zstack.header.image.ImageConstant
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by kayo on 2018/1/24.
 */
class ConcurrentlyAddHostCase extends SubCase{
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        ThreadGlobalProperty.MAX_THREAD_NUM = 40
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            cephBackupStorage {
                name="bs"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bs"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "image-root-volume"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "image-data-volume"
                    mediaType = ImageConstant.ImageMediaType.DataVolumeTemplate
                    url  = "http://zstack.org/download/test-volume.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    attachPrimaryStorage("ceph")
                    attachL2Network("l2")
                }

                cephPrimaryStorage {
                    name="ceph"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]
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
                attachBackupStorage("bs")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAdd200HostConcurrently()
        }
    }

    void testAdd200HostConcurrently() {
        def list = []
        def cluster = env.inventoryByName("cluster").uuid

        for (int i = 2;i < 202;i++) {
            def ip = String.format("127.0.0.%d", i)

            def thread = Thread.start {
                addKVMHost {
                    username = "test"
                    password = "password"
                    name = "host"
                    managementIp = ip
                    clusterUuid = cluster
                }
            }

            list.add(thread)
        }

        list.each {
            it.join()
        }

        retryInSecs(25, 1) {
            assert Q.New(HostVO.class).count() == 200
        }
    }
}
