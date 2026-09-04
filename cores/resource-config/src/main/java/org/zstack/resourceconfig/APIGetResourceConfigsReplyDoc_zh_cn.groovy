package org.zstack.resourceconfig

import org.zstack.header.errorcode.ErrorCode
import org.zstack.resourceconfig.ResourceConfigStruct

doc {

	title "查询多个资源级配置的返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.resourceconfig.APIGetResourceConfigsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
	ref {
		name "configs"
		path "org.zstack.resourceconfig.APIGetResourceConfigsReply.configs"
		desc "资源及配置列表"
		type "List"
		since "3.17.0"
		clz ResourceConfigStruct.class
	}
}
