package org.zstack.pluginpremium.externalapiadapter.convert.response

/**
 * Created by lining on 2018/4/15.
 */
class ConvertResponseAttributeSpec {

    String ecsAttributeName
    Closure setEcsAttributeValue
    def ecsAttributeValue
    String zstackAttributeName
    Closure getZstackAttributeValue
    def zstackAttributeValue
    Closure addEcsValueToFather
    Closure addEcsValueToEcsAPIRsp

    List<ConvertResponseAttributeSpec> attributeSpecs = []

    ConvertResponseAttributeSpec convertResponseAttribute(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ConvertResponseAttributeSpec) Closure c) {
        def spec = new ConvertResponseAttributeSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        attributeSpecs.add(spec)
        return spec
    }

    ListSpec convertList(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ListSpec) Closure c) {
        def spec = new ListSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        attributeSpecs.add(spec)
        return spec
    }

    void makeZstackAttributeValue() {
        if (zstackAttributeValue != null) {
            return
        }

        if (getZstackAttributeValue == null) {
            return
        }

        zstackAttributeValue = getZstackAttributeValue.call()
    }

    void before() {

    }
}
