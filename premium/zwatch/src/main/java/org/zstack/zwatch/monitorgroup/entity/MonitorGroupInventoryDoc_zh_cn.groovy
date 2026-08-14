package org.zstack.zwatch.monitorgroup.entity

import org.zstack.zwatch.monitorgroup.entity.MonitorGroupState
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "资源分组"

	field {
		name "name"
		desc "资源分组名称"
		type "String"
		since "3.10.0"
	}
	ref {
		name "state"
		path "org.zstack.zwatch.monitorgroup.entity.MonitorGroupInventory.state"
		desc "资源分组状态"
		type "MonitorGroupState"
		since "3.10.0"
		clz MonitorGroupState.class
	}
	field {
		name "actions"
		desc "报警行为"
		type "String"
		since "3.10.0"
	}
	field {
		name "description"
		desc "资源分组详细描述"
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
	field {
		name "uuid"
		desc "资源分组UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "monitorGroupTemplateRefs"
		desc "告警模板&资源分组关联数组"
		type "List"
		since "3.17.11"
	}
}
