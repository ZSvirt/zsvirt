package org.zstack.pluginpremium.externalapiadapter.api.ecs.image

import org.apache.commons.lang.StringUtils
import org.zstack.header.identity.AccountType
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.ImageStatus
import org.zstack.sdk.GetResourceAccountAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.QueryImageAction
import org.zstack.utils.data.SizeUnit

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/25
 */
class DescribeImages extends BaseQueryAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = ECS_IMAGE_ID
                    zstackParamName = ZSTACK_UUID

                    putZstackParamValue = { zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName?=$zstackParamValue".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_IMAGE_NAME
                    zstackParamName = ZSTACK_NAME
                }

                querySimpleConvert {
                    ecsParamName = ECS_API_STATUS_KEY
                    zstackParamName = ZSTACK_API_STATUS_KEY
                    stillConvertParamWhenEcsParamValueIsNull = true

                    putZstackParamValue = {Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        String status = null
                        if (zstackParamValue == null) {
                            status = ImageStatus.AVAILABLE.zstackValue
                        } else {
	                        List<String> tmp = new ArrayList<String>()
                            for (String ecsImageStatus :  zstackParamValue.split(COMMON_SEPARATOR)) {
                                tmp.add(ImageStatus.getImageStatusFromEcs(ecsImageStatus).zstackValue)
                            }
                            status = StringUtils.join(tmp, COMMON_SEPARATOR)
                        }

                        conditions.add("$zstackParamName?=$status".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = "Usage"
                    zstackParamName = "volume.rootImageUuid"
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        String value = null
                        if (zstackParamValue == "none") {
                            value = "is null"
                        } else if (zstackParamValue == "instance") {
                            value = "not null"
                        }

                        if (value == null){
                            throw new APIParamConvertException(ecsParamName, "${ecsParamName}[value: $zstackParamValue] is not valid".toString())
                        }
                        conditions.add("$zstackParamName $value".toString())
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
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_IMAGE_SYSTEM
                    getZstackValue = { Map zstackParamMap, Map zstackParamValue ->
                        return null
                    }
                    putZstackParamValue = { Map zstackParamMap, String putZstackParamValue ->
                        List conditions = zstackParamMap[ZSTACK_QUERY_CONDITIONS_KEY] as List
                        conditions.add("${zstackParamName}=False".toString())
                    }
                }
            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "Images"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = "Image"
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
                        addListElement = { ImageInventory elementZstackValue ->
                            addConvertResponseAttribute {
                                ecsAttributeValue = new HashMap<>()

                                addEcsValueToFather = { fatherValue ->
                                    fatherValue.add(ecsAttributeValue)
                                }

                                zstackAttributeValue = elementZstackValue

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_IMAGE_ID

                                    zstackAttributeValue = elementZstackValue.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ImageName"

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
                                    ecsAttributeName = "OSType"

                                    zstackAttributeValue = elementZstackValue.platform != null ? elementZstackValue.platform.toLowerCase() : null

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "ImageOwnerAlias"

                                    zstackAttributeValue = "system"

                                    addEcsValueToFather = { fatherValue ->
                                        GetResourceAccountAction accountAction = new GetResourceAccountAction(
                                                sessionId: sessionId,
                                                resourceUuids: [elementZstackValue.uuid]
                                        )
                                        GetResourceAccountAction.Result accountResult = accountAction.call()

                                        if (accountResult.error == null && accountResult.value.inventories.containsKey(elementZstackValue.uuid)) {
                                            Map acc = accountResult.value.inventories.get(elementZstackValue.uuid)
                                            if (acc[ZSTACK_API_TYPE_KEY] != AccountType.SystemAdmin.toString()) {
                                                zstackAttributeValue = "self"
                                            }
                                        }
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_SIZE

                                    zstackAttributeValue = elementZstackValue.size

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, Math.ceil(SizeUnit.BYTE.toGigaByte(zstackAttributeValue.toDouble())).toInteger())
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    zstackAttributeValue = elementZstackValue.status

                                    addEcsValueToFather = { fatherValue ->
                                        String value = ImageStatus.getImageStatusFromZstack(zstackAttributeValue as String).ecsValue
                                        fatherValue.put(ecsAttributeName, value)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Usage"

                                    addEcsValueToFather = { fatherValue ->
                                        String value = ecsAPIParamMap.get(ecsAttributeName)
                                        if (value == null) {
                                            QueryImageAction action = new QueryImageAction()
                                            action.sessionId = sessionId
                                            action.conditions = ["uuid=${elementZstackValue.uuid}".toString(), "volume.rootImageUuid not null"]

                                            QueryImageAction.Result result = action.call()
                                            if (result.error != null) {
                                                throw new APIParamConvertException(ecsAttributeName, result.error)
                                            }
                                            if (result.value.inventories.size() > 0) {
                                                value = "instance"
                                            } else {
                                                value = "none"
                                            }
                                        }
                                        fatherValue.put(ecsAttributeName, value)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_CREATION_TIME

                                    zstackAttributeValue = elementZstackValue.createDate

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ExternalAPIAdapterUtils.formatIso8601Date(zstackAttributeValue))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "IsSubscribed"

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, false)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "Progress"

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, "100%")
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_IMAGE_ARCHITECTURE
                                    zstackAttributeValue = elementZstackValue.architecture

                                    addEcsValueToFather = { Map parentMap ->
                                        if (zstackAttributeValue == null) {
                                            zstackAttributeValue = "x86_64"
                                        }
                                        (parentMap[ecsAttributeName] = zstackAttributeValue)
                                    }
                                }

                                // todo
                                convertResponseAttribute {
                                    ecsAttributeName = ECS_IMAGE_PLATFORM
                                    zstackAttributeValue = elementZstackValue.platform

                                    addEcsValueToFather = { Map parentMap ->
                                        if (zstackAttributeValue == null) {
                                            zstackAttributeValue = "Other"
                                        }
                                        (parentMap[ecsAttributeName] = zstackAttributeValue)
                                    }
                                }

                                // todo
                                convertResponseAttribute {
                                    ecsAttributeName = "OSName"

                                    zstackAttributeValue = elementZstackValue.name

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                            }
                        }
                    }

                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_API_REGIONID_KEY

                    getZstackAttributeValue = {
                        return ecsAPIParamMap.get(ecsAttributeName)
                    }

                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, zstackAttributeValue)
                    }
                }
            }
        }
    }

    @Override
    Class getZStackAction() {
        return QueryImageAction.class
    }

    @Override
    void afterGetEcsApiRsp(Map ecsApiRsp) {
        String ownerAlias = ecsAPIParamMap.get("ImageOwnerAlias")
        if (ownerAlias == null) {
            return
        }
        List images = ecsApiRsp.get("Images").get("Image")
        if (images == null || images.size() == 0) {
            return
        }
        int originSize = images.size()
        List filteredImages = images.stream().filter {HashMap img ->
            img.get("ImageOwnerAlias") == ownerAlias
        }.collect(Collectors.toList())
        int newSize = filteredImages.size()
        ecsApiRsp.get("Images").put("Image", filteredImages)
        int newTotalCount = Math.max(ecsApiRsp.get(ECS_TOTAL_COUNT) - (originSize - newSize), 0)
        ecsApiRsp.put(ECS_TOTAL_COUNT, newTotalCount)
    }
}
