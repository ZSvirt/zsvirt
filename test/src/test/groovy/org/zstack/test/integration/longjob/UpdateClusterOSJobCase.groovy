package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.springframework.http.HttpEntity
import org.zstack.compute.cluster.ClusterGlobalConfig
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.thread.ChainTaskStatistic
import org.zstack.core.thread.ThreadFacadeImpl
import org.zstack.header.cluster.APIUpdateClusterOSMsg
import org.zstack.header.host.HostState
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.longjob.LongJobState
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by GuoYi on 3/13/18
 */
class UpdateClusterOSJobCase extends SubCase {
    Gson gson
    EnvSpec env
    DatabaseFacade dbf
    ThreadFacadeImpl thdf

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
                }
            }
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster3"
                    hypervisorType = "Simulator"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "127.0.0.3:/nfs_root"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm1"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useHost("kvm1")
            }

            vm {
                name = "vm2"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useHost("kvm3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            gson = new Gson()
            dbf = bean(DatabaseFacade.class)
            thdf = bean(ThreadFacadeImpl.class)

            testUpdateClusterWithExperimentalRepoEnabled()
            testUpdateClusterWithNfsRunningOnHost()
            testUpdateHostNotKvm()
            testUpdateClusterWithHostNotConnected()
            testUpdateClusterWithHostPreMaintenance()
            testUpdateClusterWithHostMaintenance()
            testUpdateClusterUsingAPI()
            testUpdateClusterExcludePackages()
            testUpdateClusterParallelismDegree()
        }
    }


    void testUpdateClusterWithExperimentalRepoEnabled() {
        ClusterInventory cls1 = env.inventoryByName("cluster1") as ClusterInventory
        ClusterInventory cls2 = env.inventoryByName("cluster2") as ClusterInventory

        def cmdList = Collections.synchronizedList(new ArrayList())
        env.afterSimulator(KVMConstant.KVM_UPDATE_HOST_OS_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.UpdateHostOSCmd.class)
            cmdList.add(cmd)
            return rsp
        }

        def cfgName = ClusterGlobalConfig.ZSTACK_EXPERIMENTAL_REPO.name
        def cfgCategory = ClusterGlobalConfig.ZSTACK_EXPERIMENTAL_REPO.category

        def result = getResourceConfig {
            resourceUuid = cls1.uuid
            name = cfgName
            category = cfgCategory
        } as GetResourceConfigResult
        assert result.value == 'false'

        result = getResourceConfig {
            resourceUuid = cls2.uuid
            name = cfgName
            category = cfgCategory
        } as GetResourceConfigResult
        assert result.value == 'false'

        // update os of cluster1
        updateClusterOS {
            uuid = cls1.uuid
        }

        retryInSecs {
            assert cmdList.size() == 2
            cmdList.each {
                assert !it.enableExpRepo
            }
        }

        // enable global experimental repo
        updateGlobalConfig {
            name = cfgName
            category = cfgCategory
            value = true
        }

        result = getResourceConfig {
            resourceUuid = cls1.uuid
            name = cfgName
            category = cfgCategory
        } as GetResourceConfigResult
        assert result.value == 'true'

        result = getResourceConfig {
            resourceUuid = cls2.uuid
            name = cfgName
            category = cfgCategory
        } as GetResourceConfigResult
        assert result.value == 'true'

        updateResourceConfig {
            resourceUuid = cls2.uuid
            name = cfgName
            category = cfgCategory
            value = false
        }

        result = getResourceConfig {
            resourceUuid = cls2.uuid
            name = cfgName
            category = cfgCategory
        } as GetResourceConfigResult
        assert result.value == 'false'

        // update os of cluster1
        cmdList.clear()
        updateClusterOS {
            uuid = cls1.uuid
        }

        retryInSecs {
            assert cmdList.size() == 2
            cmdList.each {
                assert it.enableExpRepo
            }
        }
    }

    void testUpdateClusterWithNfsRunningOnHost() {
        ClusterInventory cls = env.inventoryByName("cluster2") as ClusterInventory

        // try to update cluster os
        LongJobInventory jobInv = updateClusterOS {
            uuid = cls.uuid
        }

        assert jobInv.getJobName() == "APIUpdateClusterOSMsg"
        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Failed
        }
    }

    void testUpdateHostNotKvm() {
        ClusterInventory cls = env.inventoryByName("cluster3") as ClusterInventory

        UpdateClusterOSAction action = new UpdateClusterOSAction()
        action.sessionId = adminSession()
        action.uuid = cls.uuid
        UpdateClusterOSAction.Result result = action.call()
        assert result.error != null
    }

    void testUpdateClusterWithHostNotConnected() {
        ClusterInventory cls = env.inventoryByName("cluster1") as ClusterInventory
        HostInventory kvm1 = env.inventoryByName("kvm1") as HostInventory

        // set kvm1 connecting
        HostVO hvo = dbFindByUuid(kvm1.uuid, HostVO.class)
        hvo.status = HostStatus.Connecting
        dbf.update(hvo)

        // try to update cluster os
        UpdateClusterOSAction action = new UpdateClusterOSAction()
        action.sessionId = adminSession()
        action.uuid = cls.uuid
        UpdateClusterOSAction.Result result = action.call()
        assert result.error != null

        // recovery
        hvo = dbFindByUuid(kvm1.uuid, HostVO.class)
        hvo.status = HostStatus.Connected
        dbf.update(hvo)
    }

    void testUpdateClusterWithHostPreMaintenance() {
        ClusterInventory cls = env.inventoryByName("cluster1") as ClusterInventory
        HostInventory kvm1 = env.inventoryByName("kvm1") as HostInventory

        // set kvm1 premaintenance
        HostVO hvo = dbFindByUuid(kvm1.uuid, HostVO.class)
        hvo.state = HostState.PreMaintenance
        dbf.update(hvo)

        // try to update cluster os
        UpdateClusterOSAction action = new UpdateClusterOSAction()
        action.sessionId = adminSession()
        action.uuid = cls.uuid
        UpdateClusterOSAction.Result result = action.call()
        assert result.error != null: "API UpdateClusterOSAction fails with an error ${result.error}"

        // recovery
        hvo = dbf.reload(hvo)
        hvo.state = HostState.Enabled
        dbf.update(hvo)
    }

    void testUpdateClusterWithHostMaintenance() {
        ClusterInventory cls = env.inventoryByName("cluster1") as ClusterInventory
        HostInventory kvm1 = env.inventoryByName("kvm1") as HostInventory
        HostInventory kvm2 = env.inventoryByName("kvm2") as HostInventory

        // set kvm1 maintenance
        HostVO hvo1 = dbFindByUuid(kvm1.uuid, HostVO.class)
        assert hvo1.state == HostState.Enabled

        changeHostState {
            uuid = kvm1.uuid
            stateEvent = "maintain"
        }

        hvo1 = dbFindByUuid(kvm1.uuid, HostVO.class)
        assert hvo1.state == HostState.Maintenance

        // try to update cluster os
        APIUpdateClusterOSMsg msg = new APIUpdateClusterOSMsg()
        msg.setUuid(cls.uuid)

        LongJobInventory jobInv = submitLongJob {
            jobName = "APIUpdateClusterOSMsg"
            jobData = gson.toJson(msg)
        } as LongJobInventory

        assert jobInv.getJobName() == "APIUpdateClusterOSMsg"

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        // kvm1 still in maintenance state
        hvo1 = dbFindByUuid(kvm1.uuid, HostVO.class)
        assert hvo1.state == HostState.Maintenance

        // kvm2 still in enabled state
        HostVO hvo2 = dbFindByUuid(kvm2.uuid, HostVO.class)
        assert hvo2.state == HostState.Enabled

        // vm1 still running
        VmInstanceInventory vmInv = env.inventoryByName("vm1") as VmInstanceInventory
        VmInstanceVO vmVO = dbFindByUuid(vmInv.uuid, VmInstanceVO.class)
        assert vmVO.state == VmInstanceState.Running
    }

    void testUpdateClusterUsingAPI() {
        ClusterInventory cls = env.inventoryByName("cluster1") as ClusterInventory
        HostInventory kvm2 = env.inventoryByName("kvm2") as HostInventory
        VmInstanceInventory vmInv = env.inventoryByName("vm1") as VmInstanceInventory

        // check kvm2 state before update cluster os
        HostVO hvo2 = dbFindByUuid(kvm2.uuid, HostVO.class)
        assert hvo2.state == HostState.Enabled

        // try to update cluster os
        LongJobInventory jobInv = updateClusterOS {
            uuid = cls.uuid
        }

        assert jobInv.getJobName() == "APIUpdateClusterOSMsg"
        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        // kvm2 is back to enabled state
        hvo2 = dbFindByUuid(kvm2.uuid, HostVO.class)
        assert hvo2.state == HostState.Enabled

        // vm1 still running
        VmInstanceVO vmVO = dbFindByUuid(vmInv.uuid, VmInstanceVO.class)
        assert vmVO.state == VmInstanceState.Running
    }

    void testUpdateClusterExcludePackages() {
        ClusterInventory cls = env.inventoryByName("cluster1") as ClusterInventory

        // try to update cluster os
        LongJobInventory jobInv = updateClusterOS {
            uuid = cls.uuid
            excludePackages = ["kernel", "systemd*"]
        }

        assert jobInv.getJobName() == "APIUpdateClusterOSMsg"
        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }
    }

    void testUpdateClusterParallelismDegree() {
        ClusterInventory cls = env.inventoryByName("cluster1") as ClusterInventory

        def cmdList = Collections.synchronizedList(new ArrayList())
        def syncSignal = "update-host-os-of-cluster-" + cls.uuid

        def checkChain = false
        env.afterSimulator(KVMConstant.KVM_UPDATE_HOST_OS_PATH) { rsp, HttpEntity<String> e ->
            while (true) {
                if (checkChain) {
                    break
                }

                ChainTaskStatistic updateOsChainTask = thdf.getChainTaskStatistics().get(syncSignal)
                if (updateOsChainTask.currentRunningThreadNum == 2 && updateOsChainTask.pendingTaskNum == 0) {
                    checkChain = true
                    break
                }
            }

            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.UpdateHostOSCmd.class)
            cmdList.add(cmd)

            return rsp
        }

        // try to update cluster os
        LongJobInventory jobInv = updateClusterOS {
            uuid = cls.uuid
        }

        retryInSecs {
            assert cmdList.size() <= 2
        }

    }
}
