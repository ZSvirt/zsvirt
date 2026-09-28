package org.zstack.storage.primary.sharedblock

import org.zstack.header.errorcode.ErrorCode

doc {

	title "扩容共享块卷返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "5.2.0"
	}
	ref {
		name "error"
		path "org.zstack.storage.primary.sharedblock.APIExtendSharedBlockVolumeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.2.0"
		clz ErrorCode.class
	}
}
