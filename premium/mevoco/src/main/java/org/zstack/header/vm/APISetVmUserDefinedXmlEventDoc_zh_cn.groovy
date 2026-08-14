package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "配置虚拟机自定义XML结果"

	ref {
		name "error"
		path "org.zstack.header.vm.APISetVmUserDefinedXmlEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.10"
		clz ErrorCode.class
	}
	field {
		name "vmUserDefinedXml"
		desc "用户自定义的XML"
		type "String"
		since "3.10"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.10"
	}
}
