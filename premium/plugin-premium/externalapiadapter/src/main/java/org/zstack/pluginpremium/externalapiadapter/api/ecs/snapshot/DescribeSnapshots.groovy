package org.zstack.pluginpremium.externalapiadapter.api.ecs.snapshot

import org.apache.commons.lang.StringUtils
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIResponseConvertException
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.DiskType
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.SnapshotStatus
import org.zstack.sdk.QueryVolumeAction
import org.zstack.sdk.QueryVolumeSnapshotAction
import org.zstack.utils.data.SizeUnit

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/25
 */
class DescribeSnapshots extends BaseQueryAPI {

    @Override
    Class getZStackAction() {
        return QueryVolumeSnapshotAction.class
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_DISK_UUID

                    putZstackParamValue = { Map zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)

                        QueryVolumeAction action = new QueryVolumeAction()
                        action.sessionId = sessionId
                        action.conditions = ["vmInstanceUuid=$zstackParamValue".toString()]
                        action.apiId = requestId

                        QueryVolumeAction.Result result = action.call()
                        result.throwExceptionIfError()

                        def inventories = result.value.inventories

                        if (inventories.size() > 0) {
                            String volumes = zstackParamName + "?="
                            inventories.each { volumes += it.uuid + "," }
                            volumes = volumes.substring(0, volumes.length() - 1)
                            conditions.add(volumes)
                        }
                    }
                }
                querySimpleConvert {
                    ecsParamName = ECS_DISK_ID
                    zstackParamName = ZSTACK_DISK_UUID
                }

                querySimpleConvert {
                    ecsParamName = "SnapshotIds"
                    ecsParamType = ArrayList.class
                    zstackParamName = ZSTACK_UUID
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        String uuids = StringUtils.join(zstackParamValue, ",")
                        conditions.add("$zstackParamName?=$uuids".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = "SnapshotName"
                    zstackParamName = ZSTACK_NAME
                }

                querySimpleConvert {
                    ecsParamName = "SourceDiskType"
                    zstackParamName = "volumeType"
                    putZstackParamValue = {Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        String value = DiskType.getDiskTypeFromEcs(zstackParamValue).zstackValue
                        conditions.add("$zstackParamName=$value".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_API_STATUS_KEY
                    zstackParamName = ZSTACK_API_STATUS_KEY
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        String value = SnapshotStatus.getSnapshotStatusFromEcs(zstackParamValue as String).zstackValue

                        if (value == null){
                            return
                        }
                        conditions.add("$zstackParamName=$value".toString())
                    }
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

                zstackNeedParam {
                    zstackParamName = ZSTACK_API_STATUS_KEY
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return null
                    }
                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        conditions.add("${zstackParamName}!=Deleted".toString())
                        conditions.add("${zstackParamName}!=Deleting".toString())
                    }
                }
            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "Snapshots"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "Snapshot"
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
                        addListElement = { elementZstackValue ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = elementZstackValue

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_SNAPSHOT_ID

                                    zstackAttributeValue = elementZstackValue.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "SnapshotType"

                                    zstackAttributeValue = "user"

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap[ecsAttributeName] = zstackAttributeValue
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "SnapshotName"

                                    zstackAttributeValue = elementZstackValue.name

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_DESCRIPTION_KEY

                                    zstackAttributeValue = elementZstackValue.description

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "SourceDiskId"
                                    zstackAttributeValue = elementZstackValue.volumeUuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)

                                        String paramName = "SourceDiskSize"
                                        QueryVolumeAction action = new QueryVolumeAction()
                                        action.sessionId = sessionId
                                        action.conditions = ["uuid=$zstackAttributeValue".toString()]

                                        QueryVolumeAction.Result result = action.call()
                                        result.throwExceptionIfError()

                                        if (result.value.inventories.isEmpty()){
                                            throw new APIResponseConvertException(ecsAttributeName, "${action.class.simpleName} result is empty".toString())
                                        }

                                        def size = result.value.inventories.get(0).size
                                        fatherValue.put(paramName, Math.ceil(SizeUnit.BYTE.toGigaByte(size.toDouble())).toInteger())
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "SourceDiskType"

                                    zstackAttributeValue = elementZstackValue.volumeType

                                    addEcsValueToFather = { fatherValue ->
                                        def value = DiskType.getDiskTypeFromZstack(zstackAttributeValue as String).ecsValue
                                        fatherValue.put(ecsAttributeName, value)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    zstackAttributeValue = elementZstackValue.status

                                    addEcsValueToFather = { fatherValue ->
                                        String value = SnapshotStatus.getSnapshotStatusFromZstack(zstackAttributeValue as String).ecsValue
                                        fatherValue.put(ecsAttributeName, value)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Progress"

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, "100%")
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DISK_ENCRYPTED

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, false)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_CREATION_TIME

                                    zstackAttributeValue = elementZstackValue.createDate

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ExternalAPIAdapterUtils.formatIso8601Date(zstackAttributeValue))
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
