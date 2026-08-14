package org.zstack.softwarePackage.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "上传并更新软件包返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.softwarePackage.header.APIUploadAndExecuteSoftwareUpgradePackageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
}
