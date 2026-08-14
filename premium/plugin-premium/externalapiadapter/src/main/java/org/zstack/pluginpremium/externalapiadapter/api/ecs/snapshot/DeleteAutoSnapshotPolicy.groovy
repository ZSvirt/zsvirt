package org.zstack.pluginpremium.externalapiadapter.api.ecs.snapshot


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DeleteSchedulerTriggerAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * @Author: fubang* @Date: 2018/5/1
 */
class DeleteAutoSnapshotPolicy extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "AutoSnapshotPolicyId"
                    zstackParamName = ZSTACK_UUID
                }
            }
            convertAPIResponse {

            }
        }
    }

    @Override
    Class getZStackAction() {
        return DeleteSchedulerTriggerAction.class
    }
}
