package org.zstack.storage.primary.sharedblock

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.primary.sharedblock.SharedBlockCandidateStruct

doc {

	title "获取共享块设备候选清单结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.primary.sharedblock.APIGetSharedBlockCandidateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.5.0"
		clz ErrorCode.class
	}
	ref {
		name "results"
		path "org.zstack.storage.primary.sharedblock.APIGetSharedBlockCandidateReply.results"
		desc "候选共享块设备信息"
		type "List"
		since "2.5.0"
		clz SharedBlockCandidateStruct.class
	}
}
