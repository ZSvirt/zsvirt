package org.zstack.header.errorcode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "错误"

	field {
		name "code"
		desc "错误码号，错误的全局唯一标识，例如SYS.1000, HOST.1001"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "description"
		desc "错误的概要描述"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "details"
		desc "错误的详细信息"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "elaboration"
		desc "保留字段，默认为null"
		type "String"
		since "zsv 0.6"
	}
	ref {
		name "cause"
		path "org.zstack.header.errorcode.ErrorCode.cause"
		desc "根错误，引发当前错误的源错误，若无原错误，该字段为null", true
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "opaque"
		desc "保留字段，默认为null"
		type "LinkedHashMap"
		since "zsv 0.6"
	}
}
