package org.zstack.header.volume

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取硬盘 IO 线程绑定信息的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.31"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.APIGetVolumeIoThreadPinReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.31"
		clz ErrorCode.class
	}
	field {
		name "volumeUuid"
		desc "硬盘 UUID"
		type "String"
		since "3.16.31"
	}
	field {
		name "ioThreadId"
		desc "IO 线程的 ID"
		type "String"
		since "3.16.31"
	}
	field {
		name "pin"
		desc "绑定的 CPU 范围"
		type "String"
		since "3.16.31"
	}
}
