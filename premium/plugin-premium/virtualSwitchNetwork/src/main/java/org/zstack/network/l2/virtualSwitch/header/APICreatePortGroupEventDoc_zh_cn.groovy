package org.zstack.network.l2.virtualSwitch.header

import org.zstack.header.network.l3.L3NetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建端口组的请求返回"

	ref {
		name "inventory"
		path "org.zstack.network.l2.virtualSwitch.header.APICreatePortGroupEvent.inventory"
		desc "端口组清单"
		type "L3NetworkInventory"
		since "4.2.0"
		clz L3NetworkInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APICreatePortGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.2.0"
		clz ErrorCode.class
	}
}
