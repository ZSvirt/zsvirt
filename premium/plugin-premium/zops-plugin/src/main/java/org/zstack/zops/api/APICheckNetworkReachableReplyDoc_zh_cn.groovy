package org.zstack.zops.api

import org.zstack.zops.NetworkReachablePair
import org.zstack.header.errorcode.ErrorCode

doc {

	title "检查多个主机之间网络连通性返回"

	ref {
		name "results"
		path "org.zstack.zops.api.APICheckNetworkReachableReply.results"
		desc "网络连通性检查结果"
		type "List"
		since "3.17.21"
		clz NetworkReachablePair.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.zops.api.APICheckNetworkReachableReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
