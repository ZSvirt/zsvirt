package org.zstack.test.integration.computes.kvm

import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test
import org.zstack.testlib.premium.PremiumEnv

class KvmTest extends Test {
    static SpringSpec springSpec = PremiumEnv.makeSpring()

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }

    static EnvSpec cephPsCephBs2Host() {
        return makeEnv {
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
        }
    }
}
