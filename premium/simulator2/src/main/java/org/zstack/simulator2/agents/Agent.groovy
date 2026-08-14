package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.simulator2.Simulator
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

/**
 * Created by xing5 on 2017/9/16.
 */
abstract class Agent {
    static CLogger logger = Utils.getLogger(Agent.class)

    Simulator simulator

    Agent(Simulator simulator) {
        this.simulator = simulator
    }

    abstract void setupAgentHandler()

    protected void handle(String path, Closure c) {
        logger.info("simulator2 Agent handle path: " + path)
        simulator.installAgentHandler(path, c)
    }

    static <T> T json(String str, Class<T> type) {
        return JSONObjectUtil.toObject(str, type)
    }

    static <T> T json(HttpEntity<String> e, Class<T> type) {
        return JSONObjectUtil.toObject(e.body, type)
    }
}
