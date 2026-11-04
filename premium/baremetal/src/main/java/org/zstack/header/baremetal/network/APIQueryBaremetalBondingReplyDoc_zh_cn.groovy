package org.zstack.header.baremetal.network

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.baremetal.network.BaremetalBondingInventory

doc {

	title "查询裸金属网卡绑定返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.baremetal.network.APIQueryBaremetalBondingReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.baremetal.network.APIQueryBaremetalBondingReply.inventories"
		desc "裸金属网卡绑定清单"
		type "List"
		since "zsv_3.4.0"
		clz BaremetalBondingInventory.class
	}
}
