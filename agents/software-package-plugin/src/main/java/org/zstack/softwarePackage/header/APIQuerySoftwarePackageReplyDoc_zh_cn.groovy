package org.zstack.softwarePackage.header


import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询软件包返回"

	ref {
		name "inventories"
		path "org.zstack.softwarePackage.header.APIQuerySoftwarePackageReply.inventories"
		desc "软件包清单"
		type "List"
		since "4.10.20"
		clz SoftwarePackageInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.20"
	}
	ref {
		name "error"
		path "org.zstack.softwarePackage.header.APIQuerySoftwarePackageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
}
