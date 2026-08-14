package org.zstack.pluginpremium.externalapiadapter.convert.param

/**
 * Created by lining on 2018/4/17.
 */
abstract class ParameterConversion {
    String ecsParamName
    Class ecsParamType = String.class

    String zstackParamName
    Class zstackParamType = String.class

    Closure putZstackParamValue

    abstract void convertAndSetResult(Map ecsParamMap, Map zstackParamMap)

    boolean stillConvertParamWhenEcsParamValueIsNull = false
}