package org.zstack.loginControl.api

import org.zstack.loginControl.entity.AccessControlRuleInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询IP访问控制规则返回"

	ref {
		name "inventories"
		path "org.zstack.loginControl.api.APIQueryAccessControlRuleReply.inventories"
		desc "null"
		type "List"
		since "zsv_3.5.1"
		clz AccessControlRuleInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_3.5.1"
	}
	ref {
		name "error"
		path "org.zstack.loginControl.api.APIQueryAccessControlRuleReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.5.1"
		clz ErrorCode.class
	}
}
