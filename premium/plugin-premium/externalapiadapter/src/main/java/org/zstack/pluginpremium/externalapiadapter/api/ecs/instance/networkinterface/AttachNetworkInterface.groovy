package org.zstack.pluginpremium.externalapiadapter.api.ecs.instance.networkinterface

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.zstack.header.vm.VmNicVO
import org.zstack.pluginpremium.externalapiadapter.EcsSystemTags
import org.zstack.pluginpremium.externalapiadapter.api.BaseAPI
import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException
import org.zstack.pluginpremium.externalapiadapter.typeconvertor.SecurityGroupUtils
import org.zstack.sdk.AttachVmNicToVmAction
import org.zstack.sdk.QuerySystemTagAction
import org.zstack.sdk.QueryVmNicAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_INSTANCE_ID
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ZSTACK_VM_INSTANCE_UUID

/**
 * Created by lining on 2018/5/20.
 */
class AttachNetworkInterface extends BaseAPI {
	@Override
	Class getZStackAction() {
		return AttachVmNicToVmAction.class
	}

	@Override
    void configAPIConversionSpec() {
        spec = config {
            convertAPIParam {
                simpleConvert {
                    ecsParamName = "NetworkInterfaceId"
                    zstackParamName = "vmNicUuid"
                }

				simpleConvert {
					ecsParamName = ECS_INSTANCE_ID
					zstackParamName = ZSTACK_VM_INSTANCE_UUID
				}
			}

			convertAPIResponse {
			}
		}
	}

	@Override
	Object callZStackAction() {
		Gson gson = new GsonBuilder().create()
		AttachVmNicToVmAction action = gson.fromJson(JSONObjectUtil.toJsonString(zstackAPIParamMap), AttachVmNicToVmAction.class)
		QueryVmNicAction queryVmNicAction = new QueryVmNicAction(
				sessionId: sessionId,
				conditions: [
						"uuid=${action.vmNicUuid}".toString(),
						"vmInstanceUuid=${action.vmInstanceUuid}".toString()
				]
		)
		QueryVmNicAction.Result queryVmNicResult = queryVmNicAction.call()
		queryVmNicResult.throwExceptionIfError()
		if (queryVmNicResult.value.inventories.size() != 0) {
			return null
		}
		AttachVmNicToVmAction.Result result = action.call()
		result.throwExceptionIfError()

		this.afterCallZStackAction(result)

		return result
	}

	@Override
	void afterCallZStackAction(Object zstackActionResult) {
		//vmInstance inventory in the result
		super.afterCallZStackAction(zstackActionResult)
		AttachVmNicToVmAction.Result result = zstackActionResult
		result.throwExceptionIfError()
		VmInstanceInventory vmInventory = result.value.inventory
		if (vmInventory == null) {
			throw new APIParamConvertException("Unknown", "Binding NIC to VM failed.")
		}
		List nics = vmInventory.vmNics
		addAddVmNicToSecurityGroup(nics)
	}

	private void addAddVmNicToSecurityGroup(List nics) {
		String nicId = ecsAPIParamMap.get("NetworkInterfaceId")
		String l3NetworkId = null

		//each instance only allowed to have one nic in each network
		l3NetworkId = nics.stream().filter({nic -> nic.uuid == nicId}).findAny().get().l3NetworkUuid

		QuerySystemTagAction querySystemTagAction = new QuerySystemTagAction(
				sessionId: sessionId,
				conditions: [
						"resourceUuid=$nicId".toString(),
						"resourceType=${VmNicVO.class.simpleName}".toString()
				]
		)

		QuerySystemTagAction.Result querySystemTagResult = querySystemTagAction.call()
		querySystemTagResult.throwExceptionIfError()

		// use multiple tags instead of one tag with list
		List securityGroupTag = []

		querySystemTagResult.value.inventories.each {
			String tag = it.tag
			if (EcsSystemTags.SECURITYGROUP_ID.isMatch(tag)) {
				securityGroupTag.add(EcsSystemTags.SECURITYGROUP_ID.getTokenByTag(tag, EcsSystemTags.SECURITYGROUP_ID_TOKEN))
			}
		}

		if (securityGroupTag.size() == 0) {
			return
		}

		if (l3NetworkId == null) {
			logger.debug("L3NetworkUuid missed")
			return
		}

		securityGroupTag.each { String securityGroupId ->
			SecurityGroupUtils.addVmNicToSecurityGroup(sessionId, securityGroupId, l3NetworkId, nicId)
		}
	}
}
