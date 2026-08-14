package org.zstack.header.cloudformation

import org.zstack.header.errorcode.ErrorCode

doc {

	title "预览资源编排模板生成的资源清单列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.cloudformation.APIPreviewResourceStackReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.5.0"
		clz ErrorCode.class
	}
	ref {
		name "preview"
		path "org.zstack.header.cloudformation.APIPreviewResourceStackReply.preview"
		desc "预览资源编排模板生成的资源清单列表"
		type "PreviewResourceStruct"
		since "2.5.0"
		clz PreviewResourceStruct.class
	}
}
