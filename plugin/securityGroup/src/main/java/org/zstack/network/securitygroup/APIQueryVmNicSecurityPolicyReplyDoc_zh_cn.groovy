package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.VmNicSecurityPolicyInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询虚拟机网卡安全组策略的请求返回"

	ref {
		name "inventories"
		path "org.zstack.network.securitygroup.APIQueryVmNicSecurityPolicyReply.inventories"
		desc "虚拟机网卡安全组策略清单列表"
		type "List"
		since "3.17.21"
		clz VmNicSecurityPolicyInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.network.securitygroup.APIQueryVmNicSecurityPolicyReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
