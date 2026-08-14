package org.zstack.header.identity

import org.zstack.header.errorcode.ErrorCode

doc {

	title "账户资源引用清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.identity.APIChangeResourceOwnerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.identity.APIChangeResourceOwnerEvent.inventory"
		desc "账户资源引用清单"
		type "AccountResourceRefInventory"
		since "zsv 0.6"
		clz AccountResourceRefInventory.class
	}
}
