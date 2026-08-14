package org.zstack.pluginpremium.externalapiadapter.convert.response

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.pluginpremium.externalapiadapter.exception.APIResponseConvertException
import org.zstack.sdk.ApiException
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_API_REQUESTID_KEY
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_API_APIID_KEY

/**
 * Created by lining on 2018/4/15.
 */
class ConvertAPIResponseSpec {
    final CLogger logger = Utils.getLogger(ConvertAPIResponseSpec.class)
    final List<ConvertResponseAttributeSpec> convertResponseAttributes = []

    Map zstackAPIReq
    def zstackAPIRsp
    Map ecsAPIReq

    ConvertAPIResponseSpec() {
        convertResponseAttribute {
            ecsAttributeName = ECS_API_REQUESTID_KEY

            addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                ecsAPIRsp.put(ecsAttributeName, zstackAPIReq.get(ZSTACK_API_APIID_KEY))
            }
        }
    }

    ConvertResponseAttributeSpec convertResponseAttribute(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ConvertResponseAttributeSpec) Closure c) {
        def spec = new ConvertResponseAttributeSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        convertResponseAttributes.add(spec)
        return spec
    }

    private void before(ConvertResponseAttributeSpec convert) {
        convert.before()
        for (ConvertResponseAttributeSpec c : convert.attributeSpecs) {
            before(c)
        }
    }

    private void before() {
        for (ConvertResponseAttributeSpec c : convertResponseAttributes) {
            before(c)
        }
    }

    private void makeZstackAttributeValue() {
        for (ConvertResponseAttributeSpec c : convertResponseAttributes) {
            makeZstackAttributeValue(c)
        }
    }

    private void makeZstackAttributeValue(ConvertResponseAttributeSpec responseAttributeSpec) {
        responseAttributeSpec.makeZstackAttributeValue()

        for (ConvertResponseAttributeSpec c : responseAttributeSpec.attributeSpecs) {
            makeZstackAttributeValue(c)
        }
    }

    private void addEcsValueToFather() {
        for (ConvertResponseAttributeSpec c : convertResponseAttributes) {
            recursionAddEcsValueToFather(c)
        }
    }

    private void recursionAddEcsValueToFather(ConvertResponseAttributeSpec c) {
        def fatherValue = c.ecsAttributeValue
        for (ConvertResponseAttributeSpec child : c.attributeSpecs) {
            if (child.addEcsValueToFather != null) {
                try {
                    child.addEcsValueToFather(fatherValue)
                } catch (ApiException e) {
                    throw new APIResponseConvertException(child.ecsAttributeName, e.message)
                } catch (Exception e) {
                    logger.warn("${zstackAPIReq.get(ExternalAPIAdapterConstants.ZSTACK_API_APIID_KEY)} ${e.getMessage()}".toString(), e)
                    // todo remove it
                    e.printStackTrace()
                    throw new APIResponseConvertException(child.ecsAttributeName, "Internal error occurred")
                }
            }
            recursionAddEcsValueToFather(child)
        }
    }

    private void makeEcsAPIRsp(Map ecsAPIRsp) {
        for (ConvertResponseAttributeSpec c : convertResponseAttributes) {
            if (c.addEcsValueToEcsAPIRsp != null) {
                c.addEcsValueToEcsAPIRsp(ecsAPIRsp)
            }
        }
    }

    Map getEcsAPIRsp(Map ecsAPIReq, Map zstackAPIReq, def zstackAPIRsp) {
        Map ecsAPIRsp = new HashMap()

        this.zstackAPIReq = zstackAPIReq
        this.zstackAPIRsp = zstackAPIRsp
        this.ecsAPIReq = ecsAPIReq

        // 让每个节点获取对应的zstack rsp
        this.makeZstackAttributeValue()

        // 处理List, 更新转换关系
        this.before()

        // 让每个子节点向父节点汇报 自己的值
        this.addEcsValueToFather()

        // 生成最终结果
        this.makeEcsAPIRsp(ecsAPIRsp)

        return ecsAPIRsp
    }

}
