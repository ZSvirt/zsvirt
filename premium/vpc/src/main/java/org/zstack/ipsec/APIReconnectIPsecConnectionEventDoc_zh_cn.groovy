package org.zstack.ipsec

import org.zstack.ipsec.IPsecConnectionInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "重连IPSec"

	ref {
		name "inventory"
		path "org.zstack.ipsec.APIReconnectIPsecConnectionEvent.inventory"
		desc "null"
		type "IPsecConnectionInventory"
		since "4.5"
		clz IPsecConnectionInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.5"
	}
	ref {
		name "error"
		path "org.zstack.ipsec.APIReconnectIPsecConnectionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.5"
		clz ErrorCode.class
	}
}
