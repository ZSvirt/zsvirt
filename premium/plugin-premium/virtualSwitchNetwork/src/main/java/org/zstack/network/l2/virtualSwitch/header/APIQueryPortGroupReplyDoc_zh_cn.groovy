package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.PortGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询端口组的请求返回"

	ref {
		name "inventories"
		path "org.zstack.network.l2.virtualSwitch.header.APIQueryPortGroupReply.inventories"
		desc "端口组清单列表"
		type "List"
		since "4.2.0"
		clz PortGroupInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APIQueryPortGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.2.0"
		clz ErrorCode.class
	}
}
