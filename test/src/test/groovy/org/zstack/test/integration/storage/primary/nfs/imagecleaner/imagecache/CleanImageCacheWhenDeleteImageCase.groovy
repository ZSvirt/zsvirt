package org.zstack.test.integration.storage.primary.nfs.imagecleaner.imagecache

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SimpleQuery
import org.zstack.header.image.ImageDeletionPolicyManager
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.image.ImageGlobalConfig
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.path.PathUtil
import java.util.concurrent.TimeUnit

/**
 * Created by lining on 2017/3/25.
 */
// base on TestNfsImageCleaner
class CleanImageCacheWhenDeleteImageCase extends SubCase{

    EnvSpec env
    DatabaseFacade dbf

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
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testDelete()
        }
    }

    void testDelete(){
        dbf = bean(DatabaseFacade.class)

        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
        ImageInventory image1 = env.inventoryByName("image1")
        deleteImage {
            uuid = image1.uuid
        }

        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class)
        q.add(ImageCacheVO_.imageUuid, SimpleQuery.Op.EQ, image1.getUuid())
        ImageCacheVO c = q.find()
        assert null != c

        def checked = false
        env.afterSimulator(NfsPrimaryStorageKVMBackend.DELETE_PATH) { rsp, HttpEntity<String> e ->
            NfsPrimaryStorageKVMBackendCommands.DeleteCmd cmd = JSONObjectUtil.toObject(e.body, NfsPrimaryStorageKVMBackendCommands.DeleteCmd.class)
            assert PathUtil.parentFolder(c.installUrl) == cmd.installPath
            checked = true
            return rsp
        }

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString());
        VmInstanceInventory vm1 = env.inventoryByName("vm")
        destroyVmInstance {
            uuid = vm1.uuid
        }

        PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL.updateValue(1)
        TimeUnit.SECONDS.sleep(3)

        assert checked
        c = q.find()
        assert null == c
    }
}