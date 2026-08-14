package org.zstack.pciDevice.virtual.sr_iov

import org.zstack.header.sriov.EthernetVfPciDeviceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询网卡VF结果"

	ref {
		name "inventories"
		path "org.zstack.pciDevice.virtual.sr_iov.APIQueryEthernetVFReply.inventories"
		desc "网卡 VF 列表"
		type "List"
		since "3.18.0"
		clz EthernetVfPciDeviceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.18.0"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.virtual.sr_iov.APIQueryEthernetVFReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.18.0"
		clz ErrorCode.class
	}
}
