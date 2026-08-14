package org.zstack.header.resourceattribute.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除自定义属性值的结果"

	field {
		name "success"
		desc "删除是否成功"
		type "boolean"
		since "zsv 4.10.16"
	}
	ref {
		name "error"
		path "org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeValueEvent.error"
		desc "错误码，若不为 null，则表示操作失败，操作成功时该字段为 null"
		type "ErrorCode"
		since "zsv 4.10.16"
		clz ErrorCode.class
	}
}
