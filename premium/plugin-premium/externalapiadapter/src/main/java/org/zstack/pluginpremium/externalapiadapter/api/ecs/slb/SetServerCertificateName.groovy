package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.UpdateCertificateAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_NAME
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * Created by Qi Le on 2019/10/9
 */
class SetServerCertificateName extends BaseAPI {
    @Override
    Class getZStackAction() {
        return UpdateCertificateAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "ServerCertificateId"
                    zstackParamName = ZSTACK_UUID
                }

                simpleConvert {
                    ecsParamName = "ServerCertificateName"
                    zstackParamName = ZSTACK_NAME
                }
            }

            convertAPIResponse {}
        }
    }
}
