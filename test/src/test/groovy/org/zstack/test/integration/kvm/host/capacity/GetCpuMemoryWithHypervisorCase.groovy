package org.zstack.test.integration.kvm.host.capacity

import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetCpuMemoryCapacityResult
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.kvm.hostallocator.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by kayo on 2018/4/3.
 */
class GetCpuMemoryWithHypervisorCase extends SubCase {
    EnvSpec env

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
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 2
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
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
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(10)
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(10)
                    }

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(10)
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
        }
    }

    @Override
    void test() {
        env.create {
            testGetCpuMemoryCapacityByHost()
            testGetCpuMemoryCapacityByCluster()
            testGetCpuMemoryCapacityByZone()
        }
    }

    void testGetCpuMemoryCapacityByHost() {
        def host = env.inventoryByName("kvm1") as KVMHostInventory
        GetCpuMemoryCapacityResult result = getCpuMemoryCapacity {
            hostUuids = [host.uuid]
        }

        assert result.managedCpuNum == 8
        assert result.totalMemory == SizeUnit.GIGABYTE.toByte(10)

        result = getCpuMemoryCapacity {
            hostUuids = [host.uuid]
            hypervisorType = "KVM"
        }

        assert result.managedCpuNum == 8
        assert result.totalMemory == SizeUnit.GIGABYTE.toByte(10)

        result = getCpuMemoryCapacity {
            hostUuids = [host.uuid]
            hypervisorType = "ESX"
        }

        assert result.managedCpuNum == 0
        assert result.totalMemory == 0
    }

    void testGetCpuMemoryCapacityByCluster() {
        def cluster = env.inventoryByName("cluster") as ClusterInventory

        GetCpuMemoryCapacityResult result = getCpuMemoryCapacity {
            clusterUuids = [cluster.uuid]
        }

        assert result.managedCpuNum == 8 * 3
        assert result.totalMemory == SizeUnit.GIGABYTE.toByte(10) * 3

        result = getCpuMemoryCapacity {
            clusterUuids = [cluster.uuid]
            hypervisorType = "KVM"
        }

        assert result.managedCpuNum == 8 * 3
        assert result.totalMemory == SizeUnit.GIGABYTE.toByte(10) * 3

        result = getCpuMemoryCapacity {
            clusterUuids = [cluster.uuid]
            hypervisorType = "ESX"
        }

        assert result.managedCpuNum == 0
        assert result.totalMemory == 0
    }

    void testGetCpuMemoryCapacityByZone() {
        def zone = env.inventoryByName("zone") as ZoneInventory

        GetCpuMemoryCapacityResult result = getCpuMemoryCapacity {
            zoneUuids = [zone.uuid]
        }

        assert result.managedCpuNum == 8 * 3
        assert result.totalMemory == SizeUnit.GIGABYTE.toByte(10) * 3

        result = getCpuMemoryCapacity {
            zoneUuids = [zone.uuid]
            hypervisorType = "KVM"
        }

        assert result.managedCpuNum == 8 * 3
        assert result.totalMemory == SizeUnit.GIGABYTE.toByte(10) * 3

        result = getCpuMemoryCapacity {
            zoneUuids = [zone.uuid]
            hypervisorType = "ESX"
        }

        assert result.managedCpuNum == 0
        assert result.totalMemory == 0
    }
}
