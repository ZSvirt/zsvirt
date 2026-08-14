package org.zstack.storage.device.hba


import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询 HBA 卡返回"

	ref {
		name "inventories"
		path "org.zstack.storage.device.hba.APIQueryHBADeviceReply.inventories"
		desc "HBA 卡清单列表"
		type "List"
		since "4.10.7"
		clz HbaDeviceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.7"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.hba.APIQueryHBADeviceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.7"
		clz ErrorCode.class
	}
}
