package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询端口组二层网络的请求返回"

	ref {
		name "inventories"
		path "org.zstack.network.l2.virtualSwitch.header.APIQueryL2PortGroupNetworkReply.inventories"
		desc "端口组二层网络清单列表"
		type "List"
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
		path "org.zstack.network.l2.virtualSwitch.header.APIQueryL2PortGroupNetworkReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
