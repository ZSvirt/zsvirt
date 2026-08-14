package org.zstack.header.network.service

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.service.NetworkServiceL3NetworkRefInventory

doc {

	title "三层网络清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.network.service.APIQueryNetworkServiceL3NetworkRefReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.network.service.APIQueryNetworkServiceL3NetworkRefReply.inventories"
		desc "null"
		type "List"
		since "zsv 0.6"
		clz NetworkServiceL3NetworkRefInventory.class
	}
}
