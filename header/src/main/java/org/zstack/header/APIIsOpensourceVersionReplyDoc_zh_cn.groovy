package org.zstack.header

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "返回当前ZStack是否为开源版本"

	ref {
		name "error"
		path "org.zstack.header.APIIsOpensourceVersionReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "opensource"
		desc "true：当前为开源版本，不带企业版插件；false:当前为企业版本，带所有插件"
		type "boolean"
		since "zsv 2.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
}
