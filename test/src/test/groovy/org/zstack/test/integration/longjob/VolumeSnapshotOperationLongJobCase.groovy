package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.zstack.core.Platform
import org.zstack.header.longjob.LongJobState
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg
import org.zstack.header.storage.snapshot.APIRevertVolumeFromSnapshotMsg
import org.zstack.sdk.LongJobInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by kayo on 2018/5/9.
 */
class VolumeSnapshotOperationLongJobCase extends SubCase {
    EnvSpec env
    Gson gson

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(4)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(10)
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
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(32)
                        totalCpu = 8
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalMem = SizeUnit.GIGABYTE.toByte(32)
                        totalCpu = 8
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
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useRootDiskOffering("diskOffering")
                useDiskOfferings("diskOffering")
                useL3Networks("pubL3")
                useHost("kvm1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDeleteSnapshotLongJob()
            testRevertSnapshotLongJob()
            testDeleteNotExistsSnapshotLongJob()
        }
    }

    void testRevertSnapshotLongJob() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        gson = new Gson()

        createVolumeSnapshot {
            volumeUuid = vm.getRootVolumeUuid()
            name = "snapshot"
        } as VolumeSnapshotInventory

        def snapshot = createVolumeSnapshot {
            volumeUuid = vm.getRootVolumeUuid()
            name = "snapshot"
        } as VolumeSnapshotInventory

        createVolumeSnapshot {
            volumeUuid = vm.getRootVolumeUuid()
            name = "snapshot"
        } as VolumeSnapshotInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        def msg = new APIRevertVolumeFromSnapshotMsg()
        msg.setUuid(snapshot.getUuid())
        msg.setVolumeUuid(snapshot.getVolumeUuid())

        def id = Platform.getUuid()

        def job = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
            description = "this is a long job to delete snapshot"
            sessionId = adminSession()
            apiId = id
        } as LongJobInventory

        assert job.getJobName() == msg.getClass().getSimpleName()
        assert job.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            def vo = dbFindByUuid(job.getUuid(), LongJobVO.class)
            assert vo.state.toString() == LongJobState.Succeeded.toString()
        }

        env.cleanAfterSimulatorHandlers()
    }

    void testDeleteSnapshotLongJob() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        gson = new Gson()

        def snapshot = createVolumeSnapshot {
            volumeUuid = vm.getRootVolumeUuid()
            name = "snapshot"
        } as VolumeSnapshotInventory

        def msg = new APIDeleteVolumeSnapshotMsg()
        msg.setUuid(snapshot.getUuid())
        msg.setVolumeUuid(snapshot.getVolumeUuid())

        def id = Platform.getUuid()

        def job = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
            description = "this is a long job to delete snapshot"
            sessionId = adminSession()
            apiId = id
        } as LongJobInventory

        assert job.getJobName() == msg.getClass().getSimpleName()
        assert job.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            def vo = dbFindByUuid(job.getUuid(), LongJobVO.class)
            assert vo.state.toString() == LongJobState.Succeeded.toString()
        }

        env.cleanAfterSimulatorHandlers()
    }

    void testDeleteNotExistsSnapshotLongJob() {
        gson = new Gson()

        def msg = new APIDeleteVolumeSnapshotMsg()
        msg.setUuid(Platform.getUuid())
        msg.setVolumeUuid(Platform.getUuid())

        def id = Platform.getUuid()

        def job = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
            description = "this is a long job to delete snapshot"
            sessionId = adminSession()
            apiId = id
        } as LongJobInventory

        assert job.getJobName() == msg.getClass().getSimpleName()
        assert job.state == org.zstack.sdk.LongJobState.Succeeded

        env.cleanAfterSimulatorHandlers()
    }
}
