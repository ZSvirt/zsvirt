package org.zstack.pluginpremium.externalapiadapter.api.ecs.deploymentset

import org.apache.commons.lang.StringUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.DeploymentSetStategy
import org.zstack.sdk.AffinityGroupInventory
import org.zstack.sdk.QueryAffinityGroupAction

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/4/28.
 */
class DescribeDeploymentSets extends BaseQueryAPI{
    @Override
    Class getZStackAction() {
        return QueryAffinityGroupAction.class
    }

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_DEPLOYMENT_SET_APPLIANCE
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return null
                    }
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName=CUSTOMER".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_IDS
                    ecsParamType = ArrayList.class
                    zstackParamName = ZSTACK_UUID
                    zstackParamType = String.class

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)

                        String uuids = StringUtils.join(zstackParamValue, ",")
                        conditions.add("$zstackParamName?=$uuids".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_NAME
                    zstackParamName = ZSTACK_NAME

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName=$zstackParamValue".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_GRANULARITY
                    zstackParamName = ZSTACK_API_TYPE_KEY

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName=HOST".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_DEPLOYMENT_SET_STRATEGY
                    zstackParamName = ZSTACK_DEPLOYMENT_SET_STRATEGY

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List

                        String policy = DeploymentSetStategy.getDeploymentSetStategyFromEcs(zstackParamValue).zstackValue
                        conditions.add("$zstackParamName=$policy".toString())
                    }
                }
            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_DEPLOYMENT_SETS
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = ECS_DEPLOYMENT_SET
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

                        addListElement = { AffinityGroupInventory elementZStackValue ->

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = elementZStackValue

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DEPLOYMENT_SET_ID

                                    zstackAttributeValue = elementZStackValue.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DEPLOYMENT_SET_NAME

                                    zstackAttributeValue = elementZStackValue.name

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DEPLOYMENT_SET_DESCRIPTION

                                    zstackAttributeValue = elementZStackValue.description

                                    addEcsValueToFather = { fatherValue ->
                                        String desc = zstackAttributeValue ? zstackAttributeValue : ""
                                        fatherValue.put(ecsAttributeName, desc)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DEPLOYMENT_SET_DOMAIN

                                    zstackAttributeValue = "Default"

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DEPLOYMENT_SET_GRANULARITY

                                    zstackAttributeValue = "Host"

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DEPLOYMENT_SET_STRATEGY

                                    zstackAttributeValue = elementZStackValue.policy

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, DeploymentSetStategy.getDeploymentSetStategyFromZstack(zstackAttributeValue as String).ecsValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_REGIONID_KEY

                                    zstackAttributeValue = ecsAPIParamMap.get(ECS_API_REGIONID_KEY)

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_IDS

                                    zstackAttributeValue = elementZStackValue.usages

                                    addEcsValueToFather = { Map parentMap ->
                                        List instanceIds = zstackAttributeValue.stream().filter{usage ->
                                            usage.resourceType == "VmInstanceVO"
                                        }.map {usage ->
                                            usage.resourceUuid
                                        }.collect(Collectors.toList())
                                        parentMap.put(ecsAttributeName, instanceIds)
                                        parentMap.put(ECS_DEPLOYMENT_SET_INSTANCE_AMOUNT, instanceIds.size())
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
