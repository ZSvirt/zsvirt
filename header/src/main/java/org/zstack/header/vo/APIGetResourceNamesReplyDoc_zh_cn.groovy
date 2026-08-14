package org.zstack.header.vo

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vo.ResourceInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vo.APIGetResourceNamesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 2.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.vo.APIGetResourceNamesReply.inventories"
		desc "资源结构，包含UUID、名称和类型字段"
		type "List"
		since "zsv 2.0"
		clz ResourceInventory.class
	}
}
