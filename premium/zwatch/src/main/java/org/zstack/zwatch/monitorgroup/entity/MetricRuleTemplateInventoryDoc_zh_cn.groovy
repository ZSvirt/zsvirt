package org.zstack.zwatch.monitorgroup.entity

import org.zstack.zwatch.ruleengine.ComparisonOperator
import org.zstack.zwatch.datatype.EmergencyLevel
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "资源报警模板"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.10.0"
	}
	field {
		name "monitorTemplateUuid"
		desc "监控模板"
		type "String"
		since "3.10.0"
	}
	ref {
		name "comparisonOperator"
		path "org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateInventory.comparisonOperator"
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
		path "org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateInventory.emergencyLevel"
		desc "报警等级"
		type "EmergencyLevel"
		since "3.10.0"
		clz EmergencyLevel.class
	}
	field {
		name "labels"
		desc "标签"
		type "String"
		since "3.10.0"
	}
	field {
		name "enableRecovery"
		desc "开启恢复通知"
		type "boolean"
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
	field {
		name "uuid"
		desc "资源报警模板 UUID"
		type "String"
		since "3.10.0"
	}
}
