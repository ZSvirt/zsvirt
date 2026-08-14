package org.zstack.pluginpremium.externalapiadapter.convert.param

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils

/**
 * Created by lining on 2018/4/15.
 */
class SimpleConvertSpec extends ParameterConversion {

    @Override
    void convertAndSetResult(Map ecsParamMap, Map zstackParamMap) {
        def ecsParamValue = ecsParamMap.get(ecsParamName)

        if (ecsParamValue == null && !stillConvertParamWhenEcsParamValueIsNull) {
            return
        }

        if(ecsParamValue != null && ecsParamValue.class.getName() != ecsParamType.getName()) {
            ecsParamValue = ExternalAPIAdapterUtils.changeValueType(ecsParamValue, ecsParamType)
        }

        if (putZstackParamValue == null) {
            zstackParamMap.put(zstackParamName, ecsParamValue)
        } else {
            putZstackParamValue(zstackParamMap, ecsParamValue)
        }

    }

}
