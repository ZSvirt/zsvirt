package org.zstack.header.acl

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.acl.AccessControlListInventory

doc {

	title "查询访问控制策略组清单的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.9"
	}
	ref {
		name "error"
		path "org.zstack.header.acl.APIQueryAccessControlListReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.9"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.acl.APIQueryAccessControlListReply.inventories"
		desc "访问控制策略组清单列表"
		type "List"
		since "3.9"
		clz AccessControlListInventory.class
	}
}
