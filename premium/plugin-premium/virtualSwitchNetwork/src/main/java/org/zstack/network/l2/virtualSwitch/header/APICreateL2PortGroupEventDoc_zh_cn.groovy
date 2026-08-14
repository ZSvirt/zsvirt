package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建二层网络端口组结果"

	ref {
		name "inventory"
		path "org.zstack.network.l2.virtualSwitch.header.APICreateL2PortGroupEvent.inventory"
		desc "端口组二层网络清单"
		type "L2PortGroupNetworkInventory"
		since "3.17.0"
		clz L2PortGroupNetworkInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APICreateL2PortGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
