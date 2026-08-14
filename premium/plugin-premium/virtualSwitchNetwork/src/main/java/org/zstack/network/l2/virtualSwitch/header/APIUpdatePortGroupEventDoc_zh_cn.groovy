package org.zstack.network.l2.virtualSwitch.header

import org.zstack.header.network.l3.L3NetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "端口组清单"

	ref {
		name "inventory"
		path "org.zstack.network.l2.virtualSwitch.header.APIUpdatePortGroupEvent.inventory"
		desc "null"
		type "L3NetworkInventory"
		since "4.2.0"
		clz L3NetworkInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APIUpdatePortGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.2.0"
		clz ErrorCode.class
	}
}
