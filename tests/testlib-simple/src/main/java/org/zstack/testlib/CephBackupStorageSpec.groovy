package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.agent.AgentConstant
import org.zstack.core.db.Q
import org.zstack.storage.ceph.CephConstants
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.DataSecurityPolicy
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.backup.CephBackupStorageMonBase
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO_
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.testlib.vfs.VFS
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/20.
 */
class CephBackupStorageSpec extends BackupStorageSpec {
    @SpecParam(required = true)
    String fsid
    @SpecParam(required = true)
    List<String> monUrls
    @SpecParam
    Map<String, String> monAddrs = [:]
    @SpecParam
    String poolName = "bak-t-" + Platform.getUuid()

    CephBackupStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    static VFS vfs(CephBackupStorageBase.AgentCommand cmd, EnvSpec env, boolean errorOnNotExisting=false) {
        return CephPrimaryStorageSpec.vfs1(cmd.fsid, env, errorOnNotExisting)
    }

    static String cephPathToVFSPath(String str) {
        return CephPrimaryStorageSpec.cephPathToVFSPath(str)
    }

    static class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec espec) {
            def simulator = { arg1, arg2 ->
                espec.simulator(arg1, arg2)
            }

            simulator(CephBackupStorageBase.GET_FACTS) { HttpEntity<String> e, EnvSpec spec ->
                CephBackupStorageBase.GetFactsCmd cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.GetFactsCmd.class)
                CephBackupStorageSpec bspec = spec.specByUuid(cmd.uuid)
                assert bspec != null: "cannot find the backup storage[uuid:${cmd.uuid}}, check your environment()"

                def rsp = new CephBackupStorageBase.GetFactsRsp()
                rsp.fsid = bspec.fsid

                String monAddr = Q.New(CephBackupStorageMonVO.class).select(CephBackupStorageMonVO_.monAddr)
                        .eq(CephBackupStorageMonVO_.uuid, cmd.monUuid).findValue()

                rsp.monAddr = bspec.monAddrs[(monAddr)]
                return rsp
            }

            VFS.vfsHook(CephBackupStorageBase.GET_IMAGE_SIZE_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                CephBackupStorageBase.GetImageSizeCmd cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.GetImageSizeCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.Assert(vfs.exists(cephPathToVFSPath(cmd.installPath)), "cannot find file[${cmd.installPath}]")
            }

            simulator(CephBackupStorageBase.GET_IMAGE_SIZE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def rsp = new CephBackupStorageBase.GetImageSizeRsp()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            VFS.vfsHook(CephBackupStorageBase.INIT_PATH, espec) { CephBackupStorageBase.InitRsp rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.InitCmd.class)
                CephBackupStorageSpec bspec = spec.specByUuid(cmd.uuid)

                if (bspec == null) {
                    cmd.fsid = rsp.fsid
                } else {
                    cmd.fsid = bspec.fsid
                }

                VFS vfs = vfs(cmd, spec)

                // if spec has those pool names defined, it means those pools are pre-created
                // in ceph, here we simulate the pre-created logic
                if (bspec == null) {
                    rsp.poolCapacities.forEach({ pool ->
                        if (pool.name != null) {
                            String dir = cephPathToVFSPath(pool.name)
                            if (!vfs.exists(dir)) {
                                vfs.createDirectories(dir)
                            }
                        }
                    })
                } else if (bspec.poolName != null) {
                    String dir = cephPathToVFSPath(bspec.poolName)
                    if (!vfs.exists(dir)) {
                        vfs.createDirectories(dir)
                    }
                }

                cmd.pools.each { CephBackupStorageBase.Pool pool ->
                    String dir = cephPathToVFSPath(pool.name)
                    if (pool.predefined) {
                        vfs.Assert(vfs.isDir(dir), "cannot find ceph pool[${pool.name}]")
                    } else {
                        vfs.createDirectories(dir)
                    }
                }

                return rsp
            }

            simulator(CephBackupStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.InitCmd.class)
                CephBackupStorageSpec bspec = spec.specByUuid(cmd.uuid)
                assert bspec != null: "cannot find the backup storage[uuid:${cmd.uuid}}, check your environment()"

                def rsp = new CephBackupStorageBase.InitRsp()
                rsp.fsid = bspec.fsid
                rsp.totalCapacity = bspec.totalCapacity
                rsp.availableCapacity = bspec.availableCapacity
                List<CephPoolCapacity> poolCapacities = [
                        new CephPoolCapacity(
                                name : bspec.poolName,
                                availableCapacity : bspec.availableCapacity,
                                usedCapacity : bspec.totalCapacity - bspec.availableCapacity,
                                totalCapacity: bspec.totalCapacity,
                                replicatedSize: 3,
                                diskUtilization: 0.67,
                                securityPolicy: DataSecurityPolicy.ErasureCode.toString(),
                                relatedOsds: "osd.1"
                        )
                ]
                rsp.poolCapacities = poolCapacities
                rsp.type = CephConstants.CEPH_MANUFACTURER_OPENSOURCE
                return rsp
            }

            simulator(CephBackupStorageBase.CHECK_POOL_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def rsp = new CephBackupStorageBase.CheckRsp()
                rsp.success = true
                return rsp
            }

            VFS.vfsHook(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.DownloadCmd.class)

                VFS vfs = vfs(cmd, spec)
                vfs.createCephRaw(cephPathToVFSPath(cmd.installPath), 0L)
                return rsp
            }

            simulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def rsp = new CephBackupStorageBase.DownloadRsp()
                rsp.size = 0
                rsp.actualSize = 0
                return rsp
            }

            VFS.vfsHook(CephBackupStorageBase.DELETE_IMAGE_PATH, espec) { rsp, HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.DeleteCmd.class)
                VFS vfs = vfs(cmd, spec)
                vfs.delete(cephPathToVFSPath(cmd.installPath))
            }

            simulator(CephBackupStorageBase.DELETE_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                return new CephBackupStorageBase.DeleteRsp()
            }

            simulator(CephBackupStorageBase.ADD_EXPORT_TOKEN_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.AddImageExportTokenCmd.class)
                assert cmd.installPath.startsWith("ceph://")
                return new CephBackupStorageBase.AddImageExportTokenCmd()
            }

            simulator(CephBackupStorageBase.REMOVE_EXPORT_TOKEN_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, CephBackupStorageBase.RemoveImageExportTokenCmd.class)
                assert cmd.installPath.startsWith("ceph://")
                return new CephBackupStorageBase.RemoveImageExportTokenRsp()
            }

            simulator(AgentConstant.CANCEL_JOB) {
                return new CephBackupStorageBase.AgentResponse()
            }


            simulator(CephBackupStorageBase.CHECK_IMAGE_METADATA_FILE_EXIST) {
                def rsp = new CephBackupStorageBase.CheckImageMetaDataFileExistRsp()
                rsp.exist = true
                rsp.backupStorageMetaFileName = "bs_ceph_info.json"
                return rsp
            }

            simulator(CephBackupStorageBase.DELETE_IMAGES_METADATA) {
                def rsp = new CephBackupStorageBase.DeleteImageInfoFromMetaDataFileRsp()
                rsp.out = "success delete"
                rsp.ret = 0
                return rsp
            }

            simulator(CephBackupStorageBase.DUMP_IMAGE_METADATA_TO_FILE) {
                return new CephBackupStorageBase.DumpImageInfoToMetaDataFileRsp()
            }

            simulator(CephBackupStorageBase.GET_IMAGES_METADATA) {
                def rsp = new CephBackupStorageBase.GetImagesMetaDataRsp()
                rsp.imagesMetadata = "{\"uuid\":\"a603e80ea18f424f8a5f00371d484537\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}"
                return rsp
            }

            simulator(CephBackupStorageMonBase.PING_PATH) {
                CephBackupStorageMonBase.PingRsp rsp = new CephBackupStorageMonBase.PingRsp()
                rsp.success = true
                return rsp

            }

            simulator(CephBackupStorageMonBase.ECHO_PATH) { HttpEntity<String> entity ->
                Spec.checkHttpCallType(entity, true)
                return [:]
            }

            simulator(CephBackupStorageBase.CEPH_TO_CEPH_MIGRATE_IMAGE_PATH) { HttpEntity<String> entity ->
                return new CephBackupStorageBase.StorageMigrationRsp()
            }

            simulator(CephBackupStorageBase.FILE_DOWNLOAD_PATH) { HttpEntity<String> entity ->
                def rsp = new CephBackupStorageBase.DownloadFileResponse()
                rsp.md5sum = "d41d8cd98f00b204e9800998ecf8427e"
                rsp.size = 3L * 1024 * 1024 * 1024
                return rsp
            }

            simulator(CephBackupStorageBase.FILE_UPLOAD_PATH) { HttpEntity<String> entity ->
                def rsp = new CephBackupStorageBase.UploadFileResponse()
                rsp.directUploadUrl = "http://127.0.0.1:7761/ceph/file/direct/upload"
                return rsp
            }

            simulator(CephBackupStorageBase.FILE_DOWNLOAD_PROGRESS_PATH) { HttpEntity<String> entity ->
                def rsp = new CephBackupStorageBase.GetDownloadFileProgressResponse()
                rsp.completed = true
                rsp.progress = 100
                rsp.size = 3L * 1024 * 1024 * 1024
                rsp.actualSize = 3L * 1024 * 1024 * 1024
                rsp.installPath = "/tmp/test-software-package/unzipInstallPath"
                rsp.format = "qcow2"
                rsp.lastOpTime = System.currentTimeMillis()
                rsp.downloadSize = 3L * 1024 * 1024 * 1024
                rsp.md5sum = "d41d8cd98f00b204e9800998ecf8427e"
                rsp.supportSuspend = true
                return rsp
            }

            simulator(CephBackupStorageBase.DELETE_FILES_PATH) { HttpEntity<String> entity ->
                def rsp = new CephBackupStorageBase.DeleteFilesResponse()
                return rsp
            }

            simulator(CephBackupStorageBase.UNZIP_FILE_PATH) { HttpEntity<String> entity ->
                def rsp = new CephBackupStorageBase.UnzipFileResponse()
                rsp.unzipInstallPath = "/tmp/test-software-package/unzipInstallPath"
                rsp.fileSizes = [:]
                rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/Gateway_Linux_Server.qcow2", 1024L * 1024 * 1024)
                rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/BootImage_for_Linux.qcow2", 1024L * 1024 * 1024)
                rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/BootImage_for_Windows.qcow2", 1024L * 1024 * 1024)
                rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/TrekerInstallation.tar.gz", 1024)
                return rsp
            }

            simulator(CephBackupStorageBase.SOFTWARE_UPGRADE_PACKAGE_DEPLOY_PATH) { HttpEntity<String> entity ->
                def rsp = new CephBackupStorageBase.SoftwareUpgradePackageResponse()
                return rsp
            }
        }
    }

    SpecID create(String uuid, String sessionId) {


        inventory = addCephBackupStorage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.url = url
            delegate.sessionId = sessionId
            delegate.monUrls = monUrls
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.poolName = poolName
        }

        postCreate {
            inventory = queryCephBackupStorage {
                conditions = ["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
