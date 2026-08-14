package org.zstack.pluginpremium.externalapiadapter.api.ecs.snapshot


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.UpdateSchedulerTriggerAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_NAME
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * @Author: fubang* @Date: 2018/5/1
 */
class ModifyAutoSnapshotPolicyEx extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "AutoSnapshotPolicyId"
                    zstackParamName = ZSTACK_UUID
                }

                simpleConvert {
                    ecsParamName = "AutoSnapshotPolicyName"
                    zstackParamName = ZSTACK_NAME
                }

                simpleConvert {
                    ecsParamName = "TimePoints"
                    ecsParamType = ArrayList.class
                    zstackParamName = "startTime"
                    putZstackParamValue = { Map map, List value ->
                        int time = Integer.parseInt(value.get(0) as String)
                        Calendar calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, time)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        map.put(zstackParamName, calendar.getTimeInMillis() / 1000)
                    }
                }
            }
            convertAPIResponse {}
        }
    }

    @Override
    Class getZStackAction() {
        return UpdateSchedulerTriggerAction.class
    }
}
