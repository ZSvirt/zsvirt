package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.UplinkGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新上行链路组的请求返回"

	ref {
		name "inventory"
		path "org.zstack.network.l2.virtualSwitch.header.APIUpdateVirtualSwitchUplinkGroupEvent.inventory"
		desc "上行链路组清单"
		type "UplinkGroupInventory"
		since "4.3.0"
		clz UplinkGroupInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.3.0"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APIUpdateVirtualSwitchUplinkGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.3.0"
		clz ErrorCode.class
	}
}
