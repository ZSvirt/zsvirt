package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.springframework.http.HttpEntity
import org.springframework.web.util.UriComponentsBuilder
import org.zstack.core.Platform
import org.zstack.core.progress.ProgressCommands
import org.zstack.header.core.progress.ProgressConstants
import org.zstack.header.core.progress.TaskType
import org.zstack.header.image.APIAddImageMsg
import org.zstack.header.image.ImageConstant
import org.zstack.header.image.ImagePlatform
import org.zstack.header.longjob.LongJobState
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.Completion
import org.zstack.sdk.ErrorCode
import org.zstack.sdk.LongJobInventory
import org.zstack.sdk.TaskProgressInventory
import org.zstack.sdk.SubmitLongJobAction
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.FuncTrigger
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by kayo on 2018/3/29.
 */
class AddImageLongJobProgressCase extends SubCase {
    EnvSpec env
    Gson gson

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name = "ceph-bk"
                description = "Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]
            }
        }
    }

    @Override
    void test() {
        env.create {
            testProgressOfLongJob()
        }
    }

    void testProgressOfLongJob() {
        RESTFacade restf = bean(RESTFacade.class)
        BackupStorageInventory bs = env.inventoryByName("ceph-bk") as BackupStorageInventory
        gson = new Gson()

        def trigger = new FuncTrigger()

        env.afterSimulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) { rsp, HttpEntity<String> e ->
            Map cmd = JSONObjectUtil.toObject(e.getBody(), LinkedHashMap.class)

            UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl())
            ub.path(RESTConstant.COMMAND_CHANNEL_PATH)
            String url = ub.build().toUriString()

            int i = 0
            trigger.func = {
                i += 20
                def header = [(RESTConstant.COMMAND_PATH): ProgressConstants.PROGRESS_REPORT_PATH]
                def rcmd = new ProgressCommands.ProgressReportCmd()
                rcmd.progress = String.valueOf(i)
                rcmd.setThreadContextMap(cmd.threadContext)
                rcmd.setThreadContextStack(cmd.threadContextStack)
                restf.syncJsonPost(url, JSONObjectUtil.toJsonString(rcmd), header, ProgressCommands.ProgressReportResponse.class)
            }

            trigger.run()

            return rsp
        }

        APIAddImageMsg msg = new APIAddImageMsg()
        msg.setName("TinyLinux")
        msg.setBackupStorageUuids(Collections.singletonList(bs.uuid))
        msg.setUrl("http://192.168.1.20/share/images/tinylinux.qcow2")
        msg.setFormat(ImageConstant.QCOW2_FORMAT_STRING)
        msg.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())
        msg.setPlatform(ImagePlatform.Linux.toString())

        def id = Platform.getUuid()
        def a = new SubmitLongJobAction()
        a.jobName = msg.getClass().getSimpleName()
        a.jobData = gson.toJson(msg)
        a.description = "this is a long job to add image"
        a.sessionId = adminSession()
        a.apiId = id

        ErrorCode err = null
        LongJobInventory jobInv = null
        a.call(new Completion<SubmitLongJobAction.Result>() {
            @Override
            void complete(SubmitLongJobAction.Result ret) {
                err = ret.error
                jobInv = ret.value.inventory
            }
        })

        int num = 5
        for (i in 1..num) {
            trigger.trigger()

            retryInSecs {
                List<TaskProgressInventory> invs = getTaskProgress {
                    apiId = id
                }

                return {
                    // include 1 main progress and at less 1 sub progress
                    assert invs.size() >= 2

                    def sub = invs.find {
                        it.content.startsWith("agent-report-task-for")
                    }
                    assert sub != null
                    assert sub.currentStep == i * 20
                    assert sub.totalStep == 100
                }
            }
        }

        trigger.quit()

        retryInSecs(30) {
            assert err == null: "$err"
        }

        retryInSecs {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        List<TaskProgressInventory> invs = getTaskProgress {
            apiId = id
            all = true
        }

        assert invs.size() == 2

        deleteLongJob {
            uuid = jobInv.uuid
        }
    }
}
