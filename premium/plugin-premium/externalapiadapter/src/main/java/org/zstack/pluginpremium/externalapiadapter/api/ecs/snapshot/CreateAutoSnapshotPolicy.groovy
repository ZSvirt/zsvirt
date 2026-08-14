package org.zstack.pluginpremium.externalapiadapter.api.ecs.snapshot


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.CreateSchedulerTriggerAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_NAME

/**
 * @Author: fubang* @Date: 2018/5/1
 */
class CreateAutoSnapshotPolicy extends BaseAPI {

    @Override
    Class getZStackAction() {
        return CreateSchedulerTriggerAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "TimePoints"
                    ecsParamType = ArrayList.class
                    zstackParamName = "startTime"
                    putZstackParamValue = {Map map, List value ->
                        int time = Integer.parseInt(value.get(0) as String)
                        Calendar calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, time)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        map.put(zstackParamName, calendar.getTimeInMillis() / 1000)
                    }
                }

                simpleConvert {
                    ecsParamName = "AutoSnapshotPolicyName"
                    zstackParamName = ZSTACK_NAME
                    stillConvertParamWhenEcsParamValueIsNull = true
                    putZstackParamValue = { map, value ->
                        map.put(zstackParamName, value ?: "untitled")
                    }
                }

                zstackNeedParam {
                    zstackParamName = "schedulerType"
                    getZstackValue = { ecsParamMap, zstackParamMap ->
                        return "simple"
                    }
                }

                zstackNeedParam {
                    zstackParamName = "schedulerInterval"
                    getZstackValue = { ecsParamMap, zstackParamMap ->
                        return 24 * 60 * 60
                    }
                }

            }
            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "AutoSnapshotPolicyId"
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAPIRsp.value.inventory.uuid)
                    }
                }
            }
        }
    }
}
