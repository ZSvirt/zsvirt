package org.zstack.pluginpremium.externalapiadapter.convert.param

/**
 * Created by lining on 2018/4/15.
 */
class ZStackNeedParamSpec extends ParameterConversion {
    Closure getZstackValue

    @Override
    void convertAndSetResult(Map ecsParamMap, Map zstackParamMap) {
        assert null != getZstackValue

        def zstackParamValue = getZstackValue(ecsParamMap, zstackParamMap)

        if (putZstackParamValue == null) {
            zstackParamMap.put(zstackParamName, zstackParamValue)
        } else {
            putZstackParamValue(zstackParamMap, zstackParamValue)
        }
    }
}
