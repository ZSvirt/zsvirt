package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.SimulatorGlobalProperty
import org.zstack.storage.zbs.ZbsGlobalProperty
import org.zstack.storage.zbs.ZbsPrimaryStorageMdsBase

/**
 * @author Xingwei Yu
 * @date 2024/4/19 下午4:59
 */
class ZbsPrimaryStorageAgent extends Agent {
    ZbsPrimaryStorageAgent(Simulator simulator) {
        super(simulator)
        ZbsGlobalProperty.PRIMARY_STORAGE_AGENT_PORT = SimulatorGlobalProperty.SIMULATOR_AGENT_PORT
    }

    @Override
    void setupAgentHandler() {
        handle(ZbsPrimaryStorageMdsBase.ECHO_PATH) { HttpEntity<String> entity ->
            return [:]
        }
    }
}
