package org.zstack.pluginpremium.externalapiadapter.api.ecs.image

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.RevokeResourceSharingAction
import org.zstack.sdk.ShareResourceAction

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_IMAGE_ID

/**
 * @Author: fubang* @Date: 2018/4/25
 */
class ModifyImageSharePermission extends BaseAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = ECS_IMAGE_ID
                    zstackParamName = "resourceUuids"
                    zstackParamType = List.class
                    putZstackParamValue = { zstackParamMap, String zstackParamValue ->
                        zstackParamMap.put(zstackParamName, [zstackParamValue])
                    }
                }

                for (n in 1..10){
                    simpleConvert {
                        ecsParamName = "AddAccount.${n}".toString()
                        zstackParamName = "shareAccountUuids"
                        zstackParamType = List.class
                        putZstackParamValue = { zstackParamMap, String zstackParamValue ->
                            def value = zstackParamMap.get(zstackParamName)
                            if (value == null) {
                                value = []
                                zstackParamMap.put(zstackParamName, value)
                            }
                            value.add(zstackParamValue)
                        }
                    }
                }

                for (n in 1..10){
                    simpleConvert {
                        ecsParamName = "RemoveAccount.${n}".toString()
                        zstackParamName = "revokeAccountUuids"
                        zstackParamType = List.class
                        putZstackParamValue = { zstackParamMap, String zstackParamValue ->
                            def value = zstackParamMap.get(zstackParamName)
                            if (value == null) {
                                value = []
                                zstackParamMap.put(zstackParamName, value)
                            }
                            value.add(zstackParamValue)
                        }
                    }
                }
            }
            convertAPIResponse {

            }
        }
    }

    @Override
    Object callZStackAction() {
        def resource = zstackAPIParamMap.get("resourceUuids") as List
        def shares = zstackAPIParamMap.get("shareAccountUuids") as List
        def revokes = zstackAPIParamMap.get("revokeAccountUuids") as List

        ShareResourceAction action = new ShareResourceAction()
        action.sessionId = sessionId
        action.resourceUuids = resource
        action.accountUuids = shares

        ShareResourceAction.Result result = action.call()
        result.throwExceptionIfError()

        RevokeResourceSharingAction action2 = new RevokeResourceSharingAction()
        action2.sessionId = sessionId
        action2.resourceUuids = resource
        action2.accountUuids = revokes

        RevokeResourceSharingAction.Result result2 = action2.call()
        result2.throwExceptionIfError()

        return null
    }

    @Override
    Class getZStackAction() {
        null
    }
}
