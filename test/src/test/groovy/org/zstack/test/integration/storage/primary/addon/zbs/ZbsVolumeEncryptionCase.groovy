package org.zstack.test.integration.storage.primary.addon.zbs

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.devices.DummyEncryptedResourceKeyManager
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.core.Completion
import org.zstack.header.core.FutureReturnValueCompletion
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.image.ImageVO
import org.zstack.header.keyprovider.EncryptedResourceKeyManager
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.addon.primary.CreateVolumeSpec
import org.zstack.header.storage.addon.primary.ZbsVolumeEncryptionBackend
import org.zstack.header.storage.primary.ConvertVolumeEncryptionOnPrimaryStorageMsg
import org.zstack.header.storage.primary.ConvertVolumeEncryptionOnPrimaryStorageReply
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageReply
import org.zstack.header.storage.primary.PrimaryStorageConstant
import org.zstack.header.storage.snapshot.VolumeSnapshotState
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeStatus
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.volume.VolumeInventory
import org.zstack.header.volume.VolumeState
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeStats
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.kvm.KVMHostAsyncHttpCallMsg
import org.zstack.kvm.KVMHostAsyncHttpCallReply
import org.zstack.sdk.ChangeVolumeEncryptionAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.encrypt.VolumeEncryptedSecretHelper
import org.zstack.storage.encrypt.DummyVolumeEncryptedResourceKeyBackend
import org.zstack.storage.encrypt.ZbsVolumeEncryptionExtension
import org.zstack.storage.encrypt.ZbsVolumeEncryptionKvmCaller
import org.zstack.storage.encrypt.ZbsVolumeEncryptionMaterialFactory
import org.zstack.storage.volume.VolumeBase
import org.zstack.storage.zbs.ZbsConstants
import org.zstack.storage.zbs.ZbsStorageController
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit

class ZbsVolumeEncryptionCase extends SubCase {
    private static final String QUERY_SNAPSHOT_PATH = "/zbs/primarystorage/snapshot/query"
    private static final String KVM_LUKS_CREATE_EMPTY_PATH = "/zbs/primarystorage/kvmhost/lukscreateempty"
    private static final String KVM_LUKS_ENCRYPT_IN_PLACE_PATH = "/zbs/primarystorage/kvmhost/encryptinplace"
    private static final String KVM_LUKS_CONVERT_PATH = "/zbs/primarystorage/kvmhost/luksconvert"

    EnvSpec env
    PrimaryStorageInventory ps
    ClusterInventory cluster
    KVMHostInventory host
    CloudBus bus
    boolean primaryStorageAttached

    @Override
    void clean() {
        if (primaryStorageAttached) {
            detachPrimaryStorageFromCluster {
                primaryStorageUuid = ps.uuid
                clusterUuid = cluster.uuid
            }
            primaryStorageAttached = false
        }
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
            cluster = env.inventoryByName("cluster") as ClusterInventory
            attachPrimaryStorageToCluster {
                primaryStorageUuid = ps.uuid
                clusterUuid = cluster.uuid
            }
            primaryStorageAttached = true

            testBuildNativeCbdConversionPaths()
            testPrepareVolumeEncryptionFallsBackToTargetVolumeKey()
            testEncryptedEmptyVolumeCreatorReusesImageKey()
            testPrepareVolumeEncryptionRejectsIncompleteKeyResource()
            testChangeZbsVolumeEncryptionThroughPublicApi()
            testConvertPlainVolumeToEncryptedTarget()
            testConvertEncryptedVolumeToPlainTarget()
            testRejectBidirectionalConversionWhenZbsVersionDoesNotSupportMegabyteAllocation()
            testConvertFailureDeletesOnlyCreatedTarget()
            testConvertMaterialFailureDeletesCreatedTarget()
            testConvertMaterialRuntimeFailureDeletesCreatedTargetOnce()
            testConvertKvmRuntimeFailureDeletesCreatedTargetOnce()
            testConvertTargetConflictDoesNotCallKvmOrDeleteTarget()
            testConvertKeepsZeroKvmActualSize()
            testConvertFallsBackToTargetStats()
            testConvertStatsFailureDeletesCreatedTarget()
            testConvertStatsRuntimeFailureDeletesCreatedTargetOnce()
            testConvertCreatePathMismatchDeletesOnlyRequestedTarget()
            testConvertCreatePathMismatchNeverDeletesSource()
            testConvertBlankCreatePathDeletesRequestedTarget()
            testConvertRejectsInvalidItemsBeforeMutation()
            testEncryptVolumeBitsCreatesEncryptedTarget()
            testEncryptVolumeBitsCleansCreatedTargetOnKvmFailure()
            testRejectConversionWhenZbsVolumeHasSnapshots()
        }
    }

    void testBuildNativeCbdConversionPaths() {
        String volumeUuid = Platform.uuid
        VolumeVO volume = new VolumeVO()
        volume.uuid = volumeUuid
        volume.installPath = "cbd:physicalPool/logicalPool/source"
        VolumeBase base = new VolumeBase(volume)
        def method = VolumeBase.class.getDeclaredMethod("makeConvertedVolumeInstallPath",
                String.class, String.class, Boolean.TYPE, String.class)
        method.accessible = true

        String encryptedTarget = method.invoke(base, volume.installPath, volumeUuid, true, "conversion")
        String plainTarget = method.invoke(base, volume.installPath, volumeUuid, false, "conversion")

        assert encryptedTarget == "cbd:physicalPool/logicalPool/${volumeUuid}.encrypted.conversion"
        assert plainTarget == "cbd:physicalPool/logicalPool/${volumeUuid}.plain.conversion"
        assert encryptedTarget != volume.installPath
        assert plainTarget != volume.installPath
        assert !encryptedTarget.endsWith(".qcow2")
        assert !plainTarget.endsWith(".qcow2")
    }

    void testPrepareVolumeEncryptionFallsBackToTargetVolumeKey() {
        def method = prepareVolumeEncryptionWithSpecMethod()
        String volumeUuid = Platform.uuid
        CreateVolumeSpec spec = new CreateVolumeSpec()
        spec.uuid = volumeUuid
        StubVolumeEncryptedSecretHelper secretHelper = new StubVolumeEncryptedSecretHelper(sealedDek: "volume-sealed-dek")
        StubEncryptedResourceKeyManager keyManager = new StubEncryptedResourceKeyManager()
        StubVolumeEncryptedResourceKeyBackend keyBackend = new StubVolumeEncryptedResourceKeyBackend()

        def material = withMaterialFactoryStubs(secretHelper, keyManager, keyBackend) { factory ->
            invokePrepareVolumeEncryption(method, factory, spec)
        }

        assert secretHelper.requests == [[hostUuid: host.uuid, volumeUuid: volumeUuid]]
        assert secretHelper.sealRequests.isEmpty()
        assert keyManager.requests.isEmpty()
        assert keyBackend.volumeRequests.isEmpty()
        assert keyBackend.imageRequests.isEmpty()
        assert readMaterialField(material, "encryptedDek") == "volume-sealed-dek"
    }

    void testEncryptedEmptyVolumeCreatorReusesImageKey() {
        String imageUuid = Platform.uuid
        String keyProviderUuid = Platform.uuid
        String installPath = "cbd:lpool1/target-volume"
        CreateVolumeSpec spec = new CreateVolumeSpec()
        spec.uuid = Platform.uuid
        spec.name = "target-volume"
        spec.size = SizeUnit.GIGABYTE.toByte(1)
        spec.encryptionKeyResourceType = ImageVO.simpleName
        spec.encryptionKeyResourceUuid = imageUuid

        EncryptedResourceKeyManager.ResourceKeyResult keyResult = new EncryptedResourceKeyManager.ResourceKeyResult()
        keyResult.dekBase64 = "image-dek"
        keyResult.keyProviderUuid = keyProviderUuid
        StubVolumeEncryptedSecretHelper secretHelper = new StubVolumeEncryptedSecretHelper(sealedDek: "image-sealed-dek")
        StubEncryptedResourceKeyManager keyManager = new StubEncryptedResourceKeyManager(result: keyResult)
        StubVolumeEncryptedResourceKeyBackend keyBackend =
                new StubVolumeEncryptedResourceKeyBackend(keyProviderUuid: keyProviderUuid)
        List<String> deletedPaths = []
        ZbsVolumeEncryptionBackend backend = [
                getPrimaryStorageUuid  : { ps.uuid },
                buildConfiguredVolumePath: { String volumeName -> installPath },
                createLuksBackingVolume: { String path, long size, ReturnValueCompletion<String> completion ->
                    completion.success(path)
                },
                deleteLuksBackingVolume: { String path -> deletedPaths << path },
        ] as ZbsVolumeEncryptionBackend
        env.message(KVMHostAsyncHttpCallMsg.class) { KVMHostAsyncHttpCallMsg msg, CloudBus cloudBus ->
            assert msg.path == KVM_LUKS_CREATE_EMPTY_PATH
            LinkedHashMap cmd = JSONObjectUtil.toObject(msg.command, LinkedHashMap.class)
            assert cmd.encryptedDek == "image-sealed-dek"
            KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply()
            reply.response = [success: true] as LinkedHashMap
            cloudBus.reply(msg, reply)
        }

        FutureReturnValueCompletion completion = new FutureReturnValueCompletion(null)
        withMaterialFactoryStubs(secretHelper, keyManager, keyBackend) {
            bean(ZbsVolumeEncryptionExtension.class).createEncryptedEmptyVolume(backend, spec, completion)
        }
        completion.await(TimeUnit.SECONDS.toMillis(10))

        assert completion.success : completion.errorCode
        assert (completion.result as VolumeStats).installPath == installPath
        assert deletedPaths.isEmpty()
        assert keyBackend.imageRequests == [imageUuid]
        assert keyManager.requests*.resourceUuid == [imageUuid]
    }

    void testPrepareVolumeEncryptionRejectsIncompleteKeyResource() {
        def method = prepareVolumeEncryptionWithSpecMethod()
        CreateVolumeSpec spec = new CreateVolumeSpec()
        spec.uuid = Platform.uuid
        spec.encryptionKeyResourceType = ImageVO.simpleName
        StubVolumeEncryptedSecretHelper secretHelper = new StubVolumeEncryptedSecretHelper()
        StubEncryptedResourceKeyManager keyManager = new StubEncryptedResourceKeyManager()
        StubVolumeEncryptedResourceKeyBackend keyBackend = new StubVolumeEncryptedResourceKeyBackend()
        OperationFailureException failure = null

        withMaterialFactoryStubs(secretHelper, keyManager, keyBackend) { factory ->
            try {
                invokePrepareVolumeEncryption(method, factory, spec)
            } catch (OperationFailureException e) {
                failure = e
            }
        }

        assert failure != null
        assert failure.message.contains("requires complete key resource")
        assert secretHelper.requests.isEmpty()
        assert secretHelper.sealRequests.isEmpty()
        assert keyManager.requests.isEmpty()
        assert keyBackend.volumeRequests.isEmpty()
        assert keyBackend.imageRequests.isEmpty()
    }

    void testChangeZbsVolumeEncryptionThroughPublicApi() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        String volumeUuid = Platform.uuid
        String sourceInstallPath = "cbd:pool1/lpool1/${volumeUuid}"
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        long actualSize = SizeUnit.MEGABYTE.toByte(768)
        VolumeVO volume = new VolumeVO()
        volume.uuid = volumeUuid
        volume.name = "zbs-public-api-conversion"
        volume.primaryStorageUuid = ps.uuid
        volume.installPath = sourceInstallPath
        volume.type = VolumeType.Data
        volume.status = VolumeStatus.Ready
        volume.state = VolumeState.Enabled
        volume.format = "raw"
        volume.size = sourceSize
        volume.actualSize = SizeUnit.GIGABYTE.toByte(1)
        volume.encrypted = true
        dbf.persistAndRefresh(volume)

        Expando tracker = installPublicApiConversionSimulators(sourceInstallPath, sourceSize, actualSize)
        try {
            withSealedDekStub { List<Map<String, String>> requests ->
                ChangeVolumeEncryptionAction action = new ChangeVolumeEncryptionAction()
                action.uuid = volumeUuid
                action.encrypted = false
                action.sessionId = adminSession()
                ChangeVolumeEncryptionAction.Result result = action.call()

                assert result.error == null
                assert result.value.inventory.uuid == volumeUuid
                assert !result.value.inventory.encrypted
                assert result.value.inventory.installPath == tracker.targetInstallPath
                assert result.value.inventory.actualSize == actualSize
                assert tracker.createVolumeCount == 1
                assert tracker.kvmConvertCount == 1
                assert tracker.createCmd.logicalPool == "lpool1"
                assert tracker.createCmd.size == sourceSizeInCreateUnit(sourceSize, tracker.createCmd.unit as String)
                assert tracker.kvmCmd.installPath == sourceInstallPath
                assert tracker.kvmCmd.targetInstallPath == tracker.targetInstallPath
                assert !tracker.kvmCmd.targetEncrypted
                assert tracker.kvmCmd.virtualSize == sourceSize
                assert tracker.deletedInstallPaths.isEmpty()
                assert tracker.targetInstallPath.startsWith("cbd:pool1/lpool1/")
                assert !tracker.targetInstallPath.endsWith(".qcow2")
                assert requests == [[hostUuid: host.uuid, volumeUuid: volumeUuid]]

                VolumeVO current = dbf.findByUuid(volumeUuid, VolumeVO.class)
                assert !current.encrypted
                assert current.installPath == tracker.targetInstallPath
                assert current.actualSize == actualSize
            }
        } finally {
            dbf.removeByPrimaryKey(volumeUuid, VolumeVO.class)
        }
    }

    void testRejectConversionWhenZbsVolumeHasSnapshots() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        String volumeUuid = Platform.uuid
        String treeUuid = Platform.uuid
        String snapshotUuid = Platform.uuid
        String sourceInstallPath = "cbd:pool1/lpool1/${volumeUuid}"
        VolumeVO volume = new VolumeVO()
        volume.uuid = volumeUuid
        volume.name = "zbs-snapshot-conversion-guard"
        volume.primaryStorageUuid = ps.uuid
        volume.installPath = sourceInstallPath
        volume.type = VolumeType.Data
        volume.status = VolumeStatus.Ready
        volume.state = VolumeState.Enabled
        volume.format = "raw"
        volume.size = SizeUnit.GIGABYTE.toByte(1)
        volume.actualSize = SizeUnit.MEGABYTE.toByte(1)
        volume.encrypted = false
        dbf.persistAndRefresh(volume)

        VolumeSnapshotTreeVO tree = new VolumeSnapshotTreeVO()
        tree.uuid = treeUuid
        tree.volumeUuid = volumeUuid
        tree.current = true
        tree.status = VolumeSnapshotTreeStatus.Completed
        dbf.persistAndRefresh(tree)

        VolumeSnapshotVO snapshot = new VolumeSnapshotVO()
        snapshot.uuid = snapshotUuid
        snapshot.name = "zbs-snapshot-conversion-guard"
        snapshot.volumeUuid = volumeUuid
        snapshot.treeUuid = treeUuid
        snapshot.primaryStorageUuid = ps.uuid
        snapshot.primaryStorageInstallPath = "${sourceInstallPath}@${snapshotUuid}"
        snapshot.state = VolumeSnapshotState.Enabled
        snapshot.status = VolumeSnapshotStatus.Ready
        snapshot.format = "raw"
        snapshot.volumeType = VolumeType.Data.toString()
        snapshot.size = volume.size
        dbf.persistAndRefresh(snapshot)

        Expando tracker = installSnapshotConversionGuardCounters()
        try {
            ChangeVolumeEncryptionAction action = new ChangeVolumeEncryptionAction()
            action.uuid = volumeUuid
            action.encrypted = true
            action.sessionId = adminSession()
            ChangeVolumeEncryptionAction.Result result = action.call()

            assert result.error != null
            assert result.error.details.contains("snapshots")
            assert result.error.details.contains("ZBS/CBD")
            assert tracker.conversionMessageCount == 0
            assert tracker.createVolumeCount == 0
            assert tracker.kvmConvertCount == 0

            VolumeVO current = dbf.findByUuid(volumeUuid, VolumeVO.class)
            assert !current.encrypted
            assert current.installPath == sourceInstallPath
        } finally {
            dbf.removeByPrimaryKey(snapshotUuid, VolumeSnapshotVO.class)
            dbf.removeByPrimaryKey(treeUuid, VolumeSnapshotTreeVO.class)
            dbf.removeByPrimaryKey(volumeUuid, VolumeVO.class)
        }
    }

    void testConvertPlainVolumeToEncryptedTarget() {
        String sourceInstallPath = "cbd:pool1/lpool1/volume-source-convert"
        String targetInstallPath = "cbd:pool1/lpool1/volume-target-convert"
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        long actualSize = SizeUnit.GIGABYTE.toByte(1)
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                sourceInstallPath, targetInstallPath, sourceSize, false, true)
        Expando tracker = installConversionSimulators(msg, false, false, actualSize, sourceSize.intdiv(3))

        withSealedDekStub { List<Map<String, String>> requests ->
            ConvertVolumeEncryptionOnPrimaryStorageReply reply =
                    bus.call(msg) as ConvertVolumeEncryptionOnPrimaryStorageReply

            assert reply.success
            assert reply.actualSizes == [(msg.volume.uuid): actualSize]
            assert tracker.createVolumeCount == 1
            assert tracker.kvmConvertCount == 1
            assert tracker.statsCount == 0
            assert tracker.deletedInstallPaths.isEmpty()
            assert tracker.createCmd.logicalPool == "lpool1"
            assert tracker.createCmd.volume == "volume-target-convert"
            assert !tracker.createCmd.skipIfExisting
            assert tracker.createCmd.size == sourceSizeInCreateUnit(
                    sourceSize + SizeUnit.MEGABYTE.toByte(8), tracker.createCmd.unit as String)
            assert tracker.kvmCmd.targetEncrypted
            assert tracker.kvmCmd.encryptedDek == "sealed-dek"
            assert requests == [[hostUuid: host.uuid, volumeUuid: msg.volume.uuid]]
        }
    }

    void testConvertEncryptedVolumeToPlainTarget() {
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/encrypted-source", "cbd:pool1/lpool1/plain-target",
                sourceSize, true, false)
        Expando tracker = installConversionSimulators(msg, false, false,
                sourceSize.intdiv(2), sourceSize.intdiv(3))

        withSealedDekStub { List<Map<String, String>> requests ->
            ConvertVolumeEncryptionOnPrimaryStorageReply reply =
                    bus.call(msg) as ConvertVolumeEncryptionOnPrimaryStorageReply

            assert reply.success
            assert tracker.createCmd.size == sourceSizeInCreateUnit(sourceSize, tracker.createCmd.unit as String)
            assert tracker.kvmCmd.containsKey("targetEncrypted")
            assert !tracker.kvmCmd.targetEncrypted
            assert tracker.kvmCmd.encryptedDek == "sealed-dek"
            assert requests == [[hostUuid: host.uuid, volumeUuid: msg.volume.uuid]]
            assert tracker.deletedInstallPaths.isEmpty()
        }
    }

    void testRejectBidirectionalConversionWhenZbsVersionDoesNotSupportMegabyteAllocation() {
        env.cleanAfterSimulatorHandlers()
        env.afterSimulator(ZbsStorageController.GET_FACTS_PATH) { ZbsStorageController.GetFactsRsp rsp,
                                                                  HttpEntity<String> e ->
            rsp.version = "1.6.0"
            return rsp
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        env.cleanAfterSimulatorHandlers()

        try {
            [[false, true], [true, false]].eachWithIndex { List<Boolean> encryption, int index ->
                ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                        "cbd:pool1/lpool1/unsupported-version-source-${index}",
                        "cbd:pool1/lpool1/unsupported-version-target-${index}",
                        SizeUnit.GIGABYTE.toByte(2), encryption[0], encryption[1])
                Expando tracker = installConversionSimulators(msg, false, false,
                        SizeUnit.GIGABYTE.toByte(1), SizeUnit.GIGABYTE.toByte(1))

                withSealedDekStub {
                    MessageReply reply = bus.call(msg)

                    assert !reply.success
                    assert reply.error.details.contains("1.6.0")
                    assert reply.error.details.contains(ZbsConstants.MEGABYTE_SUPPORTED_VERSION)
                    assert tracker.createVolumeCount == 0
                    assert tracker.kvmConvertCount == 0
                    assert tracker.deletedInstallPaths.isEmpty()
                }
            }
        } finally {
            env.cleanSimulatorAndMessageHandlers()
            reconnectPrimaryStorage {
                uuid = ps.uuid
            }
        }
    }

    void testConvertFailureDeletesOnlyCreatedTarget() {
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/failure-source", "cbd:pool1/lpool1/failure-target",
                sourceSize, false, true)
        Expando tracker = installConversionSimulators(msg, true, false, null, sourceSize.intdiv(3))

        withSealedDekStub {
            MessageReply reply = bus.call(msg)

            assert !reply.success
            assert reply.error.details.contains("on purpose")
            retryInSecs(3, 1) {
                assert tracker.deletedInstallPaths == [msg.items[0].targetInstallPath]
                assert !tracker.deletedInstallPaths.contains(msg.items[0].sourceInstallPath)
            }
        }
    }

    void testConvertMaterialFailureDeletesCreatedTarget() {
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/material-failure-source", "cbd:pool1/lpool1/material-failure-target",
                sourceSize, false, true)
        Expando tracker = installConversionSimulators(msg, false, false, null, sourceSize.intdiv(3))

        withSealedDekStub("") { List<Map<String, String>> requests ->
            MessageReply reply = bus.call(msg)

            assert !reply.success
            assert reply.error.details.contains("cannot prepare LUKS encryptedDek")
            assert tracker.createVolumeCount == 1
            assert tracker.kvmConvertCount == 0
            assert requests == [[hostUuid: host.uuid, volumeUuid: msg.volume.uuid]]
            retryInSecs(3, 1) {
                assert tracker.deletedInstallPaths == [msg.items[0].targetInstallPath]
                assert !tracker.deletedInstallPaths.contains(msg.items[0].sourceInstallPath)
            }
        }
    }

    void testConvertMaterialRuntimeFailureDeletesCreatedTargetOnce() {
        Expando tracker = runPostCreateRuntimeFailure("material")

        assert tracker.runtimeException == null
        assert tracker.successCount == 0
        assert tracker.failureCount == 1
        assert tracker.error.details.contains("failed to prepare ZBS volume encryption material")
        assert tracker.error.getFromOpaque("exception") == "material runtime failure"
        assert tracker.deletedInstallPaths == [tracker.targetInstallPath]
    }

    void testConvertKvmRuntimeFailureDeletesCreatedTargetOnce() {
        Expando tracker = runPostCreateRuntimeFailure("kvm")

        assert tracker.runtimeException == null
        assert tracker.successCount == 0
        assert tracker.failureCount == 1
        assert tracker.error.details.contains("kvm runtime failure")
        assert tracker.deletedInstallPaths == [tracker.targetInstallPath]
    }

    void testConvertKeepsZeroKvmActualSize() {
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/zero-source", "cbd:pool1/lpool1/zero-target",
                sourceSize, false, true)
        Expando tracker = installConversionSimulators(msg, false, false, 0L, sourceSize.intdiv(3))

        withSealedDekStub {
            ConvertVolumeEncryptionOnPrimaryStorageReply reply =
                    bus.call(msg) as ConvertVolumeEncryptionOnPrimaryStorageReply

            assert reply.success
            assert reply.actualSizes == [(msg.volume.uuid): 0L]
            assert tracker.statsCount == 0
            assert tracker.deletedInstallPaths.isEmpty()
        }
    }

    void testConvertTargetConflictDoesNotCallKvmOrDeleteTarget() {
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/conflict-source", "cbd:pool1/lpool1/conflict-target",
                sourceSize, false, true)
        Expando tracker = installConversionSimulators(msg, false, true, null, sourceSize.intdiv(3))

        withSealedDekStub { List<Map<String, String>> requests ->
            MessageReply reply = bus.call(msg)

            assert !reply.success
            assert tracker.createVolumeCount == 1
            assert tracker.kvmConvertCount == 0
            assert tracker.deletedInstallPaths.isEmpty()
            assert requests.isEmpty()
        }
    }

    void testConvertFallsBackToTargetStats() {
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        long targetActualSize = SizeUnit.MEGABYTE.toByte(768)
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/stats-source", "cbd:pool1/lpool1/stats-target",
                sourceSize, false, true)
        Expando tracker = installConversionSimulators(msg, false, false, null, targetActualSize)

        withSealedDekStub {
            ConvertVolumeEncryptionOnPrimaryStorageReply reply =
                    bus.call(msg) as ConvertVolumeEncryptionOnPrimaryStorageReply

            assert reply.success
            assert reply.actualSizes == [(msg.volume.uuid): targetActualSize]
            assert tracker.statsCount == 1
            assert tracker.deletedInstallPaths.isEmpty()
        }
    }

    void testConvertStatsFailureDeletesCreatedTarget() {
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/stats-failure-source", "cbd:pool1/lpool1/stats-failure-target",
                sourceSize, false, true)
        Expando tracker = installConversionSimulators(msg, false, false, null, 0, true)

        withSealedDekStub {
            MessageReply reply = bus.call(msg)

            assert !reply.success
            retryInSecs(3, 1) {
                assert tracker.deletedInstallPaths == [msg.items[0].targetInstallPath]
            }
        }
    }

    void testConvertStatsRuntimeFailureDeletesCreatedTargetOnce() {
        Expando tracker = runPostCreateRuntimeFailure("stats")

        retryInSecs(3, 1) {
            assert tracker.failureCount == 1
            assert tracker.deletedInstallPaths == [tracker.targetInstallPath]
        }
        assert tracker.runtimeException == null
        assert tracker.successCount == 0
        assert tracker.error.details.contains("failed to query converted ZBS volume")
        assert tracker.error.getFromOpaque("exception") == "stats runtime failure"
    }

    void testConvertCreatePathMismatchDeletesOnlyRequestedTarget() {
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        String returnedInstallPath = "cbd:pool1/lpool1/unexpected-created-target"
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/path-mismatch-source", "cbd:pool1/lpool1/path-mismatch-target",
                sourceSize, false, true)
        Expando tracker = installConversionSimulators(msg, false, false, null, 0,
                false, returnedInstallPath)

        withSealedDekStub { List<Map<String, String>> requests ->
            MessageReply reply = bus.call(msg)

            assert !reply.success
            retryInSecs(3, 1) {
                assert tracker.deletedInstallPaths == [msg.items[0].targetInstallPath]
                assert !tracker.deletedInstallPaths.contains(msg.items[0].sourceInstallPath)
                assert !tracker.deletedInstallPaths.contains(returnedInstallPath)
            }
            assert tracker.kvmConvertCount == 0
            assert requests.isEmpty()
        }
    }

    void testConvertCreatePathMismatchNeverDeletesSource() {
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/source-returned-by-create", "cbd:pool1/lpool1/source-mismatch-target",
                sourceSize, false, true)
        Expando tracker = installConversionSimulators(msg, false, false, null, 0,
                false, msg.items[0].sourceInstallPath)

        withSealedDekStub { List<Map<String, String>> requests ->
            MessageReply reply = bus.call(msg)

            assert !reply.success
            retryInSecs(3, 1) {
                assert tracker.deletedInstallPaths == [msg.items[0].targetInstallPath]
                assert !tracker.deletedInstallPaths.contains(msg.items[0].sourceInstallPath)
            }
            assert tracker.kvmConvertCount == 0
            assert requests.isEmpty()
        }
    }

    void testConvertBlankCreatePathDeletesRequestedTarget() {
        long sourceSize = SizeUnit.GIGABYTE.toByte(2)
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/blank-path-source", "cbd:pool1/lpool1/blank-path-target",
                sourceSize, false, true)
        Expando tracker = installConversionSimulators(msg, false, false, null, 0,
                false, "")

        withSealedDekStub { List<Map<String, String>> requests ->
            MessageReply reply = bus.call(msg)

            assert !reply.success
            assert reply.error.details.contains("unexpected path")
            retryInSecs(3, 1) {
                assert tracker.deletedInstallPaths == [msg.items[0].targetInstallPath]
                assert !tracker.deletedInstallPaths.contains("")
                assert !tracker.deletedInstallPaths.contains(msg.items[0].sourceInstallPath)
            }
            assert tracker.kvmConvertCount == 0
            assert requests.isEmpty()
        }
    }

    void testConvertRejectsInvalidItemsBeforeMutation() {
        Expando tracker = installConversionMutationCounters()
        List<Closure> invalidMutations = [
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items = null },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items = [] },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items = [msg.items[0], copyConversionItem(msg.items[0])] },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].resourceType = VolumeSnapshotVO.simpleName },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].resourceUuid = Platform.getUuid() },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].sourceInstallPath = "" },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].targetInstallPath = "" },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].targetBackingInstallPath = "cbd:pool1/lpool1/backing" },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].sourceInstallPath = "pool1/lpool1/invalid-source" },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].targetInstallPath = "cbd:pool1//invalid-target" },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].sourceInstallPath += "@snapshot" },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].targetInstallPath += "@snapshot" },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].targetInstallPath = "cbd:pool2/lpool1/invalid-target" },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].targetInstallPath = "cbd:pool1/lpool2/invalid-target" },
                { ConvertVolumeEncryptionOnPrimaryStorageMsg msg -> msg.items[0].targetInstallPath = msg.items[0].sourceInstallPath }
        ]

        invalidMutations.each { Closure mutation ->
            ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                    "cbd:pool1/lpool1/valid-source", "cbd:pool1/lpool1/valid-target",
                    SizeUnit.GIGABYTE.toByte(2), false, true)
            mutation.call(msg)

            MessageReply reply = bus.call(msg)

            assert !reply.success
            assert tracker.createVolumeCount == 0
            assert tracker.kvmConvertCount == 0
            assert tracker.deletedInstallPaths.isEmpty()
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

    private Expando installSnapshotConversionGuardCounters() {
        env.cleanSimulatorAndMessageHandlers()
        Expando tracker = new Expando(
                conversionMessageCount: 0,
                createVolumeCount: 0,
                kvmConvertCount: 0
        )

        env.message(ConvertVolumeEncryptionOnPrimaryStorageMsg.class) {
            ConvertVolumeEncryptionOnPrimaryStorageMsg msg, CloudBus cloudBus ->
                tracker.conversionMessageCount++
                ConvertVolumeEncryptionOnPrimaryStorageReply reply = new ConvertVolumeEncryptionOnPrimaryStorageReply()
                reply.setError(operr("conversion message must not be sent when the ZBS volume has snapshots"))
                cloudBus.reply(msg, reply)
        }
        env.simulator(ZbsStorageController.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            tracker.createVolumeCount++
            return new ZbsStorageController.CreateVolumeRsp()
        }
        env.message(KVMHostAsyncHttpCallMsg.class) { KVMHostAsyncHttpCallMsg msg, CloudBus cloudBus ->
            tracker.kvmConvertCount++
            KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply()
            reply.response = [success: true, actualSize: 1L] as LinkedHashMap
            cloudBus.reply(msg, reply)
        }
        return tracker
    }

    private Expando installPublicApiConversionSimulators(String sourceInstallPath,
                                                          long sourceSize,
                                                          long actualSize) {
        env.cleanSimulatorAndMessageHandlers()
        Expando tracker = new Expando(
                createVolumeCount: 0,
                kvmConvertCount: 0,
                createCmd: null,
                kvmCmd: null,
                targetInstallPath: null,
                deletedInstallPaths: []
        )

        env.simulator(ZbsStorageController.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            tracker.createVolumeCount++
            tracker.createCmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVolumeCmd.class)
            tracker.targetInstallPath = "cbd:pool1/${tracker.createCmd.logicalPool}/${tracker.createCmd.volume}"
            ZbsStorageController.CreateVolumeRsp rsp = new ZbsStorageController.CreateVolumeRsp()
            rsp.installPath = tracker.targetInstallPath
            return rsp
        }
        env.simulator(ZbsStorageController.DELETE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
            tracker.deletedInstallPaths << cmd.path
            return new ZbsStorageController.DeleteVolumeRsp()
        }
        env.message(KVMHostAsyncHttpCallMsg.class) { KVMHostAsyncHttpCallMsg hmsg, CloudBus cloudBus ->
            tracker.kvmConvertCount++
            assert hmsg.path == KVM_LUKS_CONVERT_PATH
            assert hmsg.hostUuid == host.uuid
            tracker.kvmCmd = JSONObjectUtil.toObject(hmsg.command, LinkedHashMap.class)
            assert tracker.kvmCmd.psUuid == ps.uuid
            assert tracker.kvmCmd.installPath == sourceInstallPath
            assert tracker.kvmCmd.targetInstallPath == tracker.targetInstallPath
            assert tracker.kvmCmd.virtualSize == sourceSize

            KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply()
            reply.response = [success: true, actualSize: actualSize] as LinkedHashMap
            cloudBus.reply(hmsg, reply)
        }
        return tracker
    }

    private ConvertVolumeEncryptionOnPrimaryStorageMsg buildConversionMsg(String sourceInstallPath,
                                                                           String targetInstallPath,
                                                                           long sourceSize,
                                                                           boolean sourceEncrypted,
                                                                           boolean targetEncrypted) {
        VolumeInventory volume = new VolumeInventory()
        volume.uuid = Platform.getUuid()
        volume.primaryStorageUuid = ps.uuid
        volume.size = sourceSize
        volume.encrypted = sourceEncrypted

        ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem item =
                new ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem()
        item.resourceUuid = volume.uuid
        item.resourceType = VolumeVO.simpleName
        item.sourceInstallPath = sourceInstallPath
        item.targetInstallPath = targetInstallPath

        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = new ConvertVolumeEncryptionOnPrimaryStorageMsg()
        msg.volume = volume
        msg.targetEncrypted = targetEncrypted
        msg.items = [item]
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ps.uuid)
        return msg
    }

    private ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem copyConversionItem(
            ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem source) {
        ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem item =
                new ConvertVolumeEncryptionOnPrimaryStorageMsg.VolumeEncryptionConversionItem()
        item.resourceUuid = source.resourceUuid
        item.resourceType = source.resourceType
        item.sourceInstallPath = source.sourceInstallPath
        item.targetInstallPath = source.targetInstallPath
        item.targetBackingInstallPath = source.targetBackingInstallPath
        return item
    }

    private Expando installConversionSimulators(ConvertVolumeEncryptionOnPrimaryStorageMsg msg,
                                                boolean failKvm,
                                                boolean targetConflict,
                                                Long kvmActualSize,
                                                long statsActualSize,
                                                boolean failStats = false,
                                                String createdInstallPath = null) {
        env.cleanSimulatorAndMessageHandlers()
        def item = msg.items[0]
        Expando tracker = new Expando(
                createVolumeCount: 0,
                kvmConvertCount: 0,
                statsCount: 0,
                createCmd: null,
                kvmCmd: null,
                deletedInstallPaths: []
        )

        env.simulator(ZbsStorageController.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            tracker.createVolumeCount++
            tracker.createCmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.CreateVolumeCmd.class)
            if (targetConflict) {
                return [success: false, error: "target already exists"]
            }

            ZbsStorageController.CreateVolumeRsp rsp = new ZbsStorageController.CreateVolumeRsp()
            rsp.installPath = createdInstallPath == null ? item.targetInstallPath : createdInstallPath
            return rsp
        }

        env.simulator(ZbsStorageController.DELETE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
            tracker.deletedInstallPaths << cmd.path
            return new ZbsStorageController.DeleteVolumeRsp()
        }

        env.simulator(ZbsStorageController.QUERY_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            tracker.statsCount++
            ZbsStorageController.QueryVolumeCmd cmd = JSONObjectUtil.toObject(e.body, ZbsStorageController.QueryVolumeCmd.class)
            assert cmd.path == item.targetInstallPath
            if (failStats) {
                return [success: false, error: "stats failed on purpose"]
            }

            ZbsStorageController.QueryVolumeRsp rsp = new ZbsStorageController.QueryVolumeRsp()
            rsp.size = msg.volume.size
            rsp.actualSize = statsActualSize
            return rsp
        }

        env.message(KVMHostAsyncHttpCallMsg.class) { KVMHostAsyncHttpCallMsg hmsg, CloudBus cloudBus ->
            tracker.kvmConvertCount++
            assert hmsg.path == KVM_LUKS_CONVERT_PATH
            assert hmsg.hostUuid == host.uuid
            tracker.kvmCmd = JSONObjectUtil.toObject(hmsg.command, LinkedHashMap.class)
            assert tracker.kvmCmd.psUuid == ps.uuid
            assert tracker.kvmCmd.installPath == item.sourceInstallPath
            assert tracker.kvmCmd.targetInstallPath == item.targetInstallPath
            assert tracker.kvmCmd.virtualSize == msg.volume.size

            KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply()
            reply.response = [
                    success: !failKvm,
                    error: failKvm ? "on purpose" : null,
                    actualSize: kvmActualSize
            ] as LinkedHashMap
            cloudBus.reply(hmsg, reply)
        }
        return tracker
    }

    private Expando runPostCreateRuntimeFailure(String failurePoint) {
        env.cleanSimulatorAndMessageHandlers()
        String failureMessage = "${failurePoint} runtime failure"
        ConvertVolumeEncryptionOnPrimaryStorageMsg msg = buildConversionMsg(
                "cbd:pool1/lpool1/${failurePoint}-runtime-source",
                "cbd:pool1/lpool1/${failurePoint}-runtime-target",
                SizeUnit.GIGABYTE.toByte(2), false, true)
        Expando tracker = new Expando(
                targetInstallPath: msg.items[0].targetInstallPath,
                deletedInstallPaths: [],
                successCount: 0,
                failureCount: 0,
                error: null,
                runtimeException: null
        )
        ZbsVolumeEncryptionBackend backend = [
                getPrimaryStorageUuid  : { ps.uuid },
                validateConversionPaths: { String source, String target -> },
                createConversionTarget : { String target, long size, boolean encrypted,
                                           ReturnValueCompletion<String> completion ->
                    completion.success(target)
                },
                deleteConversionTarget : { String target, Completion completion ->
                    tracker.deletedInstallPaths << target
                    completion.success()
                },
                stats                  : { String target, ReturnValueCompletion<VolumeStats> completion ->
                    if (failurePoint == "stats") {
                        throw new RuntimeException(failureMessage)
                    }
                    VolumeStats stats = new VolumeStats()
                    stats.actualSize = 1L
                    completion.success(stats)
                }
        ] as ZbsVolumeEncryptionBackend

        if (failurePoint == "stats") {
            env.message(KVMHostAsyncHttpCallMsg.class) { KVMHostAsyncHttpCallMsg hmsg, CloudBus cloudBus ->
                KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply()
                reply.response = [success: true, actualSize: null] as LinkedHashMap
                cloudBus.reply(hmsg, reply)
            }
        }

        StubVolumeEncryptedSecretHelper secretHelper = new StubVolumeEncryptedSecretHelper(
                sealedDek: "sealed-dek",
                runtimeFailure: failurePoint == "material" ? new RuntimeException(failureMessage) : null)
        Closure convert = {
            try {
                bean(ZbsVolumeEncryptionExtension.class).convertVolumeEncryption(backend, msg,
                        new ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply>(null) {
                            @Override
                            void success(ConvertVolumeEncryptionOnPrimaryStorageReply reply) {
                                tracker.successCount++
                            }

                            @Override
                            void fail(ErrorCode errorCode) {
                                tracker.failureCount++
                                tracker.error = errorCode
                            }
                        })
            } catch (RuntimeException e) {
                tracker.runtimeException = e
            }
        }

        withMaterialFactoryStubs(secretHelper, new StubEncryptedResourceKeyManager(),
                new StubVolumeEncryptedResourceKeyBackend()) {
            if (failurePoint == "kvm") {
                CloudBus throwingBus = [
                        makeTargetServiceIdByResourceUuid: { Object... ignored ->
                            throw new RuntimeException(failureMessage)
                        }
                ] as CloudBus
                withKvmCallerBus(throwingBus, convert)
            } else {
                convert.call()
            }
        }
        return tracker
    }

    private Expando installConversionMutationCounters() {
        env.cleanSimulatorAndMessageHandlers()
        Expando tracker = new Expando(createVolumeCount: 0, kvmConvertCount: 0, deletedInstallPaths: [])

        env.simulator(ZbsStorageController.CREATE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            tracker.createVolumeCount++
            return new ZbsStorageController.CreateVolumeRsp()
        }
        env.simulator(ZbsStorageController.DELETE_VOLUME_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, LinkedHashMap.class)
            tracker.deletedInstallPaths << cmd.path
            return new ZbsStorageController.DeleteVolumeRsp()
        }
        env.message(KVMHostAsyncHttpCallMsg.class) { KVMHostAsyncHttpCallMsg hmsg, CloudBus cloudBus ->
            tracker.kvmConvertCount++
            KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply()
            reply.response = [success: true, actualSize: 1L] as LinkedHashMap
            cloudBus.reply(hmsg, reply)
        }
        return tracker
    }

    private <T> T withSealedDekStub(String sealedDek = "sealed-dek", Closure<T> body) {
        ZbsVolumeEncryptionMaterialFactory materialFactory = bean(ZbsVolumeEncryptionMaterialFactory.class)
        def field = ZbsVolumeEncryptionMaterialFactory.getDeclaredField("volumeEncryptedSecretHelper")
        field.accessible = true
        VolumeEncryptedSecretHelper original = field.get(materialFactory) as VolumeEncryptedSecretHelper
        StubVolumeEncryptedSecretHelper stub = new StubVolumeEncryptedSecretHelper()
        stub.sealedDek = sealedDek
        field.set(materialFactory, stub)
        try {
            return body.call(stub.requests)
        } finally {
            field.set(materialFactory, original)
        }
    }

    private def prepareVolumeEncryptionWithSpecMethod() {
        def method = ZbsVolumeEncryptionMaterialFactory.declaredMethods.find {
            it.name == "prepareVolumeEncryption" &&
                    it.parameterTypes.toList() == [String.class, CreateVolumeSpec.class, String.class]
        }
        assert method != null: "ZbsVolumeEncryptionMaterialFactory must accept CreateVolumeSpec"
        method.accessible = true
        return method
    }

    private def invokePrepareVolumeEncryption(def method, ZbsVolumeEncryptionMaterialFactory factory,
                                              CreateVolumeSpec spec) {
        try {
            return method.invoke(factory, ps.uuid, spec, "test prepare ZBS encryption material")
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.targetException
        }
    }

    private <T> T withMaterialFactoryStubs(StubVolumeEncryptedSecretHelper secretHelper,
                                           StubEncryptedResourceKeyManager keyManager,
                                           StubVolumeEncryptedResourceKeyBackend keyBackend,
                                           Closure<T> body) {
        ZbsVolumeEncryptionMaterialFactory factory = bean(ZbsVolumeEncryptionMaterialFactory.class)
        Map<String, Object> replacements = [
                volumeEncryptedSecretHelper : secretHelper,
                encryptedResourceKeyManager : keyManager,
                volumeEncryptedResourceKeyBackend: keyBackend,
        ]
        Map<String, Object> originals = [:]
        replacements.each { name, replacement ->
            def field = ZbsVolumeEncryptionMaterialFactory.getDeclaredField(name)
            field.accessible = true
            originals[name] = field.get(factory)
            field.set(factory, replacement)
        }
        try {
            return body.call(factory)
        } finally {
            originals.each { name, original ->
                def field = ZbsVolumeEncryptionMaterialFactory.getDeclaredField(name)
                field.accessible = true
                field.set(factory, original)
            }
        }
    }

    private <T> T withKvmCallerBus(CloudBus replacement, Closure<T> body) {
        ZbsVolumeEncryptionKvmCaller caller = bean(ZbsVolumeEncryptionKvmCaller.class)
        def field = ZbsVolumeEncryptionKvmCaller.getDeclaredField("bus")
        field.accessible = true
        CloudBus original = field.get(caller) as CloudBus
        field.set(caller, replacement)
        try {
            return body.call()
        } finally {
            field.set(caller, original)
        }
    }

    private static Object readMaterialField(Object material, String name) {
        def field = material.class.getDeclaredField(name)
        field.accessible = true
        return field.get(material)
    }

    private static class StubVolumeEncryptedSecretHelper extends VolumeEncryptedSecretHelper {
        List<Map<String, String>> requests = []
        List<Map<String, String>> sealRequests = []
        String sealedDek
        RuntimeException runtimeFailure

        @Override
        String materializeAndSealVolumeDekForHost(String hostUuid, String volumeUuid) {
            requests << [hostUuid: hostUuid, volumeUuid: volumeUuid]
            if (runtimeFailure != null) {
                throw runtimeFailure
            }
            return sealedDek
        }

        @Override
        String verifyHostKeyAndHpkeSealDek(String hostUuid, String resourceUuid, String dekBase64) {
            sealRequests << [hostUuid: hostUuid, resourceUuid: resourceUuid, dekBase64: dekBase64]
            return sealedDek
        }
    }

    private static class StubEncryptedResourceKeyManager extends DummyEncryptedResourceKeyManager {
        List<EncryptedResourceKeyManager.GetOrCreateResourceKeyContext> requests = []
        EncryptedResourceKeyManager.ResourceKeyResult result

        @Override
        EncryptedResourceKeyManager.ResourceKeyResult getExistingKeySync(
                EncryptedResourceKeyManager.GetOrCreateResourceKeyContext ctx) {
            requests << ctx
            return result
        }
    }

    private static class StubVolumeEncryptedResourceKeyBackend extends DummyVolumeEncryptedResourceKeyBackend {
        List<String> volumeRequests = []
        List<String> imageRequests = []
        String keyProviderUuid

        @Override
        String findKeyProviderUuidByVolume(String volumeUuid) {
            volumeRequests << volumeUuid
            return keyProviderUuid
        }

        @Override
        String findKeyProviderUuidByTemporarySnapshotImage(String imageUuid) {
            imageRequests << imageUuid
            return keyProviderUuid
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
