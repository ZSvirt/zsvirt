package org.zstack.test.integration.storage

import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit
/**
 * Created by Administrator on 2017-03-01.
 */
class CephEnv {
    static EnvSpec oneCephBSandtwoPs(){
        return Test.makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(4)
                cpu = 2
            }

            zone{
                name = "zone"

                cephPrimaryStorage {
                    name="ceph-pri"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]
                }

                cluster {
                    name = "local-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "host2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "host3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "host4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

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
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)

                    image {
                        name = "image"
                        url  = "http://zstack.org/download/image.qcow2"
                    }
                }

                attachBackupStorage("ceph-bk")
            }
        }
    }
}
