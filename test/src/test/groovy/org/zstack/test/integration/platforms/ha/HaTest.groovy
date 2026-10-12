package org.zstack.test.integration.platforms.ha

import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test

class HaTest extends Test {
    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }

    static EnvSpec normal() {
        return makeEnv {
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
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        totalMem = gb(256)
                        totalCpu = 256
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalMem = gb(256)
                        totalCpu = 256
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
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
                useHost("kvm1")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    static EnvSpec smpCephNfsOneCluster() {
        return makeEnv {
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
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        totalMem = gb(256)
                        totalCpu = 256
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalMem = gb(256)
                        totalCpu = 256
                    }

                    attachPrimaryStorage("nfs")
                    attachPrimaryStorage("smp")
                    attachPrimaryStorage("ceph-pri")
                    attachL2Network("l2")
                }

                cephPrimaryStorage {
                    name = "ceph-pri"
                    url = "ceph://pri-1"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@127.0.0.11/?monPort=7777"]
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/test"
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
                name = "vm"
                cpu = 4
                memoryGB(8)
                useHost("kvm1")
                useImage("image1")
                useL3Networks("l3")
                usePrimaryStorage("nfs")
            }
        }
    }

    static EnvSpec cephOneCluster() {
        return makeEnv {
            cephBackupStorage {
                name="ceph-bs"
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@127.0.0.11/?monPort=7777"]

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
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        totalMem = gb(256)
                        totalCpu = 256
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalMem = gb(256)
                        totalCpu = 256
                    }

                    attachPrimaryStorage("ceph-pri")
                    attachL2Network("l2")
                }

                cephPrimaryStorage {
                    name = "ceph-pri"
                    url = "ceph://pri-1"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@127.0.0.11/?monPort=7777"]
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

                attachBackupStorage("ceph-bs")
            }

            vm {
                name = "vm"
                cpu = 4
                memoryGB(8)
                useHost("kvm1")
                useImage("image1")
                useL3Networks("l3")
                usePrimaryStorage("ceph-pri")
            }
        }
    }
}
