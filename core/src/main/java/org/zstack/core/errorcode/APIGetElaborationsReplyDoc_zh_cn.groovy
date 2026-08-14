package org.zstack.core.errorcode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "查看错误码内容结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.core.errorcode.APIGetElaborationsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.3.0"
		clz ErrorCode.class
	}
	ref {
		name "contents"
		path "org.zstack.core.errorcode.APIGetElaborationsReply.contents"
		desc "错误码列表内容"
		type "List"
		since "3.3.0"
		clz ElaborationContent.class
	}
}
