package org.zstack.header.zone

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.zone.ZoneInventory

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
		path "org.zstack.header.zone.APIGetZoneReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.zone.APIGetZoneReply.inventories"
		desc "null"
		type "List"
		since "zsv 0.6"
		clz ZoneInventory.class
	}
}
