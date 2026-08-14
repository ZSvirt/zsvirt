package org.zstack.pluginpremium.externalapiadapter.api.ecs.zone

import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.QueryClusterAction
import org.zstack.sdk.QueryInstanceOfferingAction

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by Qi Le on 2019/11/12
 */
class DescribeAvailableResource extends BaseAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                }

                simpleConvert {
                    ecsParamName = ECS_API_REGIONID_KEY
                    zstackParamName = "zone.name"
                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName=$zstackParamValue".toString())
                    }
                }

                simpleConvert {
                    ecsParamName = "DestinationResource"
                    putZstackParamValue = { Map zstackParamMap, String ecsParamValue ->
                        if (ecsParamValue != ECS_INSTANCE_TYPE) {
                            throw new APIParamConvertException(ecsParamName, "$ecsParamName must be '$ECS_INSTANCE_TYPE'.".toString())
                        }
                    }
                }
            }

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "AvailableZones"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { Map ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "AvailableZone"
                        ecsAttributeValue = new ArrayList<>()

                        addEcsValueToFather = { Map parentMap ->
                            parentMap.put(ecsAttributeName, ecsAttributeValue)
                        }

                        getZstackAttributeValue = {
                            return zstackAPIRsp.value.inventories
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addListElement = { ClusterInventory elementZStackValue ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { List parentList ->
                                    parentList.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = elementZStackValue

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_REGIONID_KEY

                                    zstackAttributeValue = ecsAPIParamMap.get(ECS_API_REGIONID_KEY)

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_ZONEID_KEY

                                    zstackAttributeValue = elementZStackValue.name

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    zstackAttributeValue = "Available"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "StatusCategory"

                                    zstackAttributeValue = "WithStock"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AvailableResources"

                                    ecsAttributeValue = new HashMap<>()

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, ecsAttributeValue)
                                        List availableResource = new ArrayList<>()
                                        ecsAttributeValue.put("AvailableResource", availableResource)
                                        List res = new ArrayList<>()
                                        def instanceType = [
                                                (ECS_API_TYPE_KEY)  : ECS_INSTANCE_TYPE,
                                                "SupportedResources": [
                                                        "SupportedResource": res
                                                ]
                                        ]
                                        availableResource.add(instanceType)
                                        QueryInstanceOfferingAction query = new QueryInstanceOfferingAction(
                                                sessionId: sessionId,
                                                conditions: ["type=UserVM"]
                                        )
                                        QueryInstanceOfferingAction.Result result = query.call()
                                        result.throwExceptionIfError()
                                        List names = result.value.inventories.stream().map { InstanceOfferingInventory offering -> offering.name }.collect(Collectors.toList())
                                        for (String name : names) {
                                            res.add([
                                                    "Value"             : name,
                                                    (ECS_API_STATUS_KEY): "Available",
                                                    "StatusCategory"    : "WithStock"
                                            ])
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return QueryClusterAction.class
    }
}
