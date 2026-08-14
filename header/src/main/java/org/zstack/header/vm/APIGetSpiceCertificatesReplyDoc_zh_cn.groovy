package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取spice的CA证书"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetSpiceCertificatesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.7"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	field {
		name "certificateStr"
		desc "spice CA证书"
		type "String"
		since "zsv 3.7"
	}
}
