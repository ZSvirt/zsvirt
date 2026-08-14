package org.zstack.drs.entity

import java.sql.Timestamp
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "drsUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "vmUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "vmSourceHostUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "vmTargetHostUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "result"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "reason"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "adviceUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "cause"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "endDate"
		desc ""
		type "Timestamp"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
}
