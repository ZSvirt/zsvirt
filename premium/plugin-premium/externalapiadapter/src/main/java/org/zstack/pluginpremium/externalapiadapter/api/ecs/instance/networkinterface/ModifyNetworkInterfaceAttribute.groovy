package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance.networkinterface

import org.apache.commons.lang.StringUtils
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.sdk.AddVmNicToSecurityGroupAction
import org.zstack.sdk.DeleteVmNicFromSecurityGroupAction
import org.zstack.sdk.QuerySystemTagAction
import org.zstack.sdk.QueryVmNicAction
import org.zstack.sdk.SystemTagInventory
import org.zstack.sdk.UpdateSystemTagAction
import org.zstack.sdk.VmNicInventory

/**
 * Created by lining on 2018/5/20.
 */
class ModifyNetworkInterfaceAttribute extends BaseAPI {
	@Override
	Class getZStackAction() {
		return null
	}

	@Override
	void configAPIConversionSpec() {
		spec = config {
			convertAPIParam {
			}

			convertAPIResponse {
			}
		}
	}

	@Override
	Object callZStackAction() {
		String vmnic = ecsAPIParamMap.get("NetworkInterfaceId")
		// cancel querySystemTag when securityGroup parameter is null
		def includeSecurityGroup = false
		def newTagValue = []
		for (i in 1..100) {
			String securityGroupId = ecsAPIParamMap.get("SecurityGroupId.$i".toString())
			if (securityGroupId == null) {
				break
			}

			includeSecurityGroup = true
			newTagValue.add(securityGroupId)
		}

		if (!includeSecurityGroup) {
			return null
		}

		QuerySystemTagAction querySystemTagAction = new QuerySystemTagAction(
				sessionId: sessionId,
				conditions: ["resourceUuid=$vmnic".toString(), "resourceType=VmNicVO", "tag~=${EcsSystemTags.SECURITYGROUP_ID_TOKEN}::%".toString()]
		)
		QuerySystemTagAction.Result querySystemTagResult = querySystemTagAction.call()
		querySystemTagResult.throwExceptionIfError()

		def tags = querySystemTagResult.value.inventories
		if (tags.size() == 0) {
			logger.debug("Not found systemTag about vmNic[uuid: $vmnic]".toString())
			return
		}

		SystemTagInventory tagInventory = querySystemTagResult.value.inventories.get(0) as SystemTagInventory
		String securityGroupToken = EcsSystemTags.SECURITYGROUP_ID.getTokenByTag(tagInventory.tag, EcsSystemTags.SECURITYGROUP_ID_TOKEN)
		List<String> originSecurityGroups = securityGroupToken.split(EcsSystemTags.SECURITYGROUP_SPLIT_CHAR)

		QueryVmNicAction action = new QueryVmNicAction(
				sessionId: sessionId,
				conditions: ["uuid=$vmnic".toString()]
		)

		QueryVmNicAction.Result result = action.call()
		VmNicInventory vmNicInventory = result.value.inventories?.get(0) as VmNicInventory
		if (vmNicInventory.vmInstanceUuid != null) {
			originSecurityGroups.each {
				DeleteVmNicFromSecurityGroupAction deleteVmNicFromSecurityGroupAction = new DeleteVmNicFromSecurityGroupAction(
						sessionId: sessionId,
						securityGroupUuid: it,
						vmNicUuids: [vmnic]
				)
				DeleteVmNicFromSecurityGroupAction.Result deleteVmNicFromSecurityGroupResult = deleteVmNicFromSecurityGroupAction.call()
				deleteVmNicFromSecurityGroupResult.throwExceptionIfError()
			}

			newTagValue.each {
				AddVmNicToSecurityGroupAction addVmNicToSecurityGroupAction = new AddVmNicToSecurityGroupAction(
						sessionId: sessionId,
						securityGroupUuid: it,
						vmNicUuids: [vmnic]
				)
				AddVmNicToSecurityGroupAction.Result addVmNicToSecurityGroupResult = addVmNicToSecurityGroupAction.call()
				addVmNicToSecurityGroupResult.throwExceptionIfError()
			}
		}

		UpdateSystemTagAction updateSystemTagAction = new UpdateSystemTagAction(
				sessionId: sessionId,
				uuid: tagInventory.uuid,
				tag: EcsSystemTags.SECURITYGROUP_ID.instantiateTag([(EcsSystemTags.SECURITYGROUP_ID_TOKEN): StringUtils.join(newTagValue, EcsSystemTags.SECURITYGROUP_SPLIT_CHAR)])
		)
		UpdateSystemTagAction.Result updateSystemTagResult = updateSystemTagAction.call()
		updateSystemTagResult.throwExceptionIfError()

		return null
	}
}
