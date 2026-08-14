package org.zstack.softwarePackage.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "上传软件包到镜像存储返回"

	ref {
		name "inventory"
		path "org.zstack.softwarePackage.header.APIUploadSoftwarePackageToBackupStorageEvent.inventory"
		desc "null"
		type "SoftwarePackageInventory"
		since "5.0.0"
		clz SoftwarePackageInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.softwarePackage.header.APIUploadSoftwarePackageToBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
