package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.cluster.ClusterVO
import org.zstack.header.cluster.ClusterVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.compute.host.HostSystemTags
import org.zstack.header.storage.primary.PrimaryStorage
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.ApiException
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CreateVmWithHybridClusterCase extends SubCase {
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
                    name = "image_x86"
                    architecture = "x86_64"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "image_arm64"
                    architecture = "aarch64"
                    url  = "http://zstack.org/download/test2.qcow2"
                }

            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster_x86"
                    hypervisorType = "KVM"
                    architecture = "x86_64"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster_arm64"
                    hypervisorType = "KVM"
                    architecture = "aarch64"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                }


                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
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
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
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

            testCreateVm()
        }
    }

    void testCreateVm() {
        PrimaryStorageInventory ps = env.inventoryByName("local")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ImageInventory image_x86 = env.inventoryByName("image_x86")
        ImageInventory image_arm64 = env.inventoryByName("image_arm64")

        GetCandidateZonesClustersHostsForCreatingVmResult result = getCandidateZonesClustersHostsForCreatingVm {
            imageUuid = image_x86.uuid
            instanceOfferingUuid = instanceOffering.uuid
            l3NetworkUuids = [l3.uuid]
        } as GetCandidateZonesClustersHostsForCreatingVmResult
        for (cluster in result.getClusters()) {
            assert Q.New(ClusterVO.class).select(ClusterVO_.architecture).eq(ClusterVO_.uuid, cluster.uuid).findValue() == "x86_64"
        }

        GetCandidateZonesClustersHostsForCreatingVmResult result2 = getCandidateZonesClustersHostsForCreatingVm {
            imageUuid = image_arm64.uuid
            instanceOfferingUuid = instanceOffering.uuid
            l3NetworkUuids = [l3.uuid]
        } as GetCandidateZonesClustersHostsForCreatingVmResult
        for (cluster in result2.getClusters()) {
            assert Q.New(ClusterVO.class).select(ClusterVO_.architecture).eq(ClusterVO_.uuid, cluster.uuid).findValue() == "aarch64"
        }

        createVmInstance {
            name = "test1"
            instanceOfferingUuid = instanceOffering.uuid
            l3NetworkUuids = [l3.uuid]
            imageUuid = image_x86.uuid
        }
        assert Q.New(VmInstanceVO.class).notNull(VmInstanceVO_.uuid).count() == 1L
    }
}
