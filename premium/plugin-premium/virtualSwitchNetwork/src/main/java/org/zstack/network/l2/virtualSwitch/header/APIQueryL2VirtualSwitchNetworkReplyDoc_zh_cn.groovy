package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.L2VirtualSwitchNetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询虚拟交换机的请求返回"

	ref {
		name "inventories"
		path "org.zstack.network.l2.virtualSwitch.header.APIQueryL2VirtualSwitchNetworkReply.inventories"
		desc "虚拟交换机清单列表"
		type "List"
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
		path "org.zstack.network.l2.virtualSwitch.header.APIQueryL2VirtualSwitchNetworkReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
