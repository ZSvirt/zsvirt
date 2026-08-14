package org.zstack.network.securitygroup

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.securitygroup.VmNicSecurityGroupRefInventory

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
		path "org.zstack.network.securitygroup.APIQueryVmNicInSecurityGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.network.securitygroup.APIQueryVmNicInSecurityGroupReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz VmNicSecurityGroupRefInventory.class
	}
}
