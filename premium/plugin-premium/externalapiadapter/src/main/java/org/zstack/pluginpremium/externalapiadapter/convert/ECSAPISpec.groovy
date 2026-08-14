package org.zstack.pluginpremium.externalapiadapter.convert

import org.zstack.pluginpremium.externalapiadapter.convert.param.ConvertAPIParamSpec
import org.zstack.pluginpremium.externalapiadapter.convert.response.ConvertAPIResponseSpec

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_API_REQUESTID_KEY
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_QUERY_API_PAGENUMBER_KEY
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_QUERY_API_PAGESIZE_KEY
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_TOTAL_COUNT
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_API_APIID_KEY
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_QUERY_LIMIT_KEY

/**
 * Created by lining on 2018/4/15.
 */
class ECSAPISpec {
    ConvertAPIParamSpec convertAPIParamSpec
    ConvertAPIResponseSpec convertAPIResponseSpec

    ConvertAPIParamSpec convertAPIParam(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ConvertAPIParamSpec.class) Closure c) {
        def spec = new ConvertAPIParamSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        convertAPIParamSpec = spec
        spec.addCommonParamSpecs()
        return spec
    }

    ConvertAPIResponseSpec convertAPIResponse(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ConvertAPIResponseSpec.class) Closure c) {
        def spec = new ConvertAPIResponseSpec()

        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        convertAPIResponseSpec = spec
        return spec
    }

    ConvertAPIParamSpec convertQueryAPIParam(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ConvertAPIParamSpec.class) Closure c) {
        def spec = new ConvertAPIParamSpec()

        spec.simpleConvert {
            ecsParamName = ECS_QUERY_API_PAGESIZE_KEY
            ecsParamType = Integer.class
            zstackParamName = ZSTACK_QUERY_LIMIT_KEY
            zstackParamType = Integer.class
        }

        spec.simpleConvert {
            ecsParamName = ECS_QUERY_API_PAGENUMBER_KEY
            ecsParamType = Integer.class
            zstackParamName = "start"
            zstackParamType = Integer.class

            putZstackParamValue = { zstackParamMap, ecsParamValue ->
                ecsParamValue -= 1
                ecsParamValue = ecsParamValue * zstackParamMap.get(ZSTACK_QUERY_LIMIT_KEY)
                zstackParamMap.put(zstackParamName, ecsParamValue)
            }
        }

        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        convertAPIParamSpec = spec
        spec.addCommonParamSpecs()
        return spec
    }

    ConvertAPIResponseSpec convertQueryAPIResponse(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ConvertAPIResponseSpec.class) Closure c) {
        def spec = new ConvertAPIResponseSpec()

        spec.convertResponseAttribute {
            ecsAttributeName = ECS_TOTAL_COUNT

            getZstackAttributeValue = {
                return convertAPIResponseSpec.zstackAPIRsp.value.total
            }

            addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
            }
        }

        spec.convertResponseAttribute {
            ecsAttributeName = ECS_QUERY_API_PAGENUMBER_KEY

            getZstackAttributeValue = {
                return Integer.parseInt(convertAPIResponseSpec.ecsAPIReq.get(ecsAttributeName))
            }

            addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
            }
        }

        spec.convertResponseAttribute {
            ecsAttributeName = ECS_QUERY_API_PAGESIZE_KEY

            getZstackAttributeValue = {
                return Integer.parseInt(convertAPIResponseSpec.ecsAPIReq.get(ecsAttributeName))
            }

            addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
            }
        }

        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        convertAPIResponseSpec = spec
        return spec
    }

}
