package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance

import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.convert.param.ParameterConversionUtils
import org.zstack.sdk.*

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * Created by lining on 2018/6/26.
 */
class DescribeInstanceStatus extends BaseQueryAPI {

    void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = ECS_API_ZONEID_KEY
                    zstackParamName = ZSTACK_API_ZONEID_KEY

                    putZstackParamValue = { Map zstackParamMap, String ecsParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        String zoneId = ParameterConversionUtils.convertZoneId(sessionId, ecsParamValue)
                        conditions.add("$zstackParamName=$zoneId".toString())
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_API_TYPE_KEY

                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return VmInstanceConstant.USER_VM_TYPE
                    }

                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        conditions.add("$zstackParamName=$zstackParamValue".toString())
                    }
                }

            }

            convertQueryAPIResponse {

                convertResponseAttribute {
                    ecsAttributeName = "InstanceStatuses"
                    ecsAttributeValue = new HashMap<>()

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "InstanceStatus"
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

                        addListElement = { VmInstanceInventory vmInventory ->

                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = vmInventory

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_ID

                                    zstackAttributeValue = vmInventory.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_ZONEID_KEY

                                    zstackAttributeValue = ecsAPIParamMap.get(ecsAttributeName)

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
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    zstackAttributeValue = vmInventory.state

                                    addEcsValueToFather = { fatherValue ->
                                        // todo
                                        if (VmInstanceState.Rebooting.toString() == zstackAttributeValue) {
                                            zstackAttributeValue = VmInstanceState.Starting.toString()
                                        }

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
        return QueryVmInstanceAction.class
    }

}
