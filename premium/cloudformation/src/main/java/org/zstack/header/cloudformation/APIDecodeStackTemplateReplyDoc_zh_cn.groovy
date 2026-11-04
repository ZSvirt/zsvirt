package org.zstack.header.cloudformation

import org.zstack.header.errorcode.ErrorCode
import org.zstack.cloudformation.template.struct.ResourceStruct

doc {

	title "资源编排解析后的资源关系图列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.cloudformation.APIDecodeStackTemplateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.0.0"
		clz ErrorCode.class
	}
	ref {
		name "resources"
		path "org.zstack.header.cloudformation.APIDecodeStackTemplateReply.resources"
		desc "null"
		type "List"
		since "zsv_3.0.0"
		clz ResourceStruct.class
	}
}
