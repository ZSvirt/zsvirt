package org.zstack.zwatch.monitorgroup.entity

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "监控报警模板"

	field {
		name "uuid"
		desc "监控报警模板UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "name"
		desc "名称"
		type "String"
		since "3.10.0"
	}
	field {
		name "description"
		desc "详细描述"
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
		name "monitorGroupTemplateRefs"
		desc "告警模板&资源分组关联数组"
		type "List"
		since "3.17.11"
	}
}
