package org.zstack.network.l2.virtualSwitch.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建二层网络虚拟交换机结果"

	ref {
		name "inventory"
		path "org.zstack.network.l2.virtualSwitch.header.APICreateL2VirtualSwitchEvent.inventory"
		desc "虚拟交换机清单"
		type "L2VirtualSwitchNetworkInventory"
		since "3.17.0"
		clz L2VirtualSwitchNetworkInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APICreateL2VirtualSwitchEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
