package org.zstack.twoFactorAuthentication

import org.zstack.header.errorcode.ErrorCode
import org.zstack.twoFactorAuthentication.TwoFactorAuthenticationSecretInventory

doc {

	title "查询双因子认证密匙的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.twoFactorAuthentication.APIQueryTwoFactorAuthenticationReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.twoFactorAuthentication.APIQueryTwoFactorAuthenticationReply.inventories"
		desc "null"
		type "List"
		since "4.10.0"
		clz TwoFactorAuthenticationSecretInventory.class
	}
}
