package org.zstack.test.integration.storage.primary.local.datavolume

import org.springframework.beans.BeanUtils
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.storage.primary.ImageCacheShadowVO
import org.zstack.header.storage.primary.ImageCacheShadowVO_
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.header.storage.primary.PrimaryStorageConstant
import org.zstack.sdk.GetVolumeCapabilitiesAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.LocalStorageMigrateVolumeAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageCreateEmptyVolumeMsg
import org.zstack.storage.primary.local.LocalStorageCreateEmptyVolumeReply
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow
import org.zstack.storage.primary.local.LocalStoragePrimaryStorageGlobalConfig
import org.zstack.storage.primary.local.LocalStorageUtils
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*
import org.zstack.utils.ObjectUtils

import static org.zstack.core.Platform.operr

/**
 * Created by camile on 2017/5/4.
 */
class MigrateVolumeCase extends SubCase {
    DatabaseFacade dbf
    EnvSpec env
    PrimaryStorageSpec primaryStorageSpec
    String psUuid
    KVMHostSpec kvm
    KVMHostSpec kvm1
    DiskOfferingSpec disk
    VmInstanceInventory vm
    ImageCacheVO originCache
    boolean isColdMigrateVm = new Random().nextBoolean()

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnvForPrimaryStorage()
    }

    @Override
    void test() {
        env.create {
            primaryStorageSpec = env.specByName("local")
            psUuid = primaryStorageSpec.inventory.uuid
            kvm = env.specByName("kvm")
            kvm1 = env.specByName("kvm1")
            disk = env.specByName("diskOffering")
            vm = env.inventoryByName("test-vm") as VmInstanceInventory
            dbf = bean(DatabaseFacade.class)
            prepareEnv()
            testMigrateRootVolumeWhenImageDeletedAndBaseImageIsNotImageCache()
            testMigrateRootVolumeWhenImageDeletedAndBaseImageIsNotImageCacheRollback()
            testMigrateRootVolumeWhenImageDeletedAndBaseImageIsImageCache()
            testMigrateRootVolumeWhenImageDeletedAndBaseImageIsImageCacheRollback()
            testMigrateRootVolumeWhenImageDeletedAndHaveNoBackingFile()
            testMigrateRootVolumeWhenImageDeletedAndHaveNoBackingFileRollback()
            testMigrateRootVolumeWhenImageDeletedAndGetBaseImageFail()
            testMigrateVmAttachedISO()
            testMigrateVolumeWhenPsIsMaintainFailure()
        }
    }

    void prepareEnv(){
        if (!isColdMigrateVm) {
            VmGlobalConfig.NUMA.updateValue(true)
            LocalStoragePrimaryStorageGlobalConfig.ALLOW_LIVE_MIGRATION.updateValue(true)
        } else {
            stopVmInstance {
                uuid = vm.uuid
            }
        }

        originCache = Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.imageUuid, vm.imageUuid)
                .like(ImageCacheVO_.installUrl, String.format("%%hostUuid://%s%%", kvm.inventory.uuid))
                .find()

        deleteImage {
            uuid = vm.imageUuid
        }
    }

    void testMigrateRootVolumeWhenImageDeletedAndBaseImageIsNotImageCache(){
        String dstHostUuid = kvm1.inventory.uuid
        SQL.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, vm.imageUuid).delete()

        coldOrLiveMigrateVmToHost(dstHostUuid)

        assertImageCacheVOIsExistedOnHost(dstHostUuid, true)
    }

    void testMigrateRootVolumeWhenImageDeletedAndBaseImageIsNotImageCacheRollback(){
        String dstHostUuid = kvm.inventory.uuid
        long originCap = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, dstHostUuid)
                .select(LocalStorageHostRefVO_.availableCapacity).findValue()

        def context = new Context(dstHostUuid)

        simulatorMigrateVolumeFail(context, {assertImageCacheVOIsExistedOnHost(dstHostUuid, true)})

        expect(AssertionError.class){
            coldOrLiveMigrateVmToHost(dstHostUuid)
        }

        assert context.called
        assertImageCacheVOIsExistedOnHost(dstHostUuid, false)
        assert originCap == Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, dstHostUuid)
                .select(LocalStorageHostRefVO_.availableCapacity).findValue()
        env.cleanSimulatorHandlers()
        env.cleanMessageHandlers()
    }

    void testMigrateRootVolumeWhenImageDeletedAndBaseImageIsImageCache(){
        String dstHostUuid = kvm.inventory.uuid

        env.simulator(LocalStorageKvmBackend.GET_BASE_IMAGE_PATH) {
            def rsp = new LocalStorageKvmBackend.GetVolumeBaseImagePathRsp()
            def path = new LocalStorageUtils.InstallPath();
            path.fullPath = originCache.installUrl
            path.disassemble()
            rsp.path = path.installPath
            rsp.size = 1
            return rsp
        }

        assertImageCacheVOIsExistedOnHost(dstHostUuid, false)

        coldOrLiveMigrateVmToHost(dstHostUuid)

        assertImageCacheVOIsExistedOnHost(dstHostUuid, true)
    }

    void testMigrateRootVolumeWhenImageDeletedAndBaseImageIsImageCacheRollback(){
        String dstHostUuid = kvm1.inventory.uuid
        long originCap = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, dstHostUuid)
                .select(LocalStorageHostRefVO_.availableCapacity).findValue()

        def context = new Context(dstHostUuid)

        simulatorMigrateVolumeFail(context, {assertImageCacheVOIsExistedOnHost(dstHostUuid, true)})

        SQL.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, vm.imageUuid).delete()
        mockImageCacheShadowOnHost(dstHostUuid)
        expect(AssertionError.class){
            coldOrLiveMigrateVmToHost(dstHostUuid)
        }
        assert context.called
        assertImageCacheVOIsExistedOnHost(dstHostUuid, false)
        assertImageCacheShadowVOIsExistedOnHost(dstHostUuid, true)
        assert originCap == Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, dstHostUuid)
                .select(LocalStorageHostRefVO_.availableCapacity).findValue()

        // test do not roll back existed vo
        mockImageCacheOnHost(dstHostUuid)
        SQL.New(ImageCacheShadowVO.class).eq(ImageCacheShadowVO_.imageUuid, vm.imageUuid).delete()
        context.called = false
        expect(AssertionError.class){
            coldOrLiveMigrateVmToHost(dstHostUuid)
        }

        assert context.called
        assertImageCacheVOIsExistedOnHost(dstHostUuid, true)
        assert originCap == Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, dstHostUuid)
                .select(LocalStorageHostRefVO_.availableCapacity).findValue()

        SQL.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, vm.imageUuid).delete()
        env.cleanSimulatorHandlers()
        env.cleanMessageHandlers()
    }

    void testMigrateRootVolumeWhenImageDeletedAndHaveNoBackingFile(){
        String dstHostUuid = kvm1.inventory.uuid

        boolean called
        env.simulator(LocalStorageKvmBackend.GET_BASE_IMAGE_PATH) { HttpEntity<String> entity ->
            def cmd = json(entity.body,LocalStorageKvmBackend.GetVolumeBaseImagePathCmd.class)
            def rsp = new LocalStorageKvmBackend.GetVolumeBaseImagePathRsp()
            rsp.path = null
            rsp.size = null
            called = true
            return rsp
        }

        SQL.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, vm.imageUuid).delete()

        coldOrLiveMigrateVmToHost(dstHostUuid)

        assert called
        assert Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, vm.imageUuid).isExists()
        env.cleanSimulatorHandlers()
        env.cleanMessageHandlers()
    }

    void testMigrateRootVolumeWhenImageDeletedAndHaveNoBackingFileRollback(){
        String dstHostUuid = kvm.inventory.uuid
        long originCap = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, dstHostUuid)
                .select(LocalStorageHostRefVO_.availableCapacity).findValue()

        def context = new Context(dstHostUuid)

        simulatorMigrateVolumeFail(context, {assertImageCacheVOIsExistedOnHost(dstHostUuid, true)})

        assertImageCacheVOIsExistedOnHost(dstHostUuid, false)
        expect(AssertionError.class){
            coldOrLiveMigrateVmToHost(dstHostUuid)
        }

        assert context.called
        assert originCap == Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, dstHostUuid)
                .select(LocalStorageHostRefVO_.availableCapacity).findValue()

        env.cleanSimulatorHandlers()
        env.cleanMessageHandlers()
    }

    void testMigrateRootVolumeWhenImageDeletedAndGetBaseImageFail(){
        String dstHostUuid = kvm.inventory.uuid

        env.simulator(LocalStorageKvmBackend.GET_BASE_IMAGE_PATH) {
            def rsp = new LocalStorageKvmBackend.GetVolumeBaseImagePathRsp()
            rsp.setError("on purpose")
            return rsp
        }

        coldOrLiveMigrateVmToHost(dstHostUuid)

        env.cleanSimulatorHandlers()
    }

    void testMigrateVmAttachedISO(){
        String dstHostUuid = kvm1.inventory.uuid
        def iso = env.inventoryByName("test-iso") as ImageInventory
        attachIsoToVmInstance {
            isoUuid = iso.uuid
            vmInstanceUuid = vm.uuid
        }
        expect(AssertionError){
            coldOrLiveMigrateVmToHost(dstHostUuid)
        }

        detachIsoFromVmInstance {
            vmInstanceUuid = vm.uuid
        }
        coldOrLiveMigrateVmToHost(dstHostUuid)
    }

    void testMigrateVolumeWhenPsIsMaintainFailure() {
        VolumeInventory dataVolume = createDataVolume {
            name = "1G"
            diskOfferingUuid = disk.inventory.uuid
            primaryStorageUuid = psUuid
            systemTags = Arrays.asList("localStorage::hostUuid::" + kvm.inventory.uuid)
        }
        changePrimaryStorageState {
            uuid = psUuid
            stateEvent = "maintain"
        }
        GetVolumeCapabilitiesAction getVolumeCapabilitiesAction = new GetVolumeCapabilitiesAction()
        getVolumeCapabilitiesAction.uuid = dataVolume.uuid
        getVolumeCapabilitiesAction.sessionId = adminSession()
        assert getVolumeCapabilitiesAction.call().error == null
        GetVolumeCapabilitiesAction.Result result = getVolumeCapabilitiesAction.call()
        assert result.value.capabilities.get("MigrationInCurrentPrimaryStorage") == false
        assert result.value.capabilities.get("MigrationToOtherPrimaryStorage") == false

        changePrimaryStorageState {
            uuid = psUuid
            stateEvent = "enable"
        }
        assert getVolumeCapabilitiesAction.call().error == null
        result = getVolumeCapabilitiesAction.call()
        assert result.value.capabilities.get("MigrationInCurrentPrimaryStorage") == true
        assert result.value.capabilities.get("MigrationToOtherPrimaryStorage") == false

        changePrimaryStorageState {
            uuid = psUuid
            stateEvent = "maintain"
        }
        assert getVolumeCapabilitiesAction.call().error == null
        result = getVolumeCapabilitiesAction.call()
        assert result.value.capabilities.get("MigrationInCurrentPrimaryStorage") == false
        assert result.value.capabilities.get("MigrationToOtherPrimaryStorage") == false

        LocalStorageMigrateVolumeAction localStorageMigrateVolumeAction = new LocalStorageMigrateVolumeAction()
        localStorageMigrateVolumeAction.volumeUuid = dataVolume.uuid
        localStorageMigrateVolumeAction.destHostUuid = kvm1.inventory.uuid
        localStorageMigrateVolumeAction.sessionId = adminSession()
        LocalStorageMigrateVolumeAction.Result res = localStorageMigrateVolumeAction.call()
        assert res.error != null
        assert res.error.code.toString() == "SYS.1007"
        assert res.error.description.toString() == "One or more API argument is invalid"
    }

    class Context {
        boolean called = false
        String dstHostUuid

        Context(String dstHostUuid){
            this.dstHostUuid = dstHostUuid
        }
    }

    private void simulatorMigrateVolumeFail(Context context, Runnable run){
        if (isColdMigrateVm) {
            env.simulator(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH) { HttpEntity<String> entity ->
                def cmd = json(entity.body,LocalStorageKvmMigrateVmFlow.CopyBitsFromRemoteCmd.class)
                def rsp = new LocalStorageKvmBackend.AgentResponse()
                if (cmd.stage == PrimaryStorageConstant.MIGRATE_VOLUME_AFTER_BACKING_FILE_COPY_STAGE) {
                    run.run()
                    context.called = true
                    throw new HttpError(404, "on purpose")
                }
                return rsp
            }
        } else {
            env.message(LocalStorageCreateEmptyVolumeMsg.class) { LocalStorageCreateEmptyVolumeMsg msg, CloudBus bus ->
                def reply = new LocalStorageCreateEmptyVolumeReply()
                run.run()
                reply.setError(operr("on purpose"))
                context.called = true
                bus.reply(msg, reply)
            }
        }
    }

    private void coldOrLiveMigrateVmToHost(String dstHostUuid){
        if (isColdMigrateVm) {
            localStorageMigrateVolume {
                volumeUuid = vm.rootVolumeUuid
                destHostUuid = dstHostUuid
            }
        } else {
            migrateVm {
                vmInstanceUuid = vm.uuid
                hostUuid = dstHostUuid
            }
        }
    }

    private void assertImageCacheVOIsExistedOnHost(String hostUuid, boolean exsit){
        assert Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.imageUuid, vm.imageUuid)
                .like(ImageCacheVO_.installUrl, String.format("%%hostUuid://%s%%", hostUuid))
                .isExists() == exsit
        if (exsit) {
            assert !Q.New(ImageCacheShadowVO.class)
                    .eq(ImageCacheShadowVO_.imageUuid, vm.imageUuid)
                    .like(ImageCacheShadowVO_.installUrl, String.format("%%hostUuid://%s%%", hostUuid))
                    .isExists()
        }
    }

    private void assertImageCacheShadowVOIsExistedOnHost(String hostUuid, boolean exsit){
        assert Q.New(ImageCacheShadowVO.class)
                .eq(ImageCacheShadowVO_.imageUuid, vm.imageUuid)
                .like(ImageCacheShadowVO_.installUrl, String.format("%%hostUuid://%s%%", hostUuid))
                .isExists() == exsit
    }

    private void mockImageCacheOnHost(String hostUuid) {
        ImageCacheVO mock = ObjectUtils.newAndCopy(originCache, ImageCacheVO.class)

        def path = new LocalStorageUtils.InstallPath();
        path.fullPath = mock.installUrl
        path.disassemble()
        path.hostUuid = hostUuid

        mock.id = 0
        mock.installUrl = path.makeFullPath()
        dbf.persist(mock)
    }

    private void mockImageCacheShadowOnHost(String hostUuid) {
        ImageCacheShadowVO mock = new ImageCacheShadowVO()
        BeanUtils.copyProperties(originCache, mock)

        def path = new LocalStorageUtils.InstallPath();
        path.fullPath = mock.installUrl
        path.disassemble()
        path.hostUuid = hostUuid

        mock.id = 0
        mock.installUrl = path.makeFullPath()
        dbf.persist(mock)
    }

    @Override
    void clean() {
        env.delete()
    }
}
