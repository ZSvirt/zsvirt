package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.VmNicSecurityPolicyInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更改网卡的默认流量策略结果"

	ref {
		name "inventory"
		path "org.zstack.network.securitygroup.APIChangeVmNicSecurityPolicyEvent.inventory"
		desc "虚拟机网卡安全策略清单"
		type "VmNicSecurityPolicyInventory"
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
		path "org.zstack.network.securitygroup.APIChangeVmNicSecurityPolicyEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
