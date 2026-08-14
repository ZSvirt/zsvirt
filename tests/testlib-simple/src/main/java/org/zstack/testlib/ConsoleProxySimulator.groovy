package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.header.console.ConsoleConstants
import org.zstack.header.console.ConsoleProxyCommands
import org.zstack.utils.gson.JSONObjectUtil

class ConsoleProxySimulator implements Simulator {
    @Override
    void registerSimulators(EnvSpec xspec) {
        def simulator = { arg1, arg2 ->
            xspec.simulator(arg1, arg2)
        }

        simulator(ConsoleConstants.CONSOLE_PROXY_ESTABLISH_PROXY_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ConsoleProxyCommands.EstablishProxyCmd.class)
            def rsp = new ConsoleProxyCommands.EstablishProxyRsp()
            return rsp
        }

        simulator(ConsoleConstants.CONSOLE_PROXY_CHECK_PROXY_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ConsoleProxyCommands.CheckAvailabilityCmd.class)
            def rsp = new ConsoleProxyCommands.CheckAvailabilityRsp()
            return rsp
        }

        simulator(ConsoleConstants.CONSOLE_PROXY_DELETE_PROXY_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ConsoleProxyCommands.DeleteProxyCmd.class)
            def rsp = new ConsoleProxyCommands.DeleteProxyRsp()
            return rsp
        }

        simulator(ConsoleConstants.CONSOLE_PROXY_PING_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, ConsoleProxyCommands.PingCmd.class)
            def rsp = new ConsoleProxyCommands.PingRsp()
            return rsp
        }
    }
}
