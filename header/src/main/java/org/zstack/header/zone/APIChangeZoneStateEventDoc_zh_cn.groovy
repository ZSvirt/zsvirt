package org.zstack.header.zone

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.zone.ZoneInventory

doc {

	title "区域（Zone）清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.zone.APIChangeZoneStateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.zone.APIChangeZoneStateEvent.inventory"
		desc "null"
		type "ZoneInventory"
		since "zsv 0.6"
		clz ZoneInventory.class
	}
}
