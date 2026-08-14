package org.zstack.test.integration.storage.primary.smp

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.image.ImageDeletionPolicyManager
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.image.ImageGlobalConfig
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.test.integration.storage.SMPEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.path.PathUtil

import java.util.concurrent.TimeUnit
/**
 * Created by mingjian.deng on 2018/1/24.
 */
class CleanImageCacheOnSMPDeleteImageCase extends SubCase {
    EnvSpec env

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
        env = SMPEnv.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testDelete()
        }
    }

    void testDelete() {
        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
        def image = env.inventoryByName("image1") as ImageInventory
        deleteImage {
            uuid = image.uuid
        }

        def c = Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, image.uuid).find() as ImageCacheVO
        assert null != c

        def checked = false
        env.afterSimulator(KvmBackend.DELETE_BITS_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KvmBackend.DeleteBitsCmd.class)
            assert PathUtil.parentFolder(c.installUrl) == cmd.path
            checked = true
            return rsp
        }

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString());
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        destroyVmInstance {
            uuid = vm.uuid
        }

        PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL.updateValue(1)
        TimeUnit.SECONDS.sleep(2)

        assert checked
        assert null == Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, image.uuid).find()
    }
}
