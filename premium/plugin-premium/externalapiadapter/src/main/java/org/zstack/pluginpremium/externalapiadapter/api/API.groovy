package org.zstack.pluginpremium.externalapiadapter.api

/**
 * Created by lining on 2018/4/20.
 */
interface API {
    Class getZStackAction()

    Object callZStackAction()

    String call(Map ecsAPIParamMap)

    void setSessionId(String sessionId)

    void setRequestId(String requestId)
}