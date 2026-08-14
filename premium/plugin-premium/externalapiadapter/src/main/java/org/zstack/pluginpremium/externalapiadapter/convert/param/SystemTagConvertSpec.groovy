package org.zstack.pluginpremium.externalapiadapter.convert.param


import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_SYSTEMTAGS

/**
 * Created by lining on 2018/4/15.
 */
class SystemTagConvertSpec extends ParameterConversion {
    Closure getTag
    final String zstackParamName = ZSTACK_SYSTEMTAGS

    @Override
    void convertAndSetResult(Map ecsParamMap, Map zstackParamMap) {
        def ecsParamValue = null

        if (ecsParamName != null) {
            ecsParamValue = ecsParamMap.get(ecsParamName)
        }

        if (ecsParamValue == null && !stillConvertParamWhenEcsParamValueIsNull) {
            return
        }

        assert putZstackParamValue == null

        List systemTags = zstackParamMap.get(zstackParamName)
        if (systemTags == null) {
            systemTags = []
            zstackParamMap.put(zstackParamName, systemTags)
        }
        def tag = getTag(ecsParamValue)

        if (tag != null) {
            systemTags.add(tag)
        }
    }
}
