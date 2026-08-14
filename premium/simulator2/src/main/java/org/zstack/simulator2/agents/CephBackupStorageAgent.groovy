package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.SimulatorGlobalProperty
import org.zstack.simulator2.config.backupStorage.CephBackupStorage
import org.zstack.simulator2.config.backupStorage.CephBackupStorageMon
import org.zstack.simulator2.config.backupStorage.CephBackupStoragePool
import org.zstack.storage.ceph.CephConstants
import org.zstack.storage.ceph.CephGlobalProperty
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.backup.CephBackupStorageMonBase
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO_
/**
 * Created by xing5 on 2017/9/19.
 */
class CephBackupStorageAgent extends Agent {
    CephBackupStorageAgent(Simulator simulator) {
        super(simulator)

        CephGlobalProperty.BACKUP_STORAGE_AGENT_PORT = SimulatorGlobalProperty.SIMULATOR_AGENT_PORT
    }

    private CephBackupStorage find(HttpEntity<String> e) {
        String monIp = e.getHeaders().getFirst(Simulator.REMOTE_ADDR)
        assert monIp != null

        def ceph = CephBackupStorage.class.simpleName
        def mon = CephBackupStorageMon.class.simpleName

        def ret = simulator.sqlite.find("select ${ceph}.* from ${ceph} inner join ${mon} where ${mon}.cephId = ${ceph}.id and ${mon}.ip = '${monIp}'", CephBackupStorage.class)
        assert ret != null : "CEPH backup storage[mon ip: ${monIp}] has no simulator"
        return ret
    }

    @Override
    void setupAgentHandler() {
        handle(CephBackupStorageBase.GET_FACTS) { HttpEntity<String> e ->
            CephBackupStorageBase.GetFactsCmd cmd = json(e, CephBackupStorageBase.GetFactsCmd.class)

            CephBackupStorageMonVO monvo = Q.New(CephBackupStorageMonVO.class).eq(CephBackupStorageMonVO_.uuid, cmd.monUuid).find()
            assert monvo != null : "cannot find mon[uuid:${monvo.uuid}] in database"
            CephBackupStorageMon mon = simulator.sqlite.find("select * from ${CephBackupStorageMon.class.simpleName} where ip = '${monvo.hostname}'", CephBackupStorageMon.class)
            assert mon != null : "ceph backup storage mon[ip:${monvo.hostname} has no simulator"
            CephBackupStorage bs = simulator.sqlite.find("select * from ${CephBackupStorage.class.simpleName} where id = '${mon.cephId}'", CephBackupStorage.class)
            assert bs != null

            def rsp = new CephBackupStorageBase.GetFactsRsp()
            rsp.fsid = bs.fsid
            rsp.monAddr = mon.monAddr

            return rsp
        }

        handle(CephBackupStorageBase.GET_IMAGE_SIZE_PATH) {
            def rsp = new CephBackupStorageBase.GetImageSizeRsp()
            rsp.size = 21474836480
            rsp.actualSize = 10737418240
            return rsp
        }

        handle(CephBackupStorageBase.INIT_PATH) { HttpEntity<String> e ->
            def ceph = find(e)

            def cmd = json(e.body, CephBackupStorageBase.InitCmd.class)
            if (cmd.pools != null) {
                cmd.pools.each { CephBackupStorageBase.Pool pool ->
                    boolean has = simulator.sqlite.findById(pool.name, CephBackupStoragePool.class) != null
                    if (!has && pool.predefined) {
                        throw new Exception("no pool[name:${pool.name}] found")
                    }

                    if (!has) {
                        simulator.sqlite.persist(new CephBackupStoragePool(id:pool.name, name: pool.name, cephId: ceph.id))
                    }
                }
            }

            def rsp = new CephBackupStorageBase.InitRsp()
            rsp.fsid = ceph.fsid
            rsp.totalCapacity = ceph.totalCapacity
            rsp.availableCapacity = ceph.availableCapacity
            if (cmd.pools != null) {
                List<CephPoolCapacity> poolCapacities = []
                cmd.pools.each { CephBackupStorageBase.Pool pool ->
                    if (poolCapacities.isEmpty()) {
                        poolCapacities.add(new CephPoolCapacity(
                                name : pool.name,
                                availableCapacity : rsp.availableCapacity,
                                usedCapacity : rsp.totalCapacity - rsp.availableCapacity,
                                totalCapacity: rsp.totalCapacity,
                                relatedOsds: "osd.1"
                        ))
                        return
                    }

                    poolCapacities.add(new CephPoolCapacity(
                            name : pool.name,
                            availableCapacity : 0,
                            usedCapacity : 0,
                            totalCapacity: 0,
                            relatedOsds: "osd.1"
                    ))
                }
                rsp.poolCapacities = poolCapacities
                rsp.type = CephConstants.CEPH_MANUFACTURER_OPENSOURCE
            }

            return rsp
        }

        handle(CephBackupStorageBase.CHECK_POOL_PATH) { HttpEntity<String> e ->
            def cmd = json(e.body, CephBackupStorageBase.CheckCmd.class)
            def ceph = find(e)

            cmd.pools.each { pool ->
                if (simulator.sqlite.find("select * from ${CephBackupStoragePool.class.simpleName} where cephId = '${ceph.id}' and name = '${pool.name}'", CephBackupStoragePool.class) == null) {
                    throw new Exception("no pool[name:${pool.name}] found")
                }
            }

            def rsp = new CephBackupStorageBase.CheckRsp()
            rsp.success = true
            return rsp
        }

        handle(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) {
            def rsp = new CephBackupStorageBase.DownloadRsp()
            rsp.size = 21474836480
            rsp.actualSize = 10737418240
            return rsp
        }

        handle(CephBackupStorageBase.DELETE_IMAGE_PATH) {
            return new CephBackupStorageBase.DeleteRsp()
        }

        handle(CephBackupStorageBase.CHECK_IMAGE_METADATA_FILE_EXIST) {
            def rsp = new CephBackupStorageBase.CheckImageMetaDataFileExistRsp()
            rsp.exist = true
            rsp.backupStorageMetaFileName = "bs_ceph_info.json"
            return rsp
        }

        handle(CephBackupStorageBase.DELETE_IMAGES_METADATA) {
            def rsp = new CephBackupStorageBase.DeleteImageInfoFromMetaDataFileRsp()
            rsp.out = "success delete"
            rsp.ret = 0
            return rsp
        }

        handle(CephBackupStorageBase.DUMP_IMAGE_METADATA_TO_FILE) {
            return new CephBackupStorageBase.DumpImageInfoToMetaDataFileRsp()
        }

        handle(CephBackupStorageBase.GET_IMAGES_METADATA) {
            def rsp = new CephBackupStorageBase.GetImagesMetaDataRsp()
            rsp.imagesMetadata = "{\"uuid\":\"a603e80ea18f424f8a5f00371d484537\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}"
            return rsp
        }

        handle(CephBackupStorageMonBase.PING_PATH) {
            CephBackupStorageMonBase.PingRsp rsp = new CephBackupStorageMonBase.PingRsp()
            rsp.success = true
            return rsp

        }

        handle(CephBackupStorageMonBase.ECHO_PATH) { HttpEntity<String> entity ->
            return [:]
        }

        handle(CephBackupStorageBase.CEPH_TO_CEPH_MIGRATE_IMAGE_PATH) {
            return new CephBackupStorageBase.StorageMigrationRsp()
        }

        handle(CephBackupStorageBase.FILE_DOWNLOAD_PATH) {
            def rsp = new CephBackupStorageBase.DownloadFileResponse()
            rsp.md5sum = "d41d8cd98f00b204e9800998ecf8427e"
            rsp.size = 3L * 1024 * 1024 * 1024
            return rsp
        }

        handle(CephBackupStorageBase.FILE_UPLOAD_PATH) {
            def rsp = new CephBackupStorageBase.UploadFileResponse()
            rsp.directUploadUrl = "http://127.0.0.1:7761/ceph/file/direct/upload"
            return rsp
        }

        handle(CephBackupStorageBase.FILE_DOWNLOAD_PROGRESS_PATH) {
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

        handle(CephBackupStorageBase.DELETE_FILES_PATH) {
            return new CephBackupStorageBase.DeleteFilesResponse()
        }

        handle(CephBackupStorageBase.UNZIP_FILE_PATH) {
            def rsp = new CephBackupStorageBase.UnzipFileResponse()
            rsp.unzipInstallPath = "/tmp/test-software-package/unzipInstallPath"
            rsp.fileSizes = [:]
            rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/Gateway_Linux_Server.qcow2", 1024L * 1024 * 1024)
            rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/BootImage_for_Linux.qcow2", 1024L * 1024 * 1024)
            rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/BootImage_for_Windows.qcow2", 1024L * 1024 * 1024)
            rsp.fileSizes.put("/tmp/test-software-package/unzipInstallPath/TrekerInstallation.tar.gz", 1024)
            return rsp
        }
    }
}
