package org.zstack.pluginpremium.externalapiadapter.convert.response


/**
 * Created by lining on 2018/4/16.
 */
class ListSpec extends ConvertResponseAttributeSpec{
    ListElementSpec elementSpec
    Closure addListElement
    def getElementZstackValues

    ListElementSpec element(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ListElementSpec) Closure c) {
        def spec = new ListElementSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        elementSpec = spec
        return spec
    }

    ConvertResponseAttributeSpec addConvertResponseAttribute(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ConvertResponseAttributeSpec) Closure c) {
        def spec = new ConvertResponseAttributeSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        attributeSpecs.add(spec)
        return spec
    }

    @Override
    void before() {
        List elementZstackValues = getZstackAttributeValue()

        if (elementZstackValues == null) {
            return
        }

        for (def elementZstackValue : elementZstackValues) {
            this.addListElement(elementZstackValue)
        }
    }
}
