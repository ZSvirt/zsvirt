package org.zstack.pluginpremium.externalapiadapter.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants
import org.zstack.sdk.AbstractAction
import org.zstack.sdk.Completion
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit
/**
 * Created by lining on 2018/4/27.
 */
abstract class BaseAsyncAPI<T> extends BaseAPI{

    @Override
    Object callZStackAction() {
        Gson gson = new GsonBuilder().create()
        AbstractAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), this.getZStackAction())

        def result
        action.call(new Completion<T>(){
            @Override
            void complete(T ret) {
                try {
                    result = ret
                    ret.throwExceptionIfError()
                    afterCallZStackAction(ret)
                } catch (Throwable t) {
                    logger.error("[RequestId:${requestId}] Async API has an internal error".toString(), t)
                    // todo remove it
                    t.printStackTrace()
                    throw t
                } finally {
                    finishAsyncCallZStackAction()
                }
            }
        })

        // Wait several seconds and try to get results
        int waitingTime = getAsyncWaitingTime()
        int checkInterval = getAsyncCheckInterval()
        long current = System.currentTimeMillis()
        long expiredTime = current + TimeUnit.SECONDS.toMillis(waitingTime)
        while (current < expiredTime) {

            if (result != null) {
                break
            }

            TimeUnit.SECONDS.sleep(checkInterval)
            current = System.currentTimeMillis()
        }

        if (result != null) {
            result.throwExceptionIfError()
        }

        return result
    }

    int getAsyncWaitingTime() {
        ExternalAPIAdapterConstants.ZSTACK_ASYNC_API_DEFAULT_TIMEOUT
    }

    int getAsyncCheckInterval() {
        ExternalAPIAdapterConstants.QUERY_INTERVAL_TIME
    }

    void finishAsyncCallZStackAction() {}
}
