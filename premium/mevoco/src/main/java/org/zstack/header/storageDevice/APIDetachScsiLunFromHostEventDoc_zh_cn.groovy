package org.zstack.header.storageDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storageDevice.ScsiLunInventory

doc {

	title "从Host卸载LUN设备结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.11.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storageDevice.APIDetachScsiLunFromHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.11.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.storageDevice.APIDetachScsiLunFromHostEvent.inventory"
		desc "被卸载LUN设备清单"
		type "ScsiLunInventory"
		since "3.11.6"
		clz ScsiLunInventory.class
	}
}
