package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.core.agent.AgentConstant
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/15.
 */
class SftpBackupStorageSpec extends BackupStorageSpec {
    @SpecParam
    String hostname = "127.0.0.1"
    @SpecParam
    String username = "root"
    @SpecParam
    String password = "password"

    SftpBackupStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec xspec) {
            def simulator = { arg1, arg2 ->
                xspec.simulator(arg1, arg2)
            }

            simulator(SftpBackupStorageConstant.CONNECT_PATH) { HttpEntity<String> e, EnvSpec spec ->
                Spec.checkHttpCallType(e, true)
                def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.ConnectCmd.class)
                BackupStorageSpec bsSpec = spec.specByUuid(cmd.uuid)

                def rsp = new SftpBackupStorageCommands.ConnectResponse()
                rsp.totalCapacity = bsSpec.totalCapacity
                rsp.availableCapacity = bsSpec.availableCapacity
                return rsp
            }

            simulator(SftpBackupStorageConstant.ECHO_PATH) { HttpEntity<String> e ->
                Spec.checkHttpCallType(e, true)
                return [:]
            }

            simulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
                BackupStorageSpec bsSpec = spec.specByUuid(cmd.uuid)
                ImageSpec imageSpec = spec.specByUuid(cmd.imageUuid)

                boolean useSpecSize = Boolean.valueOf(System.getProperty("useImageSpecSize"))

                def rsp = new SftpBackupStorageCommands.DownloadResponse()
                rsp.size = useSpecSize ? imageSpec.size : 0L
                rsp.actualSize = useSpecSize ? imageSpec.actualSize : 0L
                rsp.availableCapacity = bsSpec.availableCapacity
                rsp.totalCapacity = bsSpec.totalCapacity
                return rsp
            }

            simulator(SftpBackupStorageConstant.GET_IMAGE_SIZE) {HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.GetImageSizeCmd.class)
                def rsp = new SftpBackupStorageCommands.GetImageSizeRsp()
                ImageSpec imageSpec = spec.specByUuid(cmd.imageUuid)

                boolean useSpecSize = Boolean.valueOf(System.getProperty("useImageSpecSize"))
                rsp.size = useSpecSize ? imageSpec.size : 0L
                rsp.actualSize = useSpecSize ? imageSpec.actualSize : 0L
                return rsp
            }

            simulator(AgentConstant.CANCEL_JOB) {
                def rsp = new SftpBackupStorageCommands.AgentResponse()
            }

            simulator(SftpBackupStorageConstant.DELETE_PATH) {
                return new SftpBackupStorageCommands.DeleteResponse()
            }

            simulator(SftpBackupStorageConstant.PING_PATH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, SftpBackupStorageCommands.PingCmd.class)
                def rsp = new SftpBackupStorageCommands.PingResponse()
                rsp.uuid = cmd.uuid
                return rsp
            }

            simulator(SftpBackupStorageConstant.CHECK_IMAGE_METADATA_FILE_EXIST) {
                def rsp = new SftpBackupStorageCommands.CheckImageMetaDataFileExistRsp()
                rsp.exist = true
                rsp.backupStorageMetaFileName = "bs_file_info.json"
                return rsp
            }

            simulator(SftpBackupStorageConstant.GENERATE_IMAGE_METADATA_FILE) {
                def rsp = new SftpBackupStorageCommands.GenerateImageMetaDataFileRsp()
                rsp.backupStorageMetaFileName = "bs_file_info.json"
                return rsp
            }

            simulator(SftpBackupStorageConstant.DUMP_IMAGE_METADATA_TO_FILE) {
                return new SftpBackupStorageCommands.DumpImageInfoToMetaDataFileRsp()
            }

            simulator(SftpBackupStorageConstant.DELETE_IMAGES_METADATA) {
                def rsp = new SftpBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp()
                rsp.out = "success"
                rsp.ret = 0
                return rsp
            }

            simulator(SftpBackupStorageConstant.GET_IMAGES_METADATA) {
                def rsp = new SftpBackupStorageCommands.GetImagesMetaDataRsp()
                rsp.imagesMetaData = "{\"uuid\":\"a603e80ea18f424f8a5f00371d484537\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}";
                return rsp
            }

            simulator(SftpBackupStorageConstant.GET_IMAGE_HASH) { HttpEntity<String> e, EnvSpec spec ->
                def cmd = JSONObjectUtil.toObject(e.body, SftpBackupStorageCommands.GetImageHashCmd.class)

                def rsp = new SftpBackupStorageCommands.GetImageHashRsp()
                rsp.hash = cmd.path
                return rsp
            }
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addSftpBackupStorage {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.url = url
            delegate.sessionId = sessionId
            delegate.hostname = hostname
            delegate.username = username
            delegate.password = password
            delegate.userTags = userTags
            delegate.systemTags = systemTags
        }

        postCreate {
            inventory = querySftpBackupStorage {
                conditions = ["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
