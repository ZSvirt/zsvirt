package org.zstack.header.network.l2


import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取集群允许加载的二层网络返回值"

	ref {
		name "error"
		path "org.zstack.header.network.l2.APIGetCandidateL2ForAttachingClusterReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.network.l2.APIGetCandidateL2ForAttachingClusterReply.inventories"
		desc "二层网络数据列表"
		type "List"
		since "zsv 3.10.0"
		clz L2NetworkData.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.10.0"
	}
}
