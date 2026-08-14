package org.zstack.pluginpremium.externalapiadapter.api.ecs.others


import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.sdk.QueryZoneAction
import org.zstack.sdk.ZoneInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_API_REGIONID_KEY

/**
 * @Author: fubang
 * @Date: 2018/4/26
 */

class DescribeRegions extends BaseQueryAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {}

            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "Regions"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "Region"
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

                        addListElement = { ZoneInventory elementZstackValue ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = elementZstackValue

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_REGIONID_KEY

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
                            }
                        }
                    }

                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return QueryZoneAction.class
    }
}
