package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.NvmeServerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询NVMe服务器结果"

	ref {
		name "inventories"
		path "org.zstack.storage.device.nvme.APIQueryNvmeServerReply.inventories"
		desc "NVMe服务器清单列表"
		type "List"
		since "3.17.21"
		clz NvmeServerInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.nvme.APIQueryNvmeServerReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
