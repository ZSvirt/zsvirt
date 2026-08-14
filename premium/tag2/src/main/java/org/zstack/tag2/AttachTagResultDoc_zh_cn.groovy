package org.zstack.tag2

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.tag.UserTagInventory

doc {

	title "加载标签到资源的结果"

	ref {
		name "error"
		path "org.zstack.tag2.AttachTagResult.error"
		desc "错误信息"
		type "ErrorCode"
		since "3.2.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.tag2.AttachTagResult.inventory"
		desc "加载的标签"
		type "UserTagInventory"
		since "3.2.0"
		clz UserTagInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.2.0"
	}
}
