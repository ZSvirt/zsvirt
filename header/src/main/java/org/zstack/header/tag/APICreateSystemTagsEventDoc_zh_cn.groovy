package org.zstack.header.tag

import org.zstack.header.tag.SystemTagInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建标签返回"

	ref {
		name "inventories"
		path "org.zstack.header.tag.APICreateSystemTagsEvent.inventories"
		desc "标签清单列表"
		type "List"
		since "zsv 3.17.0"
		clz SystemTagInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.tag.APICreateSystemTagsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.17.0"
		clz ErrorCode.class
	}
}
