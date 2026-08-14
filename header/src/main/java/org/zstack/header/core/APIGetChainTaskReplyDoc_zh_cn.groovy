package org.zstack.header.core

import org.zstack.header.core.progress.ChainInfo
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取任务队列返回"

	ref {
		name "results"
		path "org.zstack.header.core.APIGetChainTaskReply.results"
		desc "任务队列信息"
		type "Map"
		since "zsv 3.16.0"
		clz ChainInfo.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.16.0"
	}
	ref {
		name "error"
		path "org.zstack.header.core.APIGetChainTaskReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.16.0"
		clz ErrorCode.class
	}
}
