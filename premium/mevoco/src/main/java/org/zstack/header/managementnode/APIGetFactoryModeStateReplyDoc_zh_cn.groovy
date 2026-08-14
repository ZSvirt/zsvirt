package org.zstack.header.managementnode

import org.zstack.header.errorcode.ErrorCode
import java.lang.Boolean
import org.zstack.header.errorcode.ErrorCode

doc {

	title " 获取管理节点工厂状态结果"

	ref {
		name "error"
		path "org.zstack.header.managementnode.APIGetFactoryModeStateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.8"
		clz ErrorCode.class
	}
	field {
		name "factoryModeState"
		desc ""
		type "Boolean"
		since "3.8"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.8"
	}
}
