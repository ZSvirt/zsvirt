package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.progress.ProgressCommands
import org.zstack.core.progress.ProgressGlobalConfig
import org.zstack.header.core.progress.TaskProgressVO
import org.zstack.header.core.progress.TaskProgressVO_
import org.zstack.header.core.progress.ProgressConstants
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.header.vm.APICreateVmInstanceMsg
import org.zstack.sdk.*
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.FuncTrigger
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/3/23.
 */
class VmProgressCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmEnv()
    }

    void testCreateVmProgress() {
        RESTFacade restf = bean(RESTFacade.class)

        ImageInventory vmImage = env.inventoryByName("image1")

        def vmImagePath = vmImage.backupStorageRefs[0].installPath

        def ft = new FuncTrigger()

        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl())
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH)
        String url = ub.build().toUriString()

        logger.info("Test 001: Progress basic check: with agent reporter")
        env.hijackSimulator(LocalStorageKvmSftpBackupStorageMediatorImpl.DOWNLOAD_BIT_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.getBody(), LinkedHashMap.class)
            int i = 0
            while (i <= 5) {
                def header = [(RESTConstant.COMMAND_PATH): ProgressConstants.PROGRESS_REPORT_PATH]
                def rcmd = new ProgressCommands.ProgressReportCmd()
                rcmd.progress = String.valueOf(i++)
                rcmd.setThreadContextMap(cmd.threadContext)
                rcmd.setThreadContextStack(cmd.threadContextStack)
                rcmd.detail = ["remain":"17498636288", "total":"17498636288"]
                restf.syncJsonPost(url, JSONObjectUtil.toJsonString(rcmd), header, ProgressCommands.ProgressReportResponse.class)

                ft.trigger([cmd, rcmd])
            }

            return rsp
        }

        CreateVmInstanceAction a = new CreateVmInstanceAction()
        a.apiId = Platform.getUuid()
        a.sessionId = adminSession()
        a.imageUuid = vmImage.uuid
        a.instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
        a.l3NetworkUuids = [env.inventoryByName("l3").uuid]
        a.name = "vm"

        ft.func = {
            def (LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd cmd, ProgressCommands.ProgressReportCmd rcmd) = it

            if (cmd.backupStorageInstallPath == vmImagePath) {
                // downloading user vm image, 2 sub tasks here

                def invs = getTaskProgress {
                    apiId = a.apiId
                } as List<TaskProgressInventory>

                assert invs.size() >= 2

                // instantiate-volume-%s-local-primary-storage-%s
                def instantiateInv = invs.find {
                    it.content.matches("^instantiate-volume-[0-9a-f]{32}-local-primary-storage-[0-9a-f]{32}: .*")
                }
                assert instantiateInv != null
                assert instantiateInv.currentStep == 0
                assert instantiateInv.totalStep == 1

                // the next one is downloading user image
                def agentInv = invs.find {
                    it.content == "agent-report-task-for: " + APICreateVmInstanceMsg.class.getName()
                }
                assert agentInv != null
                assert agentInv.currentStep >= 0
                assert agentInv.currentStep < 100
                assert agentInv.totalStep == 100
                assert agentInv.opaque != null
                assert agentInv.opaque["remain"] != null
                assert agentInv.opaque["total"] != null

            } else {
                assert false: "should not be here: ${cmd.backupStorageInstallPath}"
            }
        }

        ErrorCode vmError = null
        a.call(new Completion<CreateVmInstanceAction.Result>() {
            @Override
            void complete(CreateVmInstanceAction.Result ret) {
                vmError = ret.error
                ft.quit()
            }
        })

        ft.run()

        retryInSecs(30) {
            assert vmError == null: "$vmError"
        }

        List<TaskProgressInventory> invs = getTaskProgress {
            apiId = a.apiId
            all = true
        }

        assert invs.size() != 0

        logger.info("Test 007: Progress will remain after API done")
        retryInSecs {
            assert Q.New(TaskProgressVO.class)
                    .eq(TaskProgressVO_.apiId, a.apiId)
                    .count() >= 3
        }
    }

    @Override
    void test() {
        env.create {
            ProgressGlobalConfig.CLEANUP_THREAD_INTERVAL.updateValue(1)
            testCreateVmProgress()
        }
    }
}
