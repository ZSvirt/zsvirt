package org.zstack.header.sriov

import org.zstack.header.errorcode.ErrorCode

doc {

	title "物理机中是否存在可用VF网卡的查询结果"

	ref {
		name "error"
		path "org.zstack.header.sriov.APIIsVfNicAvailableInL3NetworkReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "vfNicAvailable"
		desc "是否存在可用VF网卡"
		type "boolean"
		since "3.9.0"
	}
}
