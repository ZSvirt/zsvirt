package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.SecurityGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改安全组规则状态结果"

	ref {
		name "inventory"
		path "org.zstack.network.securitygroup.APIChangeSecurityGroupRuleStateEvent.inventory"
		desc "安全组清单"
		type "SecurityGroupInventory"
		since "3.17.21"
		clz SecurityGroupInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.network.securitygroup.APIChangeSecurityGroupRuleStateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
