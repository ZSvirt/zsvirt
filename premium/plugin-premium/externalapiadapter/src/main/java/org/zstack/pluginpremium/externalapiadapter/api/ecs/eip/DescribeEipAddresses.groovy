package org.zstack.pluginpremium.externalapiadapter.api.ecs.eip

import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.EipStatus
import org.zstack.sdk.*
import org.zstack.utils.data.SizeUnit

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang* @Date: 2018/5/19
 */
class DescribeEipAddresses extends BaseQueryAPI {

    @Override
    protected void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = ECS_API_STATUS_KEY
                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        conditions.add(EipStatus.getEipStatusFromEcs(zstackParamValue).zstackValue)
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_EIP_ADDRESS
                    zstackParamName = "vipIp"
                }

                querySimpleConvert {
                    ecsParamName = ECS_EIP_ALLOCATION_ID
                    zstackParamName = ZSTACK_UUID
                }

                querySimpleConvert {
                    ecsParamName = "AssociatedInstanceId"
                    zstackParamName = "vmNic.vmInstanceUuid"
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_API_STATE_KEY
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return null
                    }
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName=Enabled".toString())
                    }
                }
            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "EipAddresses"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = ECS_EIP_ADDRESS
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

                        addListElement = { EipInventory elementZstackValue ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = elementZstackValue

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_REGIONID_KEY

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_NETWORK_IP_ADDRESS

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, elementZstackValue.vipIp)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_EIP_ALLOCATION_ID

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, elementZstackValue.uuid)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    addEcsValueToFather = { Map parentMap ->
                                        QuerySystemTagAction qTagAct = new QuerySystemTagAction(
                                                sessionId: sessionId,
                                                conditions: [
                                                        "resourceUuid=${elementZstackValue.uuid}".toString(),
                                                        "resourceType=EipVO"
                                                ]
                                        )
                                        QuerySystemTagAction.Result qTagRes = qTagAct.call()

                                        List<SystemTagInventory> tags = qTagRes.value?.inventories as List<SystemTagInventory>
                                        String status
                                        if (tags != null && !tags.isEmpty()) {
                                            def statusTag = tags.find { SystemTagInventory tag -> EcsSystemTags.EIP_INTERMEDIATE_STATUS.isMatch(tag.tag) }
                                            status = EcsSystemTags.EIP_INTERMEDIATE_STATUS.getTokenByTag(statusTag.tag, EcsSystemTags.EIP_INTERMEDIATE_STATUS_TOKEN)
                                        }

                                        status = status == null ?
                                                EipStatus.getEipStatusFromZstack(elementZstackValue).ecsValue : EipStatus.getEipStatusFromTag(status).ecsValue

                                        parentMap[ecsAttributeName] = status
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_ID

                                    addEcsValueToFather = { fatherValue ->
                                        String instanceId = ecsAPIParamMap["AssociatedInstanceId"]
                                        if (instanceId == null) {
                                            QueryVmInstanceAction action = new QueryVmInstanceAction()
                                            action.sessionId = sessionId
                                            action.conditions = ["vmNics.uuid=${elementZstackValue.vmNicUuid}".toString()]

                                            QueryVmInstanceAction.Result result = action.call()
                                            result.throwExceptionIfError()
                                            List inventories = result.value.inventories
                                            instanceId = inventories.size() == 0 ? null : inventories.first().uuid
                                        }
                                        if (instanceId != null) {
                                            fatherValue[ecsAttributeName] = instanceId
                                            fatherValue[ECS_INSTANCE_TYPE] = "EcsInstance"
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_NETWORK_BANDWIDTH

                                    addEcsValueToFather = { fatherValue ->
                                        String vipId = elementZstackValue.vipUuid
                                        GetVipQosAction action = new GetVipQosAction()
                                        action.sessionId = sessionId
                                        action.uuid = vipId

                                        GetVipQosAction.Result result = action.call()
                                        result.throwExceptionIfError()

                                        List inventories = result.value.inventories
                                        String brandWidth = inventories.size() > 0 ? SizeUnit.BYTE.toMegaByte(Double.parseDouble(inventories.first().inboundBandwidth.toString()) / 8.0).toString() : "5.0"
                                        fatherValue.put(ecsAttributeName, brandWidth)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ExpiredTime"

                                    addEcsValueToFather = { fatherValue ->
                                        Calendar calendar = Calendar.getInstance()
                                        calendar.setTime(elementZstackValue.createDate)
                                        calendar.add(Calendar.YEAR, 100)

                                        fatherValue.put(ecsAttributeName, ExternalAPIAdapterUtils.formatIso8601Date(calendar.getTime()))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "AllocationTime"

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ExternalAPIAdapterUtils.formatIso8601Date(elementZstackValue.createDate))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ISP"
                                    zstackAttributeValue = "Intranet"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
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
        return QueryEipAction.class
    }
}
