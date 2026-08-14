package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取网卡交集子接口信息"

	field {
		name "vlanIds"
		desc "vlan接口Ids"
		type "List"
		since "3.17.11"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetCandidateInterfaceVlanIdsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
