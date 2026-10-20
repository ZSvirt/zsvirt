package org.zstack.test.integration.storage.backup.imagestore

import org.springframework.http.HttpEntity
import org.zstack.header.core.Completion
import org.zstack.header.core.FutureCompletion
import org.zstack.header.rest.RESTFacade
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorage
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageCommands.UploadImageToRemoteTargetCmd
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageCommands.UploadImageToRemoteTargetRsp
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageGlobalProperty
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.WebBeanConstructor
import org.zstack.testlib.premium.PremiumEnv
import org.zstack.utils.gson.JSONObjectUtil

class ImageStoreRemoteUploadIdentityCase extends SubCase {
    EnvSpec env
    Integer originalAgentPort
    String originalAgentUrlRootPath

    @Override
    void setup() {
        useSpring(PremiumEnv.makeSpring())
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void test() {
        env.create {
            testRemoteUploadIdentity()
        }
        if (originalAgentPort != null) {
            ImageStoreBackupStorageGlobalProperty.AGENT_PORT = originalAgentPort
            ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH = originalAgentUrlRootPath
        }
    }

    void testRemoteUploadIdentity() {
        originalAgentPort = ImageStoreBackupStorageGlobalProperty.AGENT_PORT
        originalAgentUrlRootPath = ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH
        ImageStoreBackupStorageGlobalProperty.AGENT_PORT = WebBeanConstructor.port
        ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH = ""
        verifyRequest("61231d117e254da1bec5c9948d84cd6a", true)
        verifyRequest("a1231d117e254da1bec5c9948d84cd6a", false)
    }

    private void verifyRequest(String uuid, boolean success) {
        HttpEntity<String> observed = null
        env.simulator(ImageStoreBackupStorageConstant.UPLOAD_IMAGE_TO_REMOTE_PATH) { HttpEntity<String> entity ->
            observed = entity
            def response = new UploadImageToRemoteTargetRsp()
            if (!success) {
                response.setError("marker unavailable")
            }
            return response
        }
        def backend = new ImageStoreBackupStorage(new ImageStoreBackupStorageVO(uuid: uuid, hostname: "localhost"))
        def restField = ImageStoreBackupStorage.getDeclaredField("restf")
        restField.accessible = true
        restField.set(backend, bean(RESTFacade))
        def command = new UploadImageToRemoteTargetCmd(
                installPath: "zstore://iso/12345678901234567890123456789012",
                remoteTargetUrl: "nbd://127.0.0.1:10809", format: "raw", imageFormat: "iso", concurrency: 3)
        FutureCompletion completion = new FutureCompletion(null)
        def upload = ImageStoreBackupStorage.getDeclaredMethod("uploadToRemote", UploadImageToRemoteTargetCmd, Completion)
        upload.accessible = true
        upload.invoke(backend, command, completion)
        completion.await(10000)
        assert !completion.timeout
        assert observed != null
        assert observed.headers.getFirst("X-ZStack-ImageStore-UUID") == uuid
        assert observed.headers.getFirst("taskuuid")
        assert observed.headers.getFirst("callbackurl")
        def body = JSONObjectUtil.toObject(observed.body, UploadImageToRemoteTargetCmd)
        assert body.installPath == command.installPath
        assert body.remoteTargetUrl == command.remoteTargetUrl
        assert body.format == "raw" && body.imageFormat == "iso" && body.concurrency == 3
        assert completion.success == success
        if (!success) {
            assert completion.errorCode.toString().contains("marker unavailable")
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
