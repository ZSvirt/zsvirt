package org.zstack.zwatch.alarm

import org.zstack.zwatch.ruleengine.ComparisonOperator
import java.lang.Integer
import java.lang.Double
import java.lang.Integer
import org.zstack.zwatch.alarm.AlarmStatus
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.zwatch.alarm.AlarmLabelInventory
import org.zstack.zwatch.alarm.AlarmActionInventory

doc {

	title "报警器结构"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.3"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.3"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.3"
	}
	ref {
		name "comparisonOperator"
		path "org.zstack.zwatch.alarm.AlarmInventory.comparisonOperator"
		desc "阈值比较符"
		type "ComparisonOperator"
		since "2.3"
		clz ComparisonOperator.class
	}
	field {
		name "period"
		desc "阈值持续时间"
		type "Integer"
		since "2.3"
	}
	field {
		name "namespace"
		desc "名字空间"
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
		name "threshold"
		desc "阈值"
		type "Double"
		since "2.3"
	}
	field {
		name "repeatInterval"
		desc "报警重复时间"
		type "Integer"
		since "2.3"
	}
	field {
		name "enableRecovery"
		desc "开启恢复通知"
		type "Boolean"
		since "3.4.0"
	}
	ref {
		name "status"
		path "org.zstack.zwatch.alarm.AlarmInventory.status"
		desc "报警器状态"
		type "AlarmStatus"
		since "2.3"
		clz AlarmStatus.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.3"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.3"
	}
	ref {
		name "labels"
		path "org.zstack.zwatch.alarm.AlarmInventory.labels"
		desc "标签列表"
		type "List"
		since "2.3"
		clz AlarmLabelInventory.class
	}
	ref {
		name "actions"
		path "org.zstack.zwatch.alarm.AlarmInventory.actions"
		desc "动作列表"
		type "List"
		since "2.3"
		clz AlarmActionInventory.class
	}
}
