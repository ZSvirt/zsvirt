package org.zstack.test.integration.kvm.host

import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.TimeUnit

class QueryCase extends SubCase{

    EnvSpec env

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            securityGroup()
            kvm()
        }
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
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

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
                name = "虚拟机"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            //sleep(TimeUnit.DAYS.toMillis(1))
            testQueryChneseVmName()
            testQueryHostContainsCpuSockets()
        }
    }

    void testQueryChneseVmName() {
        List<VmInstanceInventory> vms = queryVmInstance {
            conditions=["name=虚拟机"]
        }

        assert vms.size() == 1

        vms = queryVmInstance {
            conditions=["name~=%虚拟机%"]
        }

        assert vms.size() == 1
    }

    void testQueryHostContainsCpuSockets() {
        def inv = env.inventoryByName("kvm") as HostInventory

        def invs = queryHost {
            conditions=["uuid=${inv.uuid}"]
        } as List<HostInventory>

        assert invs != null
        assert !invs.isEmpty()
        assert invs[0].cpuSockets != null
    }

    @Override
    void clean() {
        env.delete()
    }

}
