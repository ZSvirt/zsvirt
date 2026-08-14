package org.zstack.mttyDevice

import org.zstack.mttyDevice.MttyDeviceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "MTTY 设备查询结果"

	ref {
		name "inventories"
		path "org.zstack.mttyDevice.APIQueryMttyDeviceReply.inventories"
		desc "MTTY 设备清单列表"
		type "List"
		since "3.15.11"
		clz MttyDeviceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.15.11"
	}
	ref {
		name "error"
		path "org.zstack.mttyDevice.APIQueryMttyDeviceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.15.11"
		clz ErrorCode.class
	}
}
