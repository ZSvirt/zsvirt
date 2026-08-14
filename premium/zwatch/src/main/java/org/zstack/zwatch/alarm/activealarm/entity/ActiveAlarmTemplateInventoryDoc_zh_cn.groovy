package org.zstack.zwatch.alarm.activealarm.entity

import org.zstack.zwatch.ruleengine.ComparisonOperator
import org.zstack.zwatch.datatype.EmergencyLevel
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "一键报警报警器模板"

	field {
		name "uuid"
		desc "资源的 UUID，唯一标示该资源"
		type "String"
		since "3.10.0"
	}
	field {
		name "alarmName"
		desc "名称"
		type "String"
		since "3.10.0"
	}
	ref {
		name "comparisonOperator"
		path "org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmTemplateInventory.comparisonOperator"
		desc "阈值比较符"
		type "ComparisonOperator"
		since "3.10.0"
		clz ComparisonOperator.class
	}
	field {
		name "period"
		desc "阈值持续时间"
		type "int"
		since "3.10.0"
	}
	field {
		name "repeatInterval"
		desc "报警重复时间"
		type "int"
		since "3.10.0"
	}
	field {
		name "repeatCount"
		desc "报警次数"
		type "int"
		since "3.10.0"
	}
	field {
		name "namespace"
		desc "名字空间"
		type "String"
		since "3.10.0"
	}
	field {
		name "metricName"
		desc "监控项名称"
		type "String"
		since "3.10.0"
	}
	field {
		name "threshold"
		desc "阈值"
		type "double"
		since "3.10.0"
	}
	ref {
		name "emergencyLevel"
		path "org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmTemplateInventory.emergencyLevel"
		desc "报警级别"
		type "EmergencyLevel"
		since "3.10.0"
		clz EmergencyLevel.class
	}
	field {
		name "labels"
		desc "标签列表"
		type "String"
		since "3.10.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.10.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.10.0"
	}
}
