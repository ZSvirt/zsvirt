package org.zstack.pluginpremium.externalapiadapter.typeconvertor;

import org.zstack.pluginpremium.externalapiadapter.exception.APIParamConvertException;
import org.zstack.sdk.AddVmNicToSecurityGroupAction;
import org.zstack.sdk.AttachSecurityGroupToL3NetworkAction;
import org.zstack.sdk.QuerySecurityGroupAction;
import org.zstack.sdk.SecurityGroupInventory;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: fubang
 * @Date: 2018/5/25
 */
public class SecurityGroupUtils {
	public static void addVmNicToSecurityGroup(String sessionId, String securityGroupId, String l3NetworkId, String nicId){
		QuerySecurityGroupAction querySecurityGroupAction = new QuerySecurityGroupAction();
		querySecurityGroupAction.sessionId = sessionId;
		querySecurityGroupAction.conditions = Arrays.asList(String.format("uuid=%s", securityGroupId));
		QuerySecurityGroupAction.Result querySecurityGroupResult = querySecurityGroupAction.call();
		querySecurityGroupResult.throwExceptionIfError();

		List inventories = querySecurityGroupResult.value.inventories;
		if (inventories.size() == 0){
			throw new APIParamConvertException("SecurityGroupId", String.format("Not found securityGroup[uuid:%s]", securityGroupId));
		}
		SecurityGroupInventory inventory = (SecurityGroupInventory) inventories.get(0);

		//security group and nic should under the same vpc, and this check should be done when nic is created
		if (!inventory.attachedL3NetworkUuids.contains(l3NetworkId)) {
			AttachSecurityGroupToL3NetworkAction attachSecurityGroupToL3NetworkAction = new AttachSecurityGroupToL3NetworkAction();
			attachSecurityGroupToL3NetworkAction.sessionId = sessionId;
			attachSecurityGroupToL3NetworkAction.securityGroupUuid = inventory.uuid;
			attachSecurityGroupToL3NetworkAction.l3NetworkUuid = l3NetworkId;

			AttachSecurityGroupToL3NetworkAction.Result attachSecurityGroupToL3NetworkResult = attachSecurityGroupToL3NetworkAction.call();
			attachSecurityGroupToL3NetworkResult.throwExceptionIfError();

//			throw new APIParamConvertException("SecurityGroupId", String.format("SecurityGroup[id:%s] and NetworkInterface[id:%s] not in the same vpc", securityGroupId, nicId));
		}

		AddVmNicToSecurityGroupAction action = new AddVmNicToSecurityGroupAction();
		action.sessionId = sessionId;
		action.securityGroupUuid = inventory.uuid;
		action.vmNicUuids = Arrays.asList(nicId);

		AddVmNicToSecurityGroupAction.Result result = action.call();
		result.throwExceptionIfError();
	}
}
