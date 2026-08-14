package org.zstack.pluginpremium.externalapiadapter.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants
import org.zstack.pluginpremium.externalapiadapter.convert.ECSAPISpec
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.sdk.AbstractAction
import org.zstack.sdk.ErrorCode
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

/**
 * Created by lining on 2018/4/20.
 */
abstract class BaseAPI implements API {
    final CLogger logger = Utils.getLogger(this.getClass())

    ECSAPISpec spec
    String sessionId
    String requestId

    Map ecsAPIParamMap
    Map zstackAPIParamMap
    def zstackAPIRsp

    BaseAPI() {
        this.configAPIConversionSpec()
    }

    protected ECSAPISpec config(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ECSAPISpec.class) Closure c) {
        def spec = new ECSAPISpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return spec
    }

    @Override
    Object callZStackAction() {
        Gson gson = new GsonBuilder().create()
        AbstractAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), this.getZStackAction())
        def result = action.call()
        handleActionResult(result.error, ExternalAPIAdapterConstants.ECSErrorCode.ACTION_FAILED, null)

        this.afterCallZStackAction(result)

        return result
    }

    @Override
    String call(Map ecsAPIParamMap) {
        this.ecsAPIParamMap = ecsAPIParamMap

        try {
            this.zstackAPIParamMap = getZstackAPIParam(ecsAPIParamMap)
            logger.debug("[requestId:${this.zstackAPIParamMap[ExternalAPIAdapterConstants.ZSTACK_API_APIID_KEY]}, API:${this.getClass().simpleName}] Convert into zstack api params: ${this.zstackAPIParamMap}")

            this.zstackAPIRsp = callZStackAction()

            Map ecsAPIRsp = this.getEcsAPIRsp(zstackAPIRsp)
            this.afterGetEcsApiRsp(ecsAPIRsp)
            String ecsResultRspJson = JSONObjectUtil.toJsonString(ecsAPIRsp)
            return ecsResultRspJson
        } catch (Exception e) {
            def exception = convertErrorCode(e)
            throw exception ?: e
        }
    }

    @Override
    void setSessionId(String sessionId) {
        this.sessionId = sessionId
    }

    @Override
    void setRequestId(String requestId) {
        this.requestId = requestId
    }

    protected abstract void configAPIConversionSpec()

    Map getZstackAPIParam(Map ecsAPIParam) {
        this.setEcsAPIParamDefaultValue(ecsAPIParam)
        return spec.convertAPIParamSpec.getZstackAPIParam(ecsAPIParam, this.sessionId, this.requestId)
    }

    Map getEcsAPIRsp(def zstackAPIRsp) {
        return spec.convertAPIResponseSpec.getEcsAPIRsp(this.ecsAPIParamMap, this.zstackAPIParamMap, zstackAPIRsp)
    }

    void setEcsAPIParamDefaultValue(Map ecsAPIParamMap) {
        // do nothing
    }

    void afterCallZStackAction(def zstackActionResult) {
        // do nothing
    }

    void afterGetEcsApiRsp(Map ecsApiRsp) {
        // do nothing
    }

    void handleActionResult(ErrorCode e, String code, String message) {
        if (e == null) {
            return
        }
        throw new APIAdapterSpecifiedErrorException(code, message ?: e.description)
    }

    Exception convertErrorCode(Exception e) {
        if (e instanceof APIAdapterSpecifiedErrorException
                && e.code == ExternalAPIAdapterConstants.ECSErrorCode.ACTION_FAILED) {
            return handleZStackActionFailed(e)
        } else {
            return handleOtherException(e)
        }
    }

    /*
     * this default implementation is to keep the same result with previews version
     * override this method to handle the "main" api action errors
     */
    Exception handleZStackActionFailed(Exception e) {
        return new APIAdapterSpecifiedErrorException(
                ExternalAPIAdapterConstants.ECSErrorCode.InternalError,
                e.message
        )
    }

    /*
     * override this method to handle exceptions when converting params
     */
    Exception handleOtherException(Exception e) {
        null
    }
}
