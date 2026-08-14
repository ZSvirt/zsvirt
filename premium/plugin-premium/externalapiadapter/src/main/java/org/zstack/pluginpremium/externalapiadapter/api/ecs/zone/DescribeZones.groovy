package org.zstack.pluginpremium.externalapiadapter.api.ecs.zone


import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.QueryClusterAction
import org.zstack.sdk.QueryInstanceOfferingAction

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.SUPPORT_RESOURCE_INFO.*

/**
 * @Author: fubang* @Date: 2018/4/28
 */
class DescribeZones extends BaseAPI {

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                }

                zstackNeedParam {
                    zstackParamName = "hypervisorType"

                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return null
                    }

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName=KVM".toString())
                    }
                }
            }

            convertAPIResponse {

                convertResponseAttribute {
                    ecsAttributeName = "Zones"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "Zone"
                        ecsAttributeValue = new ArrayList<>()

                        getZstackAttributeValue = {
                            return zstackAPIRsp.value.inventories
                        }

                        getElementZstackValues = {
                            return zstackAttributeValue
                        }

                        addEcsValueToFather = { fatherValue ->
                            fatherValue.put(ecsAttributeName, ecsAttributeValue)
                        }

                        addListElement = { ClusterInventory elementZstackValue ->

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = elementZstackValue

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_ZONEID_KEY

                                    zstackAttributeValue = elementZstackValue.name

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "LocalName"

                                    zstackAttributeValue = elementZstackValue.name

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AvailableResources"

                                    QueryInstanceOfferingAction query = new QueryInstanceOfferingAction(
                                            sessionId: sessionId,
                                            conditions: ["type=UserVm"]
                                    )
                                    QueryInstanceOfferingAction.Result result = query.call()
                                    result.throwExceptionIfError()
                                    List names = result.value.inventories.stream().map{InstanceOfferingInventory offering -> offering.name}.collect(Collectors.toList())

                                    zstackAttributeValue = [
                                            "ResourcesInfo" : [
                                                    [
                                                            "IoOptimized": true,
                                                            "SystemDiskCategories" : [
                                                                    "supportedSystemDiskCategory": [DISK_CATEGORY]
                                                            ],
                                                            "InstanceTypes" : [
                                                                    "supportedInstanceType" : names
                                                            ],
                                                            "InstanceTypeFamilies" : [
                                                                    "supportedInstanceTypeFamily": [INSTANCE_TYPE_FAMILY]
                                                            ],
                                                            "DataDiskCategories" : [
                                                                    "supportedDataDiskCategory": [DISK_CATEGORY]
                                                            ],
                                                            "InstanceGenerations" : [
                                                                    "supportedInstanceGeneration": [INSTANCE_TYPE_GENERATION]
                                                            ],
                                                            "NetworkTypes" : [
                                                                    "supportedNetworkCategory": [ECS_NETWORK_TYPE_VPC, ECS_NETWORK_TYPE_CLASSIC]
                                                            ]
                                                    ]
                                            ]
                                    ]

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }

                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AvailableInstanceTypes"

                                    addEcsValueToFather = { fatherValue ->
                                        QueryInstanceOfferingAction query = new QueryInstanceOfferingAction(
                                                sessionId: sessionId,
                                                conditions: ["type=UserVm"]
                                        )
                                        QueryInstanceOfferingAction.Result result = query.call()
                                        result.throwExceptionIfError()
                                        List names = result.value.inventories.stream().map{InstanceOfferingInventory offering -> offering.name}.collect(Collectors.toList())

                                        zstackAttributeValue = [
                                                "InstanceTypes" : names
                                        ]

                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }

                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AvailableDiskCategories"

                                    zstackAttributeValue = ["DiskCategories": [DISK_CATEGORY]]

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AvailableResourceCreation"

                                    zstackAttributeValue = ["ResourceTypes": [ECS_INSTANCE, ECS_DISK, "VSwitch"]]

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AvailableVolumeCategories"

                                    zstackAttributeValue = ["VolumeCategories": [DISK_CATEGORY]]

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
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
