package org.zstack.header.affinitygroup

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "亲和组使用情况"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.2"
	}
	field {
		name "affinityGroupUuid"
		desc ""
		type "String"
		since "2.2"
	}
	field {
		name "resourceUuid"
		desc ""
		type "String"
		since "2.2"
	}
	field {
		name "resourceType"
		desc ""
		type "String"
		since "2.2"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.2"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.2"
	}
}
