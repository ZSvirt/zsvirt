package org.zstack.pluginpremium.externalapiadapter.api.ecs.deploymentset

import org.zstack.header.exception.CloudRuntimeException
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.pluginpremium.externalapiadapter.exception.InvalidParameterException
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.DeploymentSetStategy
import org.zstack.sdk.CreateAffinityGroupAction
import org.zstack.sdk.ErrorCode

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/4/28.
 */
class CreateDeploymentSet extends BaseAPI {
    @Override
    Class getZStackAction() {
        return CreateAffinityGroupAction.class
    }

    @Override
    void setEcsAPIParamDefaultValue(Map ecsAPIParamMap) {
        super.setEcsAPIParamDefaultValue(ecsAPIParamMap)

        if (!ecsAPIParamMap.containsKey(ECS_DEPLOYMENT_SET_STRATEGY)) {
            ecsAPIParamMap.put(ECS_DEPLOYMENT_SET_STRATEGY, DeploymentSetStategy.LOOSEDISPERSION.ecsValue)
        }
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {

                simpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_NAME
                    zstackParamName = ZSTACK_NAME
                    stillConvertParamWhenEcsParamValueIsNull = true

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        if (zstackParamValue == null) {
                            zstackParamValue = "untitled-deployment-set"
                        }
                        zstackParamMap.put(zstackParamName, zstackParamValue)
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_API_DESCRIPTION_KEY
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

                simpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_DESCRIPTION
                    zstackParamName = ZSTACK_API_DESCRIPTION_KEY
                }

                simpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_STRATEGY
                    zstackParamName = ZSTACK_DEPLOYMENT_SET_STRATEGY

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        try {
                            String policy = DeploymentSetStategy.getDeploymentSetStategyFromEcs(zstackParamValue).zstackValue
                            zstackParamMap[zstackParamName] = policy
                        } catch (CloudRuntimeException ignore) {
                            throw new InvalidParameterException(ecsParamName, new ErrorCode(
                                    code: ECSErrorCode.ApiUnsupported,
                                    details: "Not a supported strategy: $zstackParamValue".toString()
                            ))
                        }
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_GRANULARITY
                    zstackParamName = ZSTACK_API_TYPE_KEY
                    stillConvertParamWhenEcsParamValueIsNull = true

                    putZstackParamValue = { zstackParamMap, String zstackParamValue ->
                        if (!(zstackParamValue == null || "host" == zstackParamValue.toLowerCase())) {
                            throw new APIAdapterSpecifiedErrorException(ECSErrorCode.ApiUnsupported, "Not a supported granularity: $zstackParamValue".toString())
                        }
                        zstackParamMap.put(zstackParamName, "host")
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_DOMAIN

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        if (zstackParamValue != null && "Default" != zstackParamValue) {
                            throw new APIAdapterSpecifiedErrorException(ECSErrorCode.ApiUnsupported, "Not a supported domain: $zstackParamValue".toString())
                        }
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_DEPLOYMENT_SET_ID

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAPIRsp.value.inventory.uuid)
                    }
                }
            }
        }
    }
}
