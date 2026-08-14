package org.zstack.storage.device.nvme

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.device.nvme.NvmeServerInventory

doc {

	title "刷新NVMe服务器结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.10.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.nvme.APIRefreshNvmeServerEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.device.nvme.APIRefreshNvmeServerEvent.inventory"
		desc "null"
		type "NvmeServerInventory"
		since "zsv 4.10.6"
		clz NvmeServerInventory.class
	}
}
