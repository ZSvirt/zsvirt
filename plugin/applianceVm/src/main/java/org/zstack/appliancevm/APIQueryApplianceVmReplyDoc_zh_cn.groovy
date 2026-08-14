package org.zstack.appliancevm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.appliancevm.ApplianceVmInventory

doc {

	title "系统云主机清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.appliancevm.APIQueryApplianceVmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.appliancevm.APIQueryApplianceVmReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz ApplianceVmInventory.class
	}
}
