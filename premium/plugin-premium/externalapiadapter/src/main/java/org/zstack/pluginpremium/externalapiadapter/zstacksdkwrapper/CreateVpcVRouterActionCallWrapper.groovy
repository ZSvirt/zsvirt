package org.zstack.pluginpremium.externalapiadapter.zstacksdkwrapper

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants
import org.zstack.sdk.ApiResult
import org.zstack.sdk.Completion
import org.zstack.sdk.CreateVpcVRouterAction
import org.zstack.sdk.CreateVpcVRouterResult
import org.zstack.sdk.ErrorCode
import org.zstack.sdk.InternalCompletion
import org.zstack.sdk.QueryApplianceVmAction
import org.zstack.sdk.ZSClient

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.QUERY_INTERVAL_TIME
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_ASYNC_QUERY_COUNT

/**
 * Created by lining on 2018/6/19.
 */
class CreateVpcVRouterActionCallWrapper extends CreateVpcVRouterAction {
    private static AtomicInteger completeFlag = new AtomicInteger(0)

    CreateVpcVRouterAction.Result call2(final Completion<CreateVpcVRouterAction.Result> callback) {
        CreateVpcVRouterAction.Result result

        this.call(new Completion<CreateVpcVRouterAction.Result>() {
            @Override
            void complete(CreateVpcVRouterAction.Result ret) {
                result = ret
                result.throwExceptionIfError()
                callback.complete(ret)
            }
        })

        int count = ZSTACK_ASYNC_QUERY_COUNT
        while (count >= 0) {
            count -= QUERY_INTERVAL_TIME

            if (result != null) {
                return result
            }

            QueryApplianceVmAction query = new QueryApplianceVmAction(
                    sessionId: sessionId,
                    conditions: ["uuid=$resourceUuid".toString()]
            )
            QueryApplianceVmAction.Result queryResult = query.call()
            if (queryResult.value.inventories.size() == 1) {
                return new CreateVpcVRouterAction.Result(
                        value: new CreateVpcVRouterResult()
                )
            }

            TimeUnit.SECONDS.sleep(QUERY_INTERVAL_TIME)
        }

        result = new CreateVpcVRouterAction.Result(
                error: new ErrorCode(
                        code: ExternalAPIAdapterConstants.ECSErrorCode.InternalError,
                        details : "Internal error occurred, Cannot get api operation result"
                )
        )
        return result
    }
}
