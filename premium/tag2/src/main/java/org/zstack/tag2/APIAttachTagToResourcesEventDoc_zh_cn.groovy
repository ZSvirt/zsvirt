package org.zstack.tag2

import org.zstack.header.errorcode.ErrorCode
import org.zstack.tag2.AttachTagResult
import org.zstack.header.errorcode.ErrorCode

doc {

	title "加载标签到资源上的结果"

	ref {
		name "error"
		path "org.zstack.tag2.APIAttachTagToResourcesEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.2.0"
		clz ErrorCode.class
	}
	ref {
		name "results"
		path "org.zstack.tag2.APIAttachTagToResourcesEvent.results"
		desc "加载标签的结果"
		type "List"
		since "3.2.0"
		clz AttachTagResult.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.2.0"
	}
}
