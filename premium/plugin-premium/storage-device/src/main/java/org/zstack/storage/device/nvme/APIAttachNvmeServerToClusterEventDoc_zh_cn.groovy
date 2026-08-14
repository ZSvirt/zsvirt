package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.NvmeServerInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "NVMe服务器加载到集群结果"

	ref {
		name "inventory"
		path "org.zstack.storage.device.nvme.APIAttachNvmeServerToClusterEvent.inventory"
		desc "NVMe 服务器清单列表"
		type "NvmeServerInventory"
		since "3.17.21"
		clz NvmeServerInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.nvme.APIAttachNvmeServerToClusterEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
