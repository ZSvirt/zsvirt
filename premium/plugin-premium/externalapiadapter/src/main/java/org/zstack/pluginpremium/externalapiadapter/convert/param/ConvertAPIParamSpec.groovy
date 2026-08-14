package org.zstack.pluginpremium.externalapiadapter.convert.param


import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_API_APIID_KEY
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_SESSIONID_KEY

/**
 * Created by lining on 2018/4/15.
 */
class ConvertAPIParamSpec {
    final List<SimpleConvertSpec> simpleConvertSpecs = []
    final List<SystemTagConvertSpec> systemTagConvertSpecs = []
    final List<ComplexConvertSpec> complexConvertSpecs = []
    final List<ZStackNeedParamSpec> zStackNeedParamSpecs = []

    Closure beforeZstackAPIParam
    Closure afterZstackAPIParam

    void addCommonParamSpecs() {
        simpleConvert {
            ecsParamName = "scretykey"
            zstackParamName = "sessionUuid"
        }
    }

    SimpleConvertSpec simpleConvert(
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SimpleConvertSpec.class) Closure c) {
        def spec = new SimpleConvertSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        simpleConvertSpecs.add(spec)
        return spec
    }

    SimpleConvertSpec querySimpleConvert(
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = QuerySimpleConvertSpec.class) Closure c) {
        def spec = new QuerySimpleConvertSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        simpleConvertSpecs.add(spec)
        return spec
    }

    SystemTagConvertSpec systemTagConvert(
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SystemTagConvertSpec) Closure c) {
        def spec = new SystemTagConvertSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        systemTagConvertSpecs.add(spec)
        return spec
    }

    ComplexConvertSpec complexConvert(
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ComplexConvertSpec) Closure c) {
        def spec = new ComplexConvertSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        complexConvertSpecs.add(spec)
        return spec
    }

    ComplexConvertSpec queryComplexConvert(
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = QueryComplexConvertSpec.class) Closure c) {
        def spec = new QueryComplexConvertSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        complexConvertSpecs.add(spec)
        return spec
    }

    ZStackNeedParamSpec zstackNeedParam(
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ZStackNeedParamSpec) Closure c) {
        def spec = new ZStackNeedParamSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        zStackNeedParamSpecs.add(spec)
        return spec
    }

    Map getZstackAPIParam(Map ecsAPIPraram, String sessionId, String requestId) {
        Map result = [:]
        result.put(ZSTACK_SESSIONID_KEY, sessionId)
        result.put(ZSTACK_API_APIID_KEY, requestId)

        if (beforeZstackAPIParam != null) {
            beforeZstackAPIParam(result)
        }

        for (SimpleConvertSpec c : simpleConvertSpecs) {
            c.convertAndSetResult(ecsAPIPraram, result)
        }

        for (SystemTagConvertSpec c : systemTagConvertSpecs) {
            c.convertAndSetResult(ecsAPIPraram, result)
        }

        for (ComplexConvertSpec c : complexConvertSpecs) {
            c.convertAndSetResult(ecsAPIPraram, result)
        }

        for (ZStackNeedParamSpec c : zStackNeedParamSpecs) {
            c.convertAndSetResult(ecsAPIPraram, result)
        }

        if (afterZstackAPIParam != null) {
            afterZstackAPIParam(result)
        }

        return result
    }

}
