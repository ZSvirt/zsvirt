package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmCapabilities

doc {

	title "批量获取云主机能力返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmsCapabilitiesEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.0"
		clz ErrorCode.class
	}
	ref {
		name "vmsCaps"
		path "org.zstack.header.vm.APIGetVmsCapabilitiesEvent.vmsCaps"
		desc "null"
		type "Map"
		since "zsv 4.0"
		clz VmCapabilities.class
	}
}
