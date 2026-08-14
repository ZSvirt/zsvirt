package org.zstack.network.securitygroup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.securitygroup.SecurityGroupInventory

doc {

	title "安全组（Security Group）清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.network.securitygroup.APIDeleteSecurityGroupRuleEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.network.securitygroup.APIDeleteSecurityGroupRuleEvent.inventory"
		desc "null"
		type "SecurityGroupInventory"
		since "0.6"
		clz SecurityGroupInventory.class
	}
}
