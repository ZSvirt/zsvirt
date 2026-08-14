package org.zstack.header.volume

import org.zstack.header.errorcode.ErrorCode

doc {

	title "云盘支持的类型的列表"

	ref {
		name "error"
		path "org.zstack.header.volume.APIGetVolumeCapabilitiesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	field {
		name "capabilities"
		desc "云盘支持的类型列表"
		type "Map"
		since "zsv 0.6"
	}
}
