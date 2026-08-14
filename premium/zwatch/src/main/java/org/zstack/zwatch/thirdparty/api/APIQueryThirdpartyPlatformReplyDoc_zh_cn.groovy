package org.zstack.zwatch.thirdparty.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformInventory

doc {

	title "查询第三方报警源返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.thirdparty.api.APIQueryThirdpartyPlatformReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.10"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.zwatch.thirdparty.api.APIQueryThirdpartyPlatformReply.inventories"
		desc "null"
		type "List"
		since "3.10"
		clz ThirdpartyPlatformInventory.class
	}
}
