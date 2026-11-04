package org.zstack.header.baremetal.instance

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.baremetal.instance.BaremetalInstanceInventory

doc {

	title "查询裸机实例返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.baremetal.instance.APIQueryBaremetalInstanceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_2.6.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.baremetal.instance.APIQueryBaremetalInstanceReply.inventories"
		desc "裸机实例清单"
		type "List"
		since "zsv_2.6.0"
		clz BaremetalInstanceInventory.class
	}
}
