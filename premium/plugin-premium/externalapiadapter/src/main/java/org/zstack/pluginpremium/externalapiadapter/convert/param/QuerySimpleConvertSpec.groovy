package org.zstack.pluginpremium.externalapiadapter.convert.param

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants

/**
 * Created by lining on 2018/4/15.
 */
class QuerySimpleConvertSpec extends SimpleConvertSpec {

    QuerySimpleConvertSpec() {
        putZstackParamValue = { zstackParamMap, zstackParamValue ->
            List conditions = zstackParamMap.get(ExternalAPIAdapterConstants.ZSTACK_QUERY_CONDITIONS_KEY)
            conditions.add(zstackParamName + "=" + zstackParamValue)
        }
    }

}
