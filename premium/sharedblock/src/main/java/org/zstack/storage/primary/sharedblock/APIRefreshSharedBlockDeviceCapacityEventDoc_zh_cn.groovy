package org.zstack.storage.primary.sharedblock

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.primary.sharedblock.SharedBlockGroupPrimaryStorageInventory

doc {

	title "更新共享块设备容量结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.primary.sharedblock.APIRefreshSharedBlockDeviceCapacityEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.primary.sharedblock.APIRefreshSharedBlockDeviceCapacityEvent.inventory"
		desc "共享块存储清单"
		type "SharedBlockGroupPrimaryStorageInventory"
		since "2.6"
		clz SharedBlockGroupPrimaryStorageInventory.class
	}
}
