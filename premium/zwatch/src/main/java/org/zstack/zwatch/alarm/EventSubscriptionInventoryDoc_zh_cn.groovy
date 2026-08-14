package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.EventSubscriptionState
import org.zstack.zwatch.alarm.EventSubscriptionActionInventory
import org.zstack.zwatch.alarm.EventSubscriptionLabelInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "事件订阅结果"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.3.1"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.0.0"
	}
	field {
		name "namespace"
		desc "名字空间"
		type "String"
		since "2.3.1"
	}
	field {
		name "eventName"
		desc "事件名"
		type "String"
		since "2.3.1"
	}
	ref {
		name "state"
		path "org.zstack.zwatch.alarm.EventSubscriptionInventory.state"
		desc "事件订阅状态"
		type "EventSubscriptionState"
		since "2.3.1"
		clz EventSubscriptionState.class
	}
	ref {
		name "actions"
		path "org.zstack.zwatch.alarm.EventSubscriptionInventory.actions"
		desc "动作列表"
		type "List"
		since "2.3.1"
		clz EventSubscriptionActionInventory.class
	}
	ref {
		name "labels"
		path "org.zstack.zwatch.alarm.EventSubscriptionInventory.labels"
		desc "标签列表"
		type "List"
		since "2.3.1"
		clz EventSubscriptionLabelInventory.class
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.3.1"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.3.1"
	}
	field {
		name "emergencyLevel"
		desc "报警等级"
		type "String"
		since "3.8"
	}
}
