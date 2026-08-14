package org.zstack.resourceconfig

import org.zstack.resourceconfig.ResourceConfigStruct
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新资源高级设置的请求返回"

	ref {
		name "inventories"
		path "org.zstack.resourceconfig.APIUpdateResourceConfigsEvent.inventories"
		desc "资源高级设置清单"
		type "List"
		since "3.17.0"
		clz ResourceConfigStruct.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.resourceconfig.APIUpdateResourceConfigsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
