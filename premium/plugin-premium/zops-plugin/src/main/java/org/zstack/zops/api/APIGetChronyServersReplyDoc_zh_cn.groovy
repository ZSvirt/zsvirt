package org.zstack.zops.api

import org.zstack.zops.ChronyServerInfoPair
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取chrony时间源服务器返回"

	ref {
		name "servers"
		path "org.zstack.zops.api.APIGetChronyServersReply.servers"
		desc "时间源服务器"
		type "List"
		since "3.17.21"
		clz ChronyServerInfoPair.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.zops.api.APIGetChronyServersReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
