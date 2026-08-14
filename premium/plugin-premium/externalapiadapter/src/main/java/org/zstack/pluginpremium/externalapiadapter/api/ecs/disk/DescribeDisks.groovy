package org.zstack.pluginpremium.externalapiadapter.api.ecs.disk

import org.apache.commons.lang.StringUtils
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeVO
import org.zstack.kvm.KVMSystemTags
import org.zstack.mevoco.MevocoSystemTags
import org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterUtils
import org.zstack.pluginpremium.externalapiadapter.api.BaseQueryAPI
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.DiskStatus
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.DiskType
import org.zstack.sdk.*
import org.zstack.utils.data.SizeUnit

import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.*

/**
 * @Author: fubang
 * @Date: 2018/4/23
 */
class DescribeDisks extends BaseQueryAPI {
    @Override
    void configAPIConversionSpec() {
        spec = config {
            convertQueryAPIParam {
                beforeZstackAPIParam = { zstackParamMap ->
                    zstackParamMap.put(ZSTACK_QUERY_CONDITIONS_KEY, [])
                    zstackParamMap.put(ZSTACK_QUERY_REPLYWITHCOUNT_KEY, true)
                }

                querySimpleConvert {
                    ecsParamName = ECS_API_ZONEID_KEY
                    zstackParamName = "primaryStorage.cluster.name"
                }

                querySimpleConvert {
                    ecsParamName = ECS_DISK_IDS
                    ecsParamType = ArrayList.class
                    zstackParamName = ZSTACK_UUID
                    zstackParamType = String.class
                    stillConvertParamWhenEcsParamValueIsNull = true

                    putZstackParamValue = { zstackParamMap, zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY)
                        if (zstackParamValue != null) {
                            String uuids = StringUtils.join(zstackParamValue, ",")
                            conditions.add("$zstackParamName?=$uuids".toString())
                            return
                        }

                        QueryApplianceVmAction queryVpcRouter = new QueryApplianceVmAction(
                                sessionId: sessionId
                        )
                        QueryApplianceVmAction.Result vpcRouterResult = queryVpcRouter.call()
                        if (vpcRouterResult.error != null || vpcRouterResult.value.inventories.size() == 0) {
                            return
                        }
                        List<ApplianceVmInventory> vpcRouters = vpcRouterResult.value.inventories
                        List<String> routerDisks = vpcRouters.stream().flatMap({ ApplianceVmInventory router ->
                            router.allVolumes.stream()
                        }).map({ VolumeInventory disk ->
                            disk.uuid
                        }).collect(Collectors.toList())
                        String routerDisksStr = StringUtils.join(routerDisks, ",")
                        conditions.add("$zstackParamName!?=$routerDisksStr".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_INSTANCE_ID
                    zstackParamName = ZSTACK_VM_INSTANCE_UUID
                }

                querySimpleConvert {
                    ecsParamName = ECS_DISK_TYPE
                    zstackParamName = ZSTACK_API_TYPE_KEY
                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        if (zstackParamValue == "all"){
                            return
                        }

                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        String val = DiskType.getDiskTypeFromEcs(zstackParamValue).zstackValue
                        conditions.add("$zstackParamName=$val".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_DISK_PORTABLE
                    zstackParamName = ZSTACK_API_TYPE_KEY
                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        String val = DiskType.getDiskTypeFromPortable(zstackParamValue).zstackValue
                        conditions.add("$zstackParamName=$val".toString())
                    }
                }

                querySimpleConvert {
                    ecsParamName = ECS_DISK_NAME
                    zstackParamName = ZSTACK_NAME
                }

                querySimpleConvert {
                    ecsParamName = ECS_DISK_ENABLE_SHARED
                    zstackParamName = ZSTACK_DISK_ENABLE_SHARED
                    stillConvertParamWhenEcsParamValueIsNull = true
                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        if (zstackParamValue == null) {
                            zstackParamValue = "false"
                        }
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        conditions.add("${zstackParamName}=${zstackParamValue}".toString())
                    }
                }

                simpleConvert {
                    ecsParamName = ECS_API_STATUS_KEY
                    zstackParamName = ZSTACK_API_STATUS_KEY

                    stillConvertParamWhenEcsParamValueIsNull = true

                    putZstackParamValue = {Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        String val
                        if (zstackParamValue != null) {
                            val = DiskStatus.addConditionsWhenQuery(conditions, zstackParamValue).zstackValue
                        }

                        if (val != null) {
                            conditions.add("$zstackParamName=$val".toString())
                            return
                        }

                        val = "${VolumeStatus.Deleted.toString()},${VolumeStatus.Migrating.toString()}".toString()
                        conditions.add("$zstackParamName!?=$val".toString())
                    }
                }

                zstackNeedParam {
                    zstackParamName = ZSTACK_API_STATE_KEY
                    getZstackValue = { Map ecsParamMap, Map zstackParamMap ->
                        return null
                    }
                    putZstackParamValue = { Map zstackParamMap, String zstackParamValue ->
                        List conditions = zstackParamMap.get(ZSTACK_QUERY_CONDITIONS_KEY) as List
                        conditions.add("${zstackParamName}=Enabled".toString())
                    }
                }

            }

            convertQueryAPIResponse {
                convertResponseAttribute {
                    ecsAttributeName = ECS_DISKS
                    ecsAttributeValue = new HashMap<>()
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, ecsAttributeValue)
                    }

                    convertList {
                        ecsAttributeName = ECS_DISK
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
                                    ecsAttributeName = ECS_DISK_ID

                                    zstackAttributeValue = elementZstackValue.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_ZONEID_KEY

                                    zstackAttributeValue = elementZstackValue.primaryStorageUuid

                                    addEcsValueToFather = { fatherValue ->
                                        String zoneId = ecsAPIParamMap.get(ecsAttributeName)
                                        if (zoneId != null) {
                                            fatherValue.put(ecsAttributeName, zoneId)
                                            return
                                        }
                                        if (zstackAttributeValue == null) {
                                            return fatherValue.put(ecsAttributeName, null)
                                        }
                                        QueryPrimaryStorageAction action = new QueryPrimaryStorageAction(
                                                sessionId: sessionId,
                                                conditions: ["uuid=${zstackAttributeValue}".toString()]
                                        )
                                        QueryPrimaryStorageAction.Result result = action.call()
//                                        result.throwExceptionIfError()
                                        if (result.error == null && result.value.inventories.size() != 0) {
                                            if (result.value.inventories.first().attachedClusterUuids.size() == 1) {
                                                QueryClusterAction clusterAction = new QueryClusterAction(
                                                        sessionId: sessionId,
                                                        conditions: ["uuid=${result.value.inventories.first().attachedClusterUuids.first()}".toString()]
                                                )
                                                QueryClusterAction.Result clusterResult = clusterAction.call()
                                                clusterResult.throwExceptionIfError()
                                                if (clusterResult.value.inventories.size() != 0) {
                                                    fatherValue.put(ecsAttributeName, clusterResult.value.inventories.first().name)
                                                    return
                                                }
                                            }
                                        }
                                        return fatherValue.put(ecsAttributeName, null)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_REGIONID_KEY

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, ecsAPIParamMap.get(ecsAttributeName))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DISK_NAME

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
                                    ecsAttributeName = ECS_DISK_CATEGORY
                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, SUPPORT_RESOURCE_INFO.DISK_CATEGORY)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DISK_ENCRYPTED
                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, false)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DISK_DELETE_AUTO_SNAPSHOT
                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, true)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DISK_DELETEWITHINSTANCE

                                    zstackAttributeValue = elementZstackValue.type

                                    addEcsValueToFather = { Map parentMap ->
                                        if (DiskType.SYSTEM.toString() == zstackAttributeValue) {
                                            parentMap[ecsAttributeName] = true
                                        } else {
                                            QuerySystemTagAction queryTag = new QuerySystemTagAction(
                                                    sessionId: sessionId,
                                                    conditions: [
                                                            "resourceUuid=${elementZstackValue.uuid}".toString(),
                                                            "resourceType=${VolumeVO.class.getSimpleName()}".toString(),
                                                            "tag=${ECS_DISK_DELETEWITHINSTANCE_TAG}".toString()
                                                    ]
                                            )
                                            QuerySystemTagAction.Result tagResult = queryTag.call()
                                            tagResult.throwExceptionIfError()
                                            parentMap[ecsAttributeName] = tagResult.value.inventories.size() != 0
                                        }

                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_TYPE_KEY

                                    zstackAttributeValue = elementZstackValue.type

                                    addEcsValueToFather = { fatherValue ->
                                        DiskType diskType = DiskType.getDiskTypeFromZstack(zstackAttributeValue)
                                        def value = diskType.ecsValue
                                        fatherValue.put(ecsAttributeName, value)
                                        fatherValue.put(ECS_DISK_PORTABLE, diskType.portable)
                                    }
                                }
                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_SIZE

                                    zstackAttributeValue = elementZstackValue.size

                                    addEcsValueToFather = { fatherValue ->
                                        long size = SizeUnit.BYTE.toGigaByte(zstackAttributeValue)
                                        size = size == 0L && zstackAttributeValue != 0 ? 1 : size
                                        fatherValue.put(ecsAttributeName, size)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_IMAGE_ID

                                    zstackAttributeValue = elementZstackValue.rootImageUuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }


                                convertResponseAttribute {
                                    ecsAttributeName = ECS_API_STATUS_KEY

                                    zstackAttributeValue = elementZstackValue.status

                                    addEcsValueToFather = { fatherValue ->

                                        fatherValue.put(ecsAttributeName, DiskStatus.convertDiskStatusZstackIntoEcs(elementZstackValue as VolumeInventory))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_INSTANCE_ID

                                    zstackAttributeValue = elementZstackValue.vmInstanceUuid

                                    addEcsValueToFather = { fatherValue ->
                                        fatherValue.put(ecsAttributeName, zstackAttributeValue)
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
                                    ecsAttributeName = ECS_SNAPSHOT_AUTO_POLICY_ID

                                    zstackAttributeValue = elementZstackValue.uuid

                                    addEcsValueToFather = { Map parentMap ->
                                        QuerySchedulerJobAction queryASP = new QuerySchedulerJobAction(
                                                sessionId: sessionId,
                                                conditions: ["targetResourceUuid=${zstackAttributeValue}".toString()]
                                        )
                                        QuerySchedulerJobAction.Result result = queryASP.call()

                                        String autoSnapshotPolicyUuid = null
                                        try {
                                            autoSnapshotPolicyUuid = result.value.inventories.first().triggersUuid.first()
                                        } catch (Exception ignore) {
                                        }

                                        if (autoSnapshotPolicyUuid != null) {
                                            parentMap.put(ecsAttributeName, autoSnapshotPolicyUuid)
                                            parentMap.put(ECS_SNAPSHOT_ENABLED_POLICY, true)
                                        } else {
                                            parentMap.put(ECS_SNAPSHOT_ENABLED_POLICY, false)
                                        }
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DISK_ENABLE_AUTO_SNAPSHOT

                                    zstackAttributeValue = true

                                    addEcsValueToFather = {Map parentMap ->
                                        parentMap.put(ecsAttributeName, zstackAttributeValue)
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DISK_DEVICE

                                    zstackAttributeValue = elementZstackValue.uuid

                                    addEcsValueToFather = { fatherValue ->
                                        QuerySystemTagAction action = new QuerySystemTagAction(
                                                sessionId: sessionId,
                                                conditions: ["resourceUuid=$zstackAttributeValue".toString(), "resourceType=VolumeVO"]
                                        )
                                        QuerySystemTagAction.Result result = action.call()
                                        result.throwExceptionIfError()

                                        SystemTagInventory systemTagInventory = result.value.inventories.find { it.tag.indexOf("kvm::volume::") >= 0 }
                                        if (systemTagInventory == null){
                                            logger.debug("Not found SystemTag")
                                            return
                                        }
                                        fatherValue.put(ecsAttributeName, KVMSystemTags.VOLUME_WWN.getTokenByTag(systemTagInventory.tag, KVMSystemTags.VOLUME_WWN_TOKEN))
                                    }
                                }

                                convertResponseAttribute {
                                    ecsAttributeName = ECS_DISK_IOPS

                                    zstackAttributeValue = elementZstackValue.diskOfferingUuid

                                    addEcsValueToFather = { fatherValue ->
                                        String resourceUuid = zstackAttributeValue
                                        if (resourceUuid == null){
                                            String vmInstanceUuid = elementZstackValue.vmInstanceUuid
                                            QueryVmInstanceAction queryVmInstanceAction = new QueryVmInstanceAction(
                                                    sessionId: sessionId,
                                                    conditions: ["uuid=$vmInstanceUuid".toString()]
                                            )
                                            QueryVmInstanceAction.Result queryVmInstanceResult = queryVmInstanceAction.call()
                                            queryVmInstanceResult.throwExceptionIfError()

                                            List vmInventories = queryVmInstanceResult.value.inventories
                                            if (vmInventories.size() == 0){
                                                logger.debug("Not found vmInstance [uuid: $vmInstanceUuid]".toString())
                                                return
                                            }
                                            VmInstanceInventory vmInstanceInventory = vmInventories.get(0) as VmInstanceInventory
                                            resourceUuid = vmInstanceInventory.instanceOfferingUuid
                                        }

                                        QuerySystemTagAction action = new QuerySystemTagAction(
                                                sessionId: sessionId,
                                                conditions: ["resourceUuid=$resourceUuid".toString()]
                                        )
                                        QuerySystemTagAction.Result result = action.call()
                                        result.throwExceptionIfError()

                                        List inventories = result.value.inventories
                                        if (inventories.size() == 0) {
                                            logger.debug("Not found SystemTag")
                                            return
                                        }

                                        SystemTagInventory iops = result.value.inventories.find {
                                            it.tag.indexOf("volumeTotalBandwidth::") >= 0
                                        }
                                        if (iops == null) {
                                            logger.debug("Not found SystemTag [value like volumeTotalBandwidth]")
                                            return
                                        }

                                        String iopsValue = MevocoSystemTags._VOLUME_TOTAL_BANDWIDTH.getTokenByTag(iops.tag, MevocoSystemTags.VOLUME_TOTAL_BANDWIDTH_TOKEN)
                                        fatherValue.put(ecsAttributeName, iopsValue)

                                    }
                                }
                            }
                        }
                    }

                }

                convertResponseAttribute {
                    ecsAttributeName = ECS_DISK_ENCRYPTED
                    addEcsValueToEcsAPIRsp = { ecsAPIRsp ->
                        ecsAPIRsp.put(ecsAttributeName, false)
                    }
                }
            }
        }
    }

    @Override
    void afterGetEcsApiRsp(Map ecsApiRsp) {
        if (!ecsAPIParamMap.containsKey(ECS_SNAPSHOT_ENABLED_POLICY)) {
            return
        }
        List disks = ecsApiRsp.get(ECS_DISKS) as List
        boolean enabledAutoSnapshotPolicy = ecsAPIParamMap.get(ECS_SNAPSHOT_ENABLED_POLICY)
        disks = disks.stream().filter { Map disk ->
            disk.get(ECS_SNAPSHOT_ENABLED_POLICY) == enabledAutoSnapshotPolicy
        }.collect(Collectors.toList())
        if (ecsAPIParamMap.containsKey(ECS_SNAPSHOT_AUTO_POLICY_ID)) {
            String autoSnapshotPolicyId = ecsAPIParamMap.get(ECS_SNAPSHOT_AUTO_POLICY_ID)
            disks = disks.stream().filter { Map disk ->
                disk.get(ECS_SNAPSHOT_AUTO_POLICY_ID) == autoSnapshotPolicyId
            }.collect(Collectors.toList())
        }
        ecsApiRsp.put(ECS_DISKS, disks)
    }

    @Override
    Class getZStackAction() {
        return QueryVolumeAction.class
    }
}
