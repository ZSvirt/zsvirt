package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.backup.BackupStorageVO
import org.zstack.header.storage.backup.BackupStorageVO_
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.SimulatorGlobalProperty
import org.zstack.simulator2.config.backupStorage.SftpBackupStorage
import org.zstack.storage.backup.sftp.SftpBackupStorageCommands
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.backup.sftp.SftpBackupStorageGlobalProperty
import org.zstack.storage.backup.sftp.SftpBackupStorageVO
import org.zstack.storage.backup.sftp.SftpBackupStorageVO_
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/9/19.
 */
class SftpBackupStorageAgent extends Agent {
    SftpBackupStorageAgent(Simulator simulator) {
        super(simulator)

        SftpBackupStorageGlobalProperty.AGENT_PORT = SimulatorGlobalProperty.SIMULATOR_AGENT_PORT
    }

    private SftpBackupStorage find(String uuid) {
        SftpBackupStorageVO vo = Q.New(SftpBackupStorageVO.class).eq(SftpBackupStorageVO_.uuid, uuid).find()
        assert vo != null : "cannot find backup storage[uuid:${vo.getUuid()}] in database"

        def bs = simulator.sqlite.find("select * from ${SftpBackupStorage.class.simpleName} where ip = '${vo.getHostname()}'", SftpBackupStorage.class)
        assert bs != null : "sftp backup storage[ip: ${vo.getHostname()}] has no simulator"

        return bs
    }

    @Override
    void setupAgentHandler() {
        handle(SftpBackupStorageConstant.CONNECT_PATH) { HttpEntity<String> e ->
            def cmd = json(e, SftpBackupStorageCommands.ConnectCmd.class)
            def bs = find(cmd.uuid)

            def rsp = new SftpBackupStorageCommands.ConnectResponse()
            rsp.totalCapacity = bs.totalCapacity
            rsp.availableCapacity = bs.availableCapacity
            return rsp
        }

        handle(SftpBackupStorageConstant.ECHO_PATH) { HttpEntity<String> e ->
            return [:]
        }

        handle(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), SftpBackupStorageCommands.DownloadCmd.class)
            def bs = find(cmd.uuid)

            def rsp = new SftpBackupStorageCommands.DownloadResponse()
            rsp.size = 21474836480
            rsp.actualSize = 10737418240
            rsp.availableCapacity = bs.availableCapacity
            rsp.totalCapacity = bs.totalCapacity
            return rsp
        }

        handle(SftpBackupStorageConstant.GET_IMAGE_SIZE) {
            def rsp = new SftpBackupStorageCommands.GetImageSizeRsp()
            rsp.actualSize = 10737418240
            rsp.size = 21474836480
            return rsp
        }

        handle(SftpBackupStorageConstant.DELETE_PATH) {
            return new SftpBackupStorageCommands.DeleteResponse()
        }

        handle(SftpBackupStorageConstant.PING_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, SftpBackupStorageCommands.PingCmd.class)
            def rsp = new SftpBackupStorageCommands.PingResponse()
            rsp.uuid = cmd.uuid
            return rsp
        }

        handle(SftpBackupStorageConstant.CHECK_IMAGE_METADATA_FILE_EXIST) {
            def rsp = new SftpBackupStorageCommands.CheckImageMetaDataFileExistRsp()
            rsp.exist = true
            rsp.backupStorageMetaFileName = "bs_file_info.json"
            return rsp
        }

        handle(SftpBackupStorageConstant.GENERATE_IMAGE_METADATA_FILE) {
            def rsp = new SftpBackupStorageCommands.GenerateImageMetaDataFileRsp()
            rsp.backupStorageMetaFileName = "bs_file_info.json"
            return rsp
        }

        handle(SftpBackupStorageConstant.DUMP_IMAGE_METADATA_TO_FILE) {
            return new SftpBackupStorageCommands.DumpImageInfoToMetaDataFileRsp()
        }

        handle(SftpBackupStorageConstant.DELETE_IMAGES_METADATA) {
            def rsp = new SftpBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp()
            rsp.out = "success"
            rsp.ret = 0
            return rsp
        }

        handle(SftpBackupStorageConstant.GET_IMAGES_METADATA) {
            def rsp = new SftpBackupStorageCommands.GetImagesMetaDataRsp()
            rsp.imagesMetaData = "{\"uuid\":\"a603e80ea18f424f8a5f00371d484537\",\"name\":\"test\",\"description\":\"\",\"state\":\"Enabled\",\"status\":\"Ready\",\"size\":19862528,\"actualSize\":15794176,\"md5Sum\":\"not calculated\",\"url\":\"http://192.168.200.1/mirror/diskimages/zstack-image-1.2.qcow2\",\"mediaType\":\"RootVolumeTemplate\",\"type\":\"zstack\",\"platform\":\"Linux\",\"format\":\"qcow2\",\"system\":false,\"createDate\":\"Dec 22, 2016 5:10:06 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\",\"backupStorageRefs\":[{\"id\":45,\"imageUuid\":\"a603e80ea18f424f8a5f00371d484537\",\"backupStorageUuid\":\"63879ceb90764f839d3de772aa646c83\",\"installPath\":\"/bs-sftp/rootVolumeTemplates/acct-36c27e8ff05c4780bf6d2fa65700f22e/a603e80ea18f424f8a5f00371d484537/zstack-image-1.2.template\",\"status\":\"Ready\",\"createDate\":\"Dec 22, 2016 5:10:08 PM\",\"lastOpDate\":\"Dec 22, 2016 5:10:08 PM\"}]}";
            return rsp
        }

        handle(SftpBackupStorageConstant.GET_IMAGE_HASH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, SftpBackupStorageCommands.GetImageHashCmd.class)
            def rsp = new SftpBackupStorageCommands.GetImageHashRsp()
            rsp.hash = cmd.path
            return rsp
        }
    }
}
