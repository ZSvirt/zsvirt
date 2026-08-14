package org.zstack.resourceconfig

import org.zstack.header.errorcode.ErrorCode
import org.zstack.resourceconfig.ResourceConfigInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取资源的高级设置的结果"

	ref {
		name "error"
		path "org.zstack.resourceconfig.APIGetResourceConfigReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.4.0"
		clz ErrorCode.class
	}
	field {
		name "value"
		desc "设置的值"
		type "String"
		since "3.4.0"
	}
	ref {
		name "effectiveConfigs"
		path "org.zstack.resourceconfig.APIGetResourceConfigReply.effectiveConfigs"
		desc "生效的设置列表，按优先级从高到低排序"
		type "List"
		since "3.4.0"
		clz ResourceConfigInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.4.0"
	}
}
