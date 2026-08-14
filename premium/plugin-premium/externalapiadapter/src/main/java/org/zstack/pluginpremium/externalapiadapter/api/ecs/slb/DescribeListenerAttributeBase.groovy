package org.zstack.pluginpremium.externalapiadapter.api.ecs.slb

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.QueryLoadBalancerListenerAction
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_QUERY_CONDITIONS_KEY
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_QUERY_REPLYWITHCOUNT_KEY

/**
 * Created by Qi Le on 2020/3/20
 */
//half mock api
abstract class DescribeListenerAttributeBase extends BaseAPI {
    private static final String protocol = "undefined"

    @Override
    Class getZStackAction() {
        return QueryLoadBalancerListenerAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { Map zstackParamMap ->
                    zstackParamMap[ZSTACK_QUERY_CONDITIONS_KEY] = []
                    zstackParamMap[ZSTACK_QUERY_REPLYWITHCOUNT_KEY] = true
                }

                querySimpleConvert {
                    ecsParamName = "LoadBalancerId"
                    zstackParamName = "loadBalancerUuid"
                }

                querySimpleConvert {
                    ecsParamName = "ListenerPort"
                    zstackParamName = "loadBalancerPort"
                }

                zstackNeedParam {
                    zstackParamName = "protocol"

                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return protocol
                    }

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap[ZSTACK_QUERY_CONDITIONS_KEY]
                        conditions.add("$zstackParamName=$zstackParamValue".toString())
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "Status"
                    zstackAttributeValue = "running"
                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp[ecsAttributeName] = zstackAttributeValue
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "ListenerPort"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.loadBalancerPort
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp[ecsAttributeName] = zstackAttributeValue
                    }
                }

                convertResponseAttribute {
                    ecsAttributeName = "BackendServerPort"

                    getZstackAttributeValue = {
                        return zstackAPIRsp.instancePort
                    }

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp[ecsAttributeName] = zstackAttributeValue
                    }
                }
            }
        }
    }

    @Override
    Object callZStackAction() {
        Gson gson = new GsonBuilder().create()
        QueryLoadBalancerListenerAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), QueryLoadBalancerListenerAction.class)
        def result = action.call()
        result.throwExceptionIfError()

        if (result.value.inventories == null ||result.value.inventories.size() == 0) {
            return null
        }

        LoadBalancerListenerInventory listener = result.value.inventories.first()

        this.afterCallZStackAction(listener)

        return listener
    }
}
