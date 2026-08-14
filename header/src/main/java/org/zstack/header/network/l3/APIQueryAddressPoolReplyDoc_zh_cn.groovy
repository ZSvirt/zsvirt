package org.zstack.header.network.l3

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l3.AddressPoolInventory

doc {

	title "IP地址池清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.network.l3.APIQueryAddressPoolReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.9"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.network.l3.APIQueryAddressPoolReply.inventories"
		desc "null"
		type "List"
		since "zsv 3.9"
		clz AddressPoolInventory.class
	}
}
