package org.zstack.allocator.domain.entity

import java.sql.Timestamp

doc {

	title "调度域场景清单"

	field {
		name "id"
		desc "自增主键"
		type "Long"
		since "1.0.1, zsv_5.2.0"
	}
	field {
		name "domainUuid"
		desc "所属调度域UUID"
		type "String"
		since "1.0.1, zsv_5.2.0"
	}
	field {
		name "scene"
		desc "NewCreate / Start / Migrate / Ha / HostMaintenance / Drs。行存在即启用"
		type "String"
		since "1.0.1, zsv_5.2.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "1.0.1, zsv_5.2.0"
	}
}
