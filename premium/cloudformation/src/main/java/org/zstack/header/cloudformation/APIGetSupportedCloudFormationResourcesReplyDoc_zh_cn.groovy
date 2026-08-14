package org.zstack.header.cloudformation

import org.zstack.header.errorcode.ErrorCode

doc {

	title "支持的资源列表清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.cloudformation.APIGetSupportedCloudFormationResourcesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6.0"
		clz ErrorCode.class
	}
	ref {
		name "resources"
		path "org.zstack.header.cloudformation.APIGetSupportedCloudFormationResourcesReply.resources"
		desc "支持的资源列表清单"
		type "List"
		since "2.6.0"
		clz SupportedResourceStruct.class
	}
}
