package org.zstack.header.volume

import org.zstack.header.errorcode.ErrorCode

doc {

	title "验证硬盘当前快照链"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.13.0"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APIValidateVolumeSnapshotChainEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.13.0"
		clz ErrorCode.class
	}
}
