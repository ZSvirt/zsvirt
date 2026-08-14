package org.zstack.pluginpremium.externalapiadapter.convert.param

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants

/**
 * Created by Qi Le on 2019-06-15
 */
class QueryComplexConvertSpec extends ComplexConvertSpec {
    QueryComplexConvertSpec() {
        putZstackParamValue = { zstackParamMap, zstackParamValue ->
            List conditions = zstackParamMap.get(ExternalAPIAdapterConstants.ZSTACK_QUERY_CONDITIONS_KEY)
            conditions.add(zstackParamName + "=" + zstackParamValue)
        }
    }
}
