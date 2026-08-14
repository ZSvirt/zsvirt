package org.zstack.ipsec

import org.zstack.header.errorcode.ErrorCode
import org.zstack.ipsec.IPsecConnectionInventory

doc {

	title "IPSec连接清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.ipsec.APIAttachL3NetworksToIPsecConnectionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.ipsec.APIAttachL3NetworksToIPsecConnectionEvent.inventory"
		desc "null"
		type "IPsecConnectionInventory"
		since "2.3"
		clz IPsecConnectionInventory.class
	}
}
