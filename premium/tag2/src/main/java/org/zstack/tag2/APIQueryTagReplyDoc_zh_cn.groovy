package org.zstack.tag2

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.tag.TagPatternInventory

doc {

	title "标签清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.tag.APIQueryTagReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.2.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.tag.APIQueryTagReply.inventories"
		desc "null"
		type "List"
		since "zsv_3.2.0"
		clz TagPatternInventory.class
	}
}
