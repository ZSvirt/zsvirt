package org.zstack.test.integration.kvm.vm

import org.zstack.header.errorcode.SysErrors
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by AlanJager on 2017/4/18.
 */
class CreateVmMatchWrongClusterHostOrWrongPrimaryStorageHostCase extends SubCase {
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
                memory = SizeUnit.GIGABYTE.toByte(4)
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
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
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
                        totalCpu = 40
                        totalMem = SizeUnit.GIGABYTE.toByte(40)
                    }

                    attachPrimaryStorage("local1")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalCpu = 40
                        totalMem = SizeUnit.GIGABYTE.toByte(40)
                    }

                    attachPrimaryStorage("local2")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local1"
                    url = "/local_ps"
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps"
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
        }
    }

    @Override
    void test() {
        env.create {
            testCreateVmOnSpecificHostButPrimaryStorageNotBelongtoCluster()
        }
    }

    void testCreateVmOnSpecificHostButPrimaryStorageNotBelongtoCluster() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        L3NetworkInventory l3NetworkInventory = env.inventoryByName("l3")
        PrimaryStorageInventory ps1 = env.inventoryByName("local1")
        PrimaryStorageInventory ps2 = env.inventoryByName("local2")
        ClusterInventory cluster1 = env.inventoryByName("cluster1")
        ClusterInventory cluster2 = env.inventoryByName("cluster2")
        HostInventory host1 = env.inventoryByName("kvm1")

        createVmInstance {
            name = "test"
            l3NetworkUuids = [l3NetworkInventory.uuid]
            imageUuid = image.uuid
            clusterUuid = cluster1.uuid
            primaryStorageUuidForRootVolume = ps1.uuid
            instanceOfferingUuid = instanceOffering.uuid
        }

        // choose host and ps not in one cluster
        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "test-1"
        action.imageUuid = image.uuid
        action.l3NetworkUuids = [l3NetworkInventory.uuid]
        action.hostUuid = host1.uuid
        action.primaryStorageUuidForRootVolume = ps2.uuid
        action.instanceOfferingUuid = instanceOffering.uuid
        action.sessionId = adminSession()
        CreateVmInstanceAction.Result result = action.call()
        assert result.error != null

        // choose cluster and ps without ref
        CreateVmInstanceAction action2 = new CreateVmInstanceAction()
        action2.name = "test-1"
        action2.imageUuid = image.uuid
        action2.l3NetworkUuids = [l3NetworkInventory.uuid]
        action2.clusterUuid = cluster2.uuid
        action2.primaryStorageUuidForRootVolume = ps1.uuid
        action2.instanceOfferingUuid = instanceOffering.uuid
        action2.sessionId = adminSession()
        CreateVmInstanceAction.Result result2 = action2.call()
        assert result2.error != null

        // choose cluster and host but host not in the cluster
        CreateVmInstanceAction action3 = new CreateVmInstanceAction()
        action3.name = "test-1"
        action3.imageUuid = image.uuid
        action3.l3NetworkUuids = [l3NetworkInventory.uuid]
        action3.clusterUuid = cluster2.uuid
        action3.hostUuid = host1.uuid
        action3.instanceOfferingUuid = instanceOffering.uuid
        action3.sessionId = adminSession()
        CreateVmInstanceAction.Result result3 = action3.call()
        assert result3.error != null

        def error3 = result3.error
        assert error3.code == SysErrors.INVALID_ARGUMENT_ERROR.toString()
        assert error3.opaque["host.uuid"]
        assert error3.opaque["host.uuid"] == host1.uuid
        assert error3.opaque["expect.cluster.uuid"]
        assert error3.opaque["expect.cluster.uuid"] == cluster1.uuid
        assert error3.opaque["actual.cluster.uuid"]
        assert error3.opaque["actual.cluster.uuid"] == cluster2.uuid
    }
}
