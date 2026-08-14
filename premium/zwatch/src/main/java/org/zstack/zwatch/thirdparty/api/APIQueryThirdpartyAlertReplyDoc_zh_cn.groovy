package org.zstack.zwatch.thirdparty.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.thirdparty.entity.ThirdpartyOriginalAlertInventory

doc {

	title "查询第三方报警消息返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.thirdparty.api.APIQueryThirdpartyAlertReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.10"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.zwatch.thirdparty.api.APIQueryThirdpartyAlertReply.inventories"
		desc "null"
		type "List"
		since "3.10"
		clz ThirdpartyOriginalAlertInventory.class
	}
}
