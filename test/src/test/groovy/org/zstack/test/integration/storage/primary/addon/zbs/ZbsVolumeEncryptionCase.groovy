package org.zstack.test.integration.storage.primary.addon.zbs

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.Platform
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageReply
import org.zstack.header.storage.primary.PrimaryStorageConstant
import org.zstack.kvm.KVMHostAsyncHttpCallMsg
import org.zstack.kvm.KVMHostAsyncHttpCallReply
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.zbs.ZbsConstants
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class ZbsVolumeEncryptionCase extends SubCase {
    private static final String QUERY_SNAPSHOT_PATH = "/zbs/primarystorage/snapshot/query"
    private static final String KVM_LUKS_ENCRYPT_IN_PLACE_PATH = "/zbs/primarystorage/kvmhost/encryptinplace"

    EnvSpec env
    PrimaryStorageInventory ps
    KVMHostInventory host
    CloudBus bus

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }
                }

                externalPrimaryStorage {
                    name = "zbs"
                    identity = "zbs"
                    defaultOutputProtocol = "CBD"
                    config = "{\"mdsUrls\":[\"root:password@127.0.1.1\",\"root:password@127.0.1.2\",\"root:password@127.0.1.3\"],\"logicalPoolName\":\"lpool1\"}"
                    url = "fake url"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            ps = env.inventoryByName("zbs") as PrimaryStorageInventory
            host = env.inventoryByName("kvm") as KVMHostInventory
            bus = bean(CloudBus.class)

            testEncryptVolumeBitsCreatesEncryptedTarget()
            testEncryptVolumeBitsCleansCreatedTargetOnKvmFailure()
        }
    }

    void testEncryptVolumeBitsCreatesEncryptedTarget() {
        String sourceInstallPath = "cbd:pool1/lpool1/volume-source"
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        def tracker = installEncryptInPlaceSimulators(sourceInstallPath, sourceSize, false)

        EncryptVolumeBitsOnPrimaryStorageReply reply = bus.call(buildEncryptBitsMsg(sourceInstallPath)) as EncryptVolumeBitsOnPrimaryStorageReply

        assert reply.success
        assert reply.installPath == tracker.targetInstallPath
        assert tracker.querySnapshotCount == 1
        assert tracker.queryVolumeCount == 1
        assert tracker.createVolumeCount == 1
        assert tracker.kvmEncryptCount == 1
        assert tracker.deletedInstallPaths.isEmpty() : "encrypt success path must not delete any created target, deleted=${tracker.deletedInstallPaths}"
        assert tracker.createCmdSize > sourceSizeInCreateUnit(sourceSize, tracker.createCmdUnit as String)
    }

    void testEncryptVolumeBitsCleansCreatedTargetOnKvmFailure() {
        String sourceInstallPath = "cbd:pool1/lpool1/volume-source-fail"
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        def tracker = installEncryptInPlaceSimulators(sourceInstallPath, sourceSize, true)

        MessageReply reply = bus.call(buildEncryptBitsMsg(sourceInstallPath))

        assert !reply.success
        assert tracker.querySnapshotCount == 1
        assert tracker.queryVolumeCount == 1
        assert tracker.createVolumeCount == 1
        assert tracker.kvmEncryptCount == 1
        retryInSecs(3, 1) {
            assert tracker.deletedInstallPaths.collect { it.toString() } == [tracker.targetInstallPath.toString()] : \
                    "KVM encrypt failure must cleanup created ZBS target, expected=${tracker.targetInstallPath} actual=${tracker.deletedInstallPaths}"
        }
    }

    private EncryptVolumeBitsOnPrimaryStorageMsg buildEncryptBitsMsg(String installPath) {
        EncryptVolumeBitsOnPrimaryStorageMsg msg = new EncryptVolumeBitsOnPrimaryStorageMsg()
        msg.primaryStorageUuid = ps.uuid
        msg.hostUuid = host.uuid
        msg.volumeUuid = Platform.getUuid()
        msg.installPath = installPath
        msg.encryptedDek = "sealed-dek"
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps.uuid)
        return msg
    }

    private Expando installEncryptInPlaceSimulators(String sourceInstallPath, long sourceSize, boolean failKvm) {
        env.cleanSimulatorAndMessageHandlers()

        Expando tracker = new Expando(
                querySnapshotCount: 0,
                queryVolumeCount: 0,
                createVolumeCount: 0,
                kvmEncryptCount: 0,
                createCmdSize: 0L,
                createCmdUnit: null,
                targetInstallPath: null,
                deletedInstallPaths: []
        )

        env.simulator(QUERY_SNAPSHOT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            tracker.querySnapshotCount++
            def cmd = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
            assert cmd.path == sourceInstallPath
            return [success: true, hasSnapshot: false, installPath: sourceInstallPath]
        }

        env.simulator(ZbsStorageController.QUERY_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            tracker.queryVolumeCount++
            ZbsStorageController.QueryVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.QueryVolumeCmd.class)
            assert cmd.path == sourceInstallPath

            ZbsStorageController.QueryVolumeRsp rsp = new ZbsStorageController.QueryVolumeRsp()
            rsp.size = sourceSize
            rsp.actualSize = sourceSize / 2
            return rsp
        }

        env.simulator(ZbsStorageController.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            tracker.createVolumeCount++
            ZbsStorageController.CreateVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVolumeCmd.class)
            assert cmd.logicalPool == "lpool1"
            String expectedVolumePrefix = sourceInstallPath.split("/").last().replaceAll("[^A-Za-z0-9]", "") + "encrypted"
            assert cmd.volume.startsWith(expectedVolumePrefix)

            tracker.createCmdSize = cmd.size
            tracker.createCmdUnit = cmd.unit
            tracker.targetInstallPath = "cbd:pool1/lpool1/${cmd.volume}".toString()

            ZbsStorageController.CreateVolumeRsp rsp = new ZbsStorageController.CreateVolumeRsp()
            rsp.installPath = tracker.targetInstallPath
            rsp.size = tracker.createCmdSize
            rsp.actualSize = 0
            return rsp
        }

        env.simulator(ZbsStorageController.DELETE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
            tracker.deletedInstallPaths << cmd.path
            return new ZbsStorageController.DeleteVolumeRsp()
        }

        env.message(KVMHostAsyncHttpCallMsg.class) { KVMHostAsyncHttpCallMsg msg, CloudBus cloudBus ->
            tracker.kvmEncryptCount++
            assert msg.path == KVM_LUKS_ENCRYPT_IN_PLACE_PATH
            assert msg.hostUuid == host.uuid

            def cmd = JSONObjectUtil.toObject(msg.command, LinkedHashMap.class)
            assert cmd.psUuid == ps.uuid
            assert cmd.installPath == sourceInstallPath
            assert cmd.targetInstallPath == tracker.targetInstallPath
            assert cmd.encryptedDek == "sealed-dek"

            KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply()
            reply.response = [
                    success: !failKvm,
                    error: failKvm ? "on purpose" : null,
                    installPath: tracker.targetInstallPath,
                    actualSize: sourceSize / 2
            ] as LinkedHashMap
            cloudBus.reply(msg, reply)
        }

        return tracker
    }

    private long sourceSizeInCreateUnit(long sourceSize, String unit) {
        if (ZbsConstants.MEGABYTE_UNIT == unit) {
            return (long) Math.ceil(SizeUnit.BYTE.toMegaByte((double) sourceSize))
        }
        return (long) Math.ceil(SizeUnit.BYTE.toGigaByte((double) sourceSize))
    }
}
