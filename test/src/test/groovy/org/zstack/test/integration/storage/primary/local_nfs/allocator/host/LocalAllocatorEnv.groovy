package org.zstack.test.integration.storage.primary.local_nfs.allocator.host

import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

/**
 * Created by mingjian.deng on 2017/12/22.
 */
class LocalAllocatorEnv {
    static EnvSpec tenHosts() {
        return Test.makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
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
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

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

                    kvm {
                        name = "kvm5"
                        managementIp = "127.0.0.5"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm6"
                        managementIp  = "127.0.0.6"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm7"
                        managementIp = "127.0.0.7"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm8"
                        managementIp = "127.0.0.8"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm9"
                        managementIp = "127.0.0.9"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "172.20.0.1:/nfs_root"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(10000)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(10000)
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

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
        }
    }
}
