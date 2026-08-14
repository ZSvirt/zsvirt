package org.zstack.header.baremetal.instance

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.baremetal.instance.BaremetalInstanceInventory

doc {

	title "关闭裸机实例返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.baremetal.instance.APIStopBaremetalInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.baremetal.instance.APIStopBaremetalInstanceEvent.inventory"
		desc "裸机实例清单"
		type "BaremetalInstanceInventory"
		since "2.6.0"
		clz BaremetalInstanceInventory.class
	}
}
