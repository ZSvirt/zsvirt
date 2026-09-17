package org.zstack.test.integration.storage.volume

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.gc.GarbageCollectorType
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.primary.DownloadDataVolumeToPrimaryStorageMsg
import org.zstack.header.storage.primary.DownloadDataVolumeToPrimaryStorageReply
import org.zstack.header.volume.VolumeVO
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.primary.PrimaryStorageDeleteBitGC
import org.zstack.storage.primary.PrimaryStoragePathMaker
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.path.PathUtil

import static org.zstack.core.Platform.operr
/**
 * Created by MaJin on 2017-08-15.
 */
class CreateDataVolumeFromTemplateCase extends SubCase{
    EnvSpec env
    ImageInventory image
    PrimaryStorageInventory ps
    BackupStorageInventory bs
    HostInventory host

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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
                    mediaType = ImageConstant.ImageMediaType.DataVolumeTemplate.toString()
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
                        totalCpu = 40
                        totalMem = SizeUnit.GIGABYTE.toByte(320)
                    }

                    attachPrimaryStorage("local")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            simulator()
            prepare()
            testRollbackWhenCreateFail()
            closeGC()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testRollbackWhenCreateFail(){
        def result = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        } as GetPrimaryStorageCapacityResult
        def availableCapacity = result.availableCapacity
        def availablePhysicalCapacity = result.availablePhysicalCapacity

        def installPath
        String volumeUuid = Platform.uuid

        env.message(DownloadDataVolumeToPrimaryStorageMsg.class){DownloadDataVolumeToPrimaryStorageMsg msg, CloudBus bus ->
            def reply = new DownloadDataVolumeToPrimaryStorageReply()
            reply.setError(operr("on purpose"))
            installPath = PathUtil.join(ps.getUrl(), PrimaryStoragePathMaker.makeDataVolumeInstallPath(volumeUuid))
            bus.reply(msg, reply)
        }

        def a = new CreateDataVolumeFromVolumeTemplateAction()
        a.resourceUuid = volumeUuid
        a.name = "test"
        a.imageUuid = image.uuid
        a.primaryStorageUuid = ps.uuid
        a.hostUuid = host.uuid
        a.sessionId = currentEnvSpec.session.uuid

        assert a.call().error != null

        String gcName = String.format("gc-delete-bits-volume-%s-on-primary-storage-%s", volumeUuid, ps.uuid)

        List<GarbageCollectorInventory> gcJobs = queryGCJob {
            conditions = ["name=${gcName}"]
        }

        result = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        } as GetPrimaryStorageCapacityResult

        assert result.availableCapacity == availableCapacity
        assert result.availablePhysicalCapacity == availablePhysicalCapacity

        assert gcJobs.size() == 1
        def gcJob = gcJobs.get(0) as GarbageCollectorInventory
        assert gcJob.type == GarbageCollectorType.TimeBased.name()
        def gc = JSONObjectUtil.toObject(gcJob.context, PrimaryStorageDeleteBitGC.class)
        assert gc.primaryStorageUuid == ps.uuid
        assert installPath != null
        assert gc.primaryStorageInstallPath == installPath
        assert gc.NEXT_TIME == 600

        assert Q.New(VolumeVO.class).count() == 0L
    }

    void closeGC() {
        SQL.New("delete from GarbageCollectorVO").execute()

        updateGlobalConfig {
            category = "primaryStorage"
            name = "primarystorage.delete.bits.garbage.on"
            value = false
        }

        def a = new CreateDataVolumeFromVolumeTemplateAction()
        a.name = "test"
        a.imageUuid = image.uuid
        a.primaryStorageUuid = ps.uuid
        a.hostUuid = host.uuid
        a.sessionId = currentEnvSpec.session.uuid

        assert a.call().error != null
        List<GarbageCollectorInventory> gcJobs = queryGCJob {
            conditions = ["name~=gc-delete-bits-volume-%-on-primary-storage-%"]
        }

        assert 0 == gcJobs.size()
    }

    void simulator() {
        env.simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) {
            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = SizeUnit.GIGABYTE.toByte(2)
            rsp.actualSize = SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }
    }

    void prepare() {
        bs = env.inventoryByName("sftp") as BackupStorageInventory
        image = addImage {
            name = "image2"
            url = "http://zstack.org/download/test.qcow2"
            mediaType = ImageConstant.ImageMediaType.DataVolumeTemplate.toString()
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        } as ImageInventory

        ps = env.inventoryByName("local") as PrimaryStorageInventory
        host = env.inventoryByName("kvm") as HostInventory
    }
}
