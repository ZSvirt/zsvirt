package org.zstack.test.integration.kvm.tpm

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.zstack.header.Constants
import org.zstack.header.secret.SecretHostGetReply
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.KVMSimulator
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class HostSecretKvmAgentSimulatorCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.12"
                        username = "root"
                        password = "password"
                    }
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testSimulatedGetSecretErrorMatchesMnConstant()
            testEnsureThenGetReturnsSameUuid()
            testDeleteSecretRemovesCacheEntry()
        }
    }

    void testSimulatedGetSecretErrorMatchesMnConstant() {
        KVMSimulator.resetSimulatedHostSecretCache()
        def hostUuid = "sim-host-1"
        def cmd = new KVMAgentCommands.SecretHostGetCmd()
        cmd.vmUuid = "sim-vm-1"
        cmd.purpose = "vtpm"
        cmd.keyVersion = 1
        cmd.usageInstance = KVMConstant.HOST_SECRET_USAGE_INSTANCE_VTPM
        def headers = new HttpHeaders()
        headers.add(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID, hostUuid)
        def entity = new HttpEntity<String>(JSONObjectUtil.toJsonString(cmd), headers)

        def sim = env.getSimulator(KVMConstant.KVM_GET_SECRET_PATH)
        assert sim != null
        def rsp = sim(entity) as KVMAgentCommands.SecretHostGetResponse
        assert !rsp.success
        assert SecretHostGetReply.ERROR_CODE_SECRET_NOT_FOUND == rsp.error
    }

    void testEnsureThenGetReturnsSameUuid() {
        KVMSimulator.resetSimulatedHostSecretCache()
        def hostUuid = "sim-host-2"
        def vmUuid = "sim-vm-2"
        def purpose = "vtpm"
        def keyVersion = 2

        def ensureCmd = new KVMAgentCommands.SecretHostDefineCmd()
        ensureCmd.vmUuid = vmUuid
        ensureCmd.purpose = purpose
        ensureCmd.keyVersion = keyVersion
        ensureCmd.encryptedDek = "ZHVtbXk="
        ensureCmd.description = ""
        ensureCmd.usageInstance = KVMConstant.HOST_SECRET_USAGE_INSTANCE_VTPM

        def headers = new HttpHeaders()
        headers.add(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID, hostUuid)
        def ensureEntity = new HttpEntity<String>(JSONObjectUtil.toJsonString(ensureCmd), headers)
        def ensureSim = env.getSimulator(KVMConstant.KVM_ENSURE_SECRET_PATH)
        def ensureRsp = ensureSim(ensureEntity) as KVMAgentCommands.SecretHostDefineResponse
        assert ensureRsp.success
        assert ensureRsp.secretUuid != null

        def getCmd = new KVMAgentCommands.SecretHostGetCmd()
        getCmd.vmUuid = vmUuid
        getCmd.purpose = purpose
        getCmd.keyVersion = keyVersion
        getCmd.usageInstance = KVMConstant.HOST_SECRET_USAGE_INSTANCE_VTPM
        def getEntity = new HttpEntity<String>(JSONObjectUtil.toJsonString(getCmd), headers)
        def getSim = env.getSimulator(KVMConstant.KVM_GET_SECRET_PATH)
        def getRsp = getSim(getEntity) as KVMAgentCommands.SecretHostGetResponse
        assert getRsp.success
        assert ensureRsp.secretUuid == getRsp.secretUuid
    }

    void testDeleteSecretRemovesCacheEntry() {
        KVMSimulator.resetSimulatedHostSecretCache()
        def hostUuid = "sim-host-3"
        def vmUuid = "sim-vm-3"
        def purpose = "vtpm"
        def keyVersion = 3

        KVMSimulator.putSimulatedHostSecretForTest(hostUuid, vmUuid, purpose, keyVersion, "to-be-removed")

        def delCmd = new KVMAgentCommands.SecretHostDeleteCmd()
        delCmd.vmUuid = vmUuid
        delCmd.purpose = purpose
        delCmd.keyVersion = keyVersion
        delCmd.usageInstance = KVMConstant.HOST_SECRET_USAGE_INSTANCE_VTPM
        def headers = new HttpHeaders()
        headers.add(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID, hostUuid)
        def delEntity = new HttpEntity<String>(JSONObjectUtil.toJsonString(delCmd), headers)
        def delSim = env.getSimulator(KVMConstant.KVM_DELETE_SECRET_PATH)
        assert delSim != null
        def delRsp = delSim(delEntity) as KVMAgentCommands.SecretHostDeleteResponse
        assert delRsp.success

        assert KVMSimulator.getSimulatedHostSecretForTest(hostUuid, vmUuid, purpose, keyVersion) == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
