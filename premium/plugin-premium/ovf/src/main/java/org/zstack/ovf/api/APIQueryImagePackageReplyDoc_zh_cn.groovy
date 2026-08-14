package org.zstack.ovf.api

import org.zstack.ovf.datatype.ImagePackageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询镜像包返回"

	ref {
		name "inventories"
		path "org.zstack.ovf.api.APIQueryImagePackageReply.inventories"
		desc "镜像包清单列表"
		type "List"
		since "3.14.6"
		clz ImagePackageInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.14.6"
	}
	ref {
		name "error"
		path "org.zstack.ovf.api.APIQueryImagePackageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.14.6"
		clz ErrorCode.class
	}
}
