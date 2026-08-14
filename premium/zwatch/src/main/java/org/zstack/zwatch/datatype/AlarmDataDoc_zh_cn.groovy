package org.zstack.zwatch.datatype

import java.lang.Double
import java.lang.Integer
import java.lang.Double

doc {

	title "在这里输入结构的名称"

	field {
		name "alarmUuid"
		desc "报警器UUID"
		type "String"
		since "2.3"
	}
	field {
		name "namespace"
		desc "名字空间名称"
		type "String"
		since "2.3"
	}
	field {
		name "metricName"
		desc "监控项名称"
		type "String"
		since "2.3"
	}
	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "2.3"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "2.3"
	}
	field {
		name "resourceType"
		desc "资源类型"
		type "String"
		since "2.3"
	}
	field {
		name "alarmStatus"
		desc "报警器状态"
		type "String"
		since "2.3"
	}
	field {
		name "alarmName"
		desc "报警器名称"
		type "String"
		since "2.3"
	}
	field {
		name "threshold"
		desc "报警阈值"
		type "Double"
		since "2.3"
	}
	field {
		name "period"
		desc "阈值持续周期"
		type "Integer"
		since "2.3"
	}
	field {
		name "labels"
		desc "标签列表"
		type "String"
		since "2.3"
	}
	field {
		name "metricValue"
		desc "监控项当时值"
		type "Double"
		since "2.3"
	}
	field {
		name "time"
		desc "报警纪录生成时间"
		type "long"
		since "2.3"
	}
	field {
		name "emergencyLevel"
		desc "报警等级"
		type "String"
		since "3.8"
	}
}
