package org.zstack.test.integration.kvm.vm

import org.zstack.header.host.HostVO
import org.zstack.sdk.GetVmConsoleAddressResult
import org.zstack.sdk.GetVmConsolePasswordAction
import org.zstack.sdk.SetVmConsolePasswordAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
/**
 * Created by Camile on 2017/3/13.
 *
 * base on BAT test : TestVmConsolePasswordOnKvm
 */
class VmConsoleCase extends SubCase {
    EnvSpec env

    def DOC = """

"""

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testSetConsolePasswordSuccess()
            testClearConsolePasswordSuccess()
            testGetVmConsoleAddressSuccess()
        }
    }

    void testGetVmConsoleAddressSuccess() {
        VmInstanceInventory inv = env.inventoryByName("vm")

        GetVmConsoleAddressResult res = getVmConsoleAddress {
            uuid = inv.uuid
        }

        assert res != null
        assert res.hostIp == dbFindByUuid(inv.hostUuid, HostVO.class).getManagementIp()
    }

    void testSetConsolePasswordSuccess() {
        VmSpec spec = env.specByName("vm")
        def action = new SetVmConsolePasswordAction()
        action.uuid = spec.inventory.uuid
        action.consolePassword = "password2"
        action.sessionId = adminSession()
        SetVmConsolePasswordAction.Result res = action.call()
        assert res.error != null

    }

    void testClearConsolePasswordSuccess() {
        def password = "123456789"
        VmSpec spec = env.specByName("vm")
        VmInstanceInventory inv = setVmConsolePassword {
            uuid = spec.inventory.uuid
            consolePassword = password
            sessionId = adminSession()
        }
        def action  = new GetVmConsolePasswordAction()
        action.uuid = spec.inventory.uuid
        action.sessionId = adminSession()
        GetVmConsolePasswordAction.Result res = action.call()
        assert res.value.getPassword() == password

        deleteVmConsolePassword {
            uuid = spec.inventory.uuid
        }

        res = action.call()
        assert res.value.getPassword() == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
