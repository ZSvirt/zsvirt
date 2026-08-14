package org.zstack.pluginpremium.externalapiadapter.convert.response;

/**
 * Created by lining on 2018/4/16.
 */
class ListElementSpec extends ConvertResponseAttributeSpec {

    ConvertResponseAttributeSpec convertResponseAttribute(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ConvertResponseAttributeSpec) Closure c) {
        def spec = new ConvertResponseAttributeSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        attributeSpecs.add(spec)
        return spec
    }
    
}
