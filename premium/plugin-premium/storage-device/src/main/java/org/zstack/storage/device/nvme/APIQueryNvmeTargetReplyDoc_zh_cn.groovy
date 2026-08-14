package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.NvmeTargetInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询 NVMe 设备结果"

	ref {
		name "inventories"
		path "org.zstack.storage.device.nvme.APIQueryNvmeTargetReply.inventories"
		desc "NVMe 设备清单列表"
		type "List"
		since "3.16.21"
		clz NvmeTargetInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.21"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.nvme.APIQueryNvmeTargetReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.21"
		clz ErrorCode.class
	}
}
