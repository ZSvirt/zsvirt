package org.zstack.zwatch.monitorgroup.entity

import org.zstack.zwatch.alarm.AlarmStatus
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "资源分组资源"

	field {
		name "groupUuid"
		desc "资源分组 UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "instanceResourceType"
		desc "资源类型"
		type "String"
		since "3.10.0"
	}
	field {
		name "instanceUuid"
		desc "资源实例 UUID"
		type "String"
		since "3.10.0"
	}
	ref {
		name "status"
		path "org.zstack.zwatch.monitorgroup.entity.MonitorGroupInstanceInventory.status"
		desc "报警状态"
		type "AlarmStatus"
		since "3.10.0"
		clz AlarmStatus.class
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
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.10.0"
	}
}
