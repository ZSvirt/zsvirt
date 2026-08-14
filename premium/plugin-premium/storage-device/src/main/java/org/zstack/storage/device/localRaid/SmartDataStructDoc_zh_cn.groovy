package org.zstack.storage.device.localRaid

import java.lang.Integer
import java.lang.Integer
import java.lang.Integer
import java.lang.Integer
import java.lang.Long

doc {

	title "SMART信息清单"

	field {
		name "id"
		desc "Id"
		type "Integer"
		since "3.6"
	}
	field {
		name "attributeName"
		desc "属性名"
		type "String"
		since "3.6"
	}
	field {
		name "flag"
		desc "标志"
		type "String"
		since "3.6"
	}
	field {
		name "value"
		desc "当前归一化值"
		type "Integer"
		since "3.6"
	}
	field {
		name "worst"
		desc "最差归一化值"
		type "Integer"
		since "3.6"
	}
	field {
		name "thresh"
		desc "阈值"
		type "Integer"
		since "3.6"
	}
	field {
		name "type"
		desc "类型"
		type "String"
		since "3.6"
	}
	field {
		name "updated"
		desc "更新"
		type "String"
		since "3.6"
	}
	field {
		name "whenFailed"
		desc "磁盘失效"
		type "String"
		since "3.6"
	}
	field {
		name "rawValue"
		desc "原始值"
		type "Long"
		since "3.6"
	}
	field {
		name "state"
		desc "健康状态"
		type "String"
		since "3.6"
	}
}
