package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.DeleteCertificateAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_UUID

/**
 * Created by Qi Le on 2019/11/14
 */
class DeleteServerCertificate extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "ServerCertificateId"
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertAPIResponse {}
        }
    }

    @Override
    Class getZStackAction() {
        return DeleteCertificateAction.class
    }
}
