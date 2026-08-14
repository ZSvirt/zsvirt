package org.zstack.query

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zql.ZQLQueryReturn

doc {

	title "在这里输入结构的名称"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.query.APIZQLQueryReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "result"
		path "org.zstack.query.APIZQLQueryReply.result"
		desc "null"
		type "ZQLQueryResult"
		since "0.6"
		clz ZQLQueryReturn.class
	}
}
