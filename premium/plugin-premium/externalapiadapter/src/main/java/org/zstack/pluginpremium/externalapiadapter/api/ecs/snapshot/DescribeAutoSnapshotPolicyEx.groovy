package org.zstack.pluginpremium.externalapiadapter.api.ecs.snapshot

import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.QuerySchedulerJobAction
import org.zstack.sdk.QuerySchedulerTriggerAction
import org.zstack.sdk.SchedulerTriggerInventory

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/5/1
 */
class DescribeAutoSnapshotPolicyEx extends BaseQueryAPI {
    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = "AutoSnapshotPolicyId"
                    zstackParamName = ZSTACK_UUID
                }
            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "AutoSnapshotPolicies"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "AutoSnapshotPolicy"
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

                        addListElement = { SchedulerTriggerInventory elementZstackValue ->

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = elementZstackValue

                                convertResponseAttribute {
                                    ecsAttributeName = "AutoSnapshotPolicyId"

                                    zstackAttributeValue = elementZstackValue.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_REGIONID_KEY

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AutoSnapshotPolicyName"

                                    zstackAttributeValue = elementZstackValue.name

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "TimePoints"

                                    addEcsValueToFather = { fatherValue ->
                                        Calendar calendar = Calendar.getInstance()
                                        calendar.setTime(elementZstackValue.startTime)
                                        fatherValue.put(ecsAttributeName, "[\"${calendar.get(Calendar.HOUR_OF_DAY).toString()}\"]".toString())
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "RepeatWeekdays"

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, "[\"1\", \"2\", \"3\", \"4\", \"5\", \"6\", \"7\"]")
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "RetentionDays"

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, -1)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "DiskNums"

                                    addEcsValueToFather = { fatherValue ->
                                        QuerySchedulerJobAction action = new QuerySchedulerJobAction()
                                        action.sessionId = sessionId
                                        action.apiId = requestId
                                        action.conditions = ["trigger.uuid=${elementZstackValue.uuid}".toString()]

                                        QuerySchedulerJobAction.Result result = action.call()
                                        result.throwExceptionIfError()

                                        fatherValue.put(ecsAttributeName, result.value.inventories.size())
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, "Available")
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_CREATION_TIME

                                    zstackAttributeValue = elementZstackValue.createDate

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
        return QuerySchedulerTriggerAction.class
    }
}
