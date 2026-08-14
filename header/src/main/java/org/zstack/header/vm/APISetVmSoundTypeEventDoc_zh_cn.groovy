package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "设置云主机虚拟声卡类型返回结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APISetVmSoundTypeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.7"
		clz ErrorCode.class
	}
}
