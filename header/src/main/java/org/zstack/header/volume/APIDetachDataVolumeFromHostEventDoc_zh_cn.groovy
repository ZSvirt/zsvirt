package org.zstack.header.volume

import org.zstack.header.errorcode.ErrorCode

doc {

	title "从主机卸载数据硬盘返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.15.0"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APIDetachDataVolumeFromHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.15.0"
		clz ErrorCode.class
	}
}
