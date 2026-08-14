package org.zstack.header.network.l3

import org.zstack.header.network.l3.ReservedIpRangeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "添加保留地址段清单"

	ref {
		name "inventory"
		path "org.zstack.header.network.l3.APIAddReservedIpRangeEvent.inventory"
		desc "null"
		type "ReservedIpRangeInventory"
		since "zsv 4.10.16"
		clz ReservedIpRangeInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.10.16"
	}
	ref {
		name "error"
		path "org.zstack.header.network.l3.APIAddReservedIpRangeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.16"
		clz ErrorCode.class
	}
}
