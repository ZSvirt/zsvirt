package org.zstack.pluginpremium.externalapiadapter.api.ecs.others

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.apache.commons.lang.StringUtils
import org.zstack.mevoco.MevocoSystemTags
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterGlobalConfig
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.datatypes.GpuSpec
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.sdk.PciDeviceSpecInventory
import org.zstack.sdk.QueryInstanceOfferingAction
import org.zstack.sdk.QueryPciDeviceSpecAction
import org.zstack.sdk.QuerySystemTagAction
import org.zstack.sdk.SystemTagInventory
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.lang.reflect.Type

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang* @Date: 2018/4/26
 */
class DescribeInstanceTypes extends BaseQueryAPI {

    List<GpuSpec> gpuSpecs

    @Override
    void setEcsAPIParamDefaultValue(Map ecsAPIParamMap) {
        super.setEcsAPIParamDefaultValue(ecsAPIParamMap)

        ecsAPIParamMap.put(ECS_QUERY_API_PAGESIZE_KEY, "100")

        getGPUSpecs()
    }

    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
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
                    zstackParamName = ZSTACK_API_TYPE_KEY
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return null
                    }
                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        conditions.add("$zstackParamName=UserVm".toString())
                    }
                }
            }
            convertAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = "InstanceTypes"
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = ECS_INSTANCE_TYPE
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
                                    ecsAttributeName = ECS_INSTANCE_TYPE_ID

                                    zstackAttributeValue = elementZstackValue.name

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "CpuCoreCount"

                                    zstackAttributeValue = elementZstackValue.cpuNum

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "MemorySize"

                                    zstackAttributeValue = elementZstackValue.memorySize

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, SizeUnit.BYTE.toGigaByte((double) zstackAttributeValue))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "InstanceTypeFamily"

                                    zstackAttributeValue = elementZstackValue.name

                                    addEcsValueToFather = { Map parentMap ->
                                        String[] typeNameElements = StringUtils.split(zstackAttributeValue as String, '.')
                                        String typeFamily = typeNameElements.length > 1 ? "${typeNameElements[0]}.${typeNameElements[1]}".toString() : zstackAttributeValue
                                        parentMap.put(ecsAttributeName, typeFamily)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "EniQuantity"

                                    zstackAttributeValue = 22

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "InstanceFamilyLevel"

                                    zstackAttributeValue = ""

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = "LocalStorageCategory"

                                    zstackAttributeValue = ""

                                    addEcsValueToFather = { Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {

                                    zstackAttributeValue = elementZstackValue.uuid

                                    addEcsValueToFather = { Map parentMap ->
                                        QuerySystemTagAction tagAction = new QuerySystemTagAction(
                                                sessionId: sessionId,
                                                conditions: [
                                                        "resourceType=InstanceOfferingVO",
                                                        "resourceUuid=$zstackAttributeValue".toString()
                                                ]
                                        )
                                        QuerySystemTagAction.Result tagResult = tagAction.call()
                                        //ASCM's bandwidth: 1Gbps = 1024Mbps = 1024000Kbps = 1024000000bps
                                        //ZStack's bandwidth: 1Gbps = 1024Mbps = 1048576Kbps = 1073741824bps
                                        if (tagResult.error != null || tagResult.value.inventories.size() == 0) {
                                            //32Gbps -> 32 * 1024000Kbps
                                            parentMap.put(ECS_INSTANCE_TYPE_BANDWIDTH_INBOUND, 32768000)
                                            parentMap.put(ECS_INSTANCE_TYPE_BANDWIDTH_OUTBOUND, 32768000)
                                            return
                                        }
                                        String inbound = "34359738368"
                                        String outbound = "34359738368"
                                        String gpuDeviceId
                                        String gpuVendorId
                                        String gpuSubDeviceId
                                        String gpuAmount
                                        tagResult.value.inventories.forEach { SystemTagInventory tag ->
                                            if (MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.isMatch(tag.tag)) {
                                                inbound = MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.getTokenByTag(tag.tag, MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH_TOKEN)
                                            } else if (MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.isMatch(tag.tag)) {
                                                outbound = MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.getTokenByTag(tag.tag, MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN)
                                            } else if (EcsSystemTags.PCI_DEVICE_INFO.isMatch(tag.tag)) {
                                                Map tokens = EcsSystemTags.PCI_DEVICE_INFO.getTokensByTag(tag.tag)
                                                gpuDeviceId = tokens.get(EcsSystemTags.PCI_DEVICE_INFO_DEVICEID_TOKEN)
                                                gpuVendorId = tokens.get(EcsSystemTags.PCI_DEVICE_INFO_VENDORID_TOKEN)
                                                gpuSubDeviceId = tokens.get(EcsSystemTags.PCI_DEVICE_INFO_SUBDEVICEID_TOKEN)
                                                gpuAmount = tokens.get(EcsSystemTags.PCI_DEVICE_INFO_AMOUNT_TOKEN)
                                            }
                                        }
                                        long inboundValue = (SizeUnit.BYTE.toKiloByte(Long.parseLong(inbound)) * 1000 / 1024).toLong()
                                        long outboundValue = (SizeUnit.BYTE.toKiloByte(Long.parseLong(outbound)) * 1000 / 1024).toLong()
                                        parentMap.put(ECS_INSTANCE_TYPE_BANDWIDTH_INBOUND, inboundValue)
                                        parentMap.put(ECS_INSTANCE_TYPE_BANDWIDTH_OUTBOUND, outboundValue)

                                        if (!(gpuDeviceId == null || gpuVendorId == null || gpuSubDeviceId == null || gpuAmount == null)) {
                                            QueryPciDeviceSpecAction pciAction = new QueryPciDeviceSpecAction(
                                                    sessionId: sessionId,
                                                    conditions: [
                                                            "type?=GPU_Video_Controller,GPU_3D_Controller",
                                                            "deviceId=$gpuDeviceId".toString(),
                                                            "vendorId=$gpuVendorId".toString(),
                                                            "subdeviceId=$gpuSubDeviceId".toString()
                                                    ]
                                            )
                                            QueryPciDeviceSpecAction.Result pciResult = pciAction.call()
                                            if (pciResult.error != null || pciResult.value.inventories.size() == 0) {
                                                parentMap.put(ECS_INSTANCE_TYPE_GPU_AMOUNT, 0)
                                                parentMap.put(ECS_INSTANCE_TYPE_GPU_SPEC, "")
                                                parentMap.put(ECS_INSTANCE_TYPE_ENI_QUANTITY, 22)
                                                return
                                            }
                                            PciDeviceSpecInventory pciSpec = pciResult.value.inventories.first()
                                            GpuSpec gpuSpec = gpuSpecs.find { spec ->
                                                (!(spec.deviceId != pciSpec.deviceId ||
                                                        spec.vendorId != pciSpec.vendorId ||
                                                        spec.subDeviceId != pciSpec.subdeviceId))
                                            } as GpuSpec
                                            int pciSpeed
                                            if (gpuSpec == null) {
                                                parentMap.put(ECS_INSTANCE_TYPE_GPU_SPEC, pciSpec.name)
                                                pciSpeed = 8
                                            } else {
                                                parentMap.put(ECS_INSTANCE_TYPE_GPU_SPEC, gpuSpec.ecsGpuSpec)
                                                pciSpeed = gpuSpec.pciSpeed
                                            }
                                            int pciDeviceNum = Integer.parseInt(gpuAmount)
                                            parentMap.put(ECS_INSTANCE_TYPE_GPU_AMOUNT, pciDeviceNum)
                                            int pciLanes = pciSpeed * pciDeviceNum
                                            if (pciLanes > 22) {
                                                throw new APIParamConvertException(ECS_INSTANCE_TYPE_GPU_AMOUNT, "PCI device (GPU) occupies more than 22 PCI lanes.")
                                            }
                                            parentMap.put(ECS_INSTANCE_TYPE_ENI_QUANTITY, 22 - pciLanes)
                                        } else {
                                            parentMap.put(ECS_INSTANCE_TYPE_ENI_QUANTITY, 22)
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
        return QueryInstanceOfferingAction.class
    }

    @Override
    Object callZStackAction() {
        Gson gson = new GsonBuilder().create()
        QueryInstanceOfferingAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), this.getZStackAction())

        QueryInstanceOfferingAction.Result result = action.call()
        result.throwExceptionIfError()

        List inventoryAllList = []
        inventoryAllList.addAll(result.value.inventories)
        int total = result?.value?.total
        while (inventoryAllList.size() < total) {
            action.start = action.start + 100
            QueryInstanceOfferingAction.Result tmp = action.call()
            result.throwExceptionIfError()

            if(tmp.value.inventories.size() == 0) {
                break
            }

            inventoryAllList.addAll(tmp.value.inventories)
        }

        result.value.inventories = inventoryAllList
        result.value.total = inventoryAllList.size()

        this.afterCallZStackAction(result)
        return result
    }

    private void getGPUSpecs() {
        String gpuMapping = ExternalAPIAdapterGlobalConfig.GPU_SPEC_MAPPING.value()
        if (StringUtils.isEmpty(gpuMapping)) {
            return
        }
        Type type = new TypeToken<List<GpuSpec>>() {}.getType()
        Gson gson = new GsonBuilder().create()
        gpuSpecs = gson.fromJson(gpuMapping, type)
    }
}
