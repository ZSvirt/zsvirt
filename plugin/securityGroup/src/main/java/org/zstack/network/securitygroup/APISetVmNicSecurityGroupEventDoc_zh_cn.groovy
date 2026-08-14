package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.VmNicSecurityGroupRefInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "安全组清单"

	ref {
		name "inventory"
		path "org.zstack.network.securitygroup.APISetVmNicSecurityGroupEvent.inventory"
		desc "安全组清单"
		type "List"
		since "3.17.21"
		clz VmNicSecurityGroupRefInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.network.securitygroup.APISetVmNicSecurityGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
