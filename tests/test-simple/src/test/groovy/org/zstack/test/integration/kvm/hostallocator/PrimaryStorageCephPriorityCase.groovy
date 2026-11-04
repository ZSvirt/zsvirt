package org.zstack.test.integration.kvm.hostallocator

import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2017/11/7.
 * if BS = CephBS, and we have 2 Ps(Ceph, LocalStorage), we should choose Ceph first
 */
class PrimaryStorageCephPriorityCase extends SubCase {
    EnvSpec env

    HostInventory host

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone{
                name = "zone"

                cephPrimaryStorage {
                    name="ceph-pri"
                    description="Test"
                    totalCapacity = gb(100)
                    availableCapacity= gb(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]
                }

                cluster {
                    name = "local-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host5"
                        managementIp = "127.0.0.5"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local-pri")
                    attachL2Network("l2")
                }

                cluster {
                    name = "ceph-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host6"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("ceph-pri")
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

                localPrimaryStorage {
                    name = "local-pri"
                    url = "/local_ps"
                }

                cephBackupStorage {
                    name="ceph-bk"
                    description="Test"
                    url = "/bk"
                    fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]
                    totalCapacity = gb(100)
                    availableCapacity= gb(100)

                    image {
                        name = "image"
                        url  = "http://zstack.org/download/image.qcow2"
                    }
                }

                attachBackupStorage("ceph-bk")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testChooseCephPs()
        }
    }

    void testChooseCephPs() {
        logger.info("Test 001: image in ceph bs -> VM from this image will start on ceph on default")
        host = env.inventoryByName("host6") as HostInventory
        def image = env.inventoryByName("image") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        def vm1 = createVmInstance {
            name = "vm1"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            cpuNum = 2
            memorySize = gb(4)
        } as VmInstanceInventory

        assert vm1.hostUuid == host.uuid
    }
}
