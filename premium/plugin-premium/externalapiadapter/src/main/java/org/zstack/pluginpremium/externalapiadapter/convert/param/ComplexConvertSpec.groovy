package org.zstack.pluginpremium.externalapiadapter.convert.param

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.ApiException
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

/**
 * Created by lining on 2018/4/15.
 */
class ComplexConvertSpec extends ParameterConversion{
    final CLogger logger = Utils.getLogger(ComplexConvertSpec.class)
    Closure getZstackValue
    String alterEcsParamName
    Class alterEcsParamType = String.class

    @Override
    void convertAndSetResult(Map ecsParamMap, Map zstackParamMap) {
        def ecsParamValue = ecsParamMap.get(ecsParamName)

        if (ecsParamValue == null && alterEcsParamName != null) {
            ecsParamValue = ecsParamMap.get(alterEcsParamName)
        }

        if (ecsParamValue == null && !stillConvertParamWhenEcsParamValueIsNull) {
            return
        }

        assert null != getZstackValue

        try {
            def zstackParamValue
            if (getZstackValue.getMaximumNumberOfParameters() == 2) {
                zstackParamValue = getZstackValue(ecsParamMap, ecsParamValue)
            } else {
                zstackParamValue = getZstackValue(ecsParamValue)
            }
            if (putZstackParamValue == null) {
                zstackParamMap.put(zstackParamName, zstackParamValue)
            } else {
                putZstackParamValue(zstackParamMap, zstackParamValue)
            }
        } catch (ApiException e) {
            throw new APIParamConvertException(this.ecsParamName, e.message)
        } catch (APIParamConvertException e) {
            throw e
        } catch (APIAdapterSpecifiedErrorException e) {
            throw e
        } catch (Exception e) {
            logger.warn("${zstackParamMap.get(ExternalAPIAdapterConstants.ZSTACK_API_APIID_KEY)} ${e.getMessage()}".toString(), e)
            throw new APIParamConvertException(this.ecsParamName, "Internal error occurred")
        }
    }
}
