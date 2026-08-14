package org.zstack.softwarePackage.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "安装软件包返回"

	field {
		name "success"
		desc "操作成功时为true，否则为false"
		type "boolean"
		since "4.10.20"
	}
	ref {
		name "error"
		path "org.zstack.softwarePackage.header.APIInstallSoftwarePackageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
}
