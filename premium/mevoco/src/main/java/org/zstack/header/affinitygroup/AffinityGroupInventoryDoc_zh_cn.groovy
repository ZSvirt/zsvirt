package org.zstack.header.affinitygroup

import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.affinitygroup.AffinityGroupUsageInventory

doc {

	title "亲和组清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.2"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.2"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.2"
	}
	field {
		name "policy"
		desc ""
		type "String"
		since "2.2"
	}
	field {
		name "version"
		desc ""
		type "String"
		since "2.2"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "2.2"
	}
	field {
		name "appliance"
		desc ""
		type "String"
		since "2.2"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "2.3"
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
	ref {
		name "usages"
		path "org.zstack.header.affinitygroup.AffinityGroupInventory.usages"
		desc "null"
		type "List"
		since "2.2"
		clz AffinityGroupUsageInventory.class
	}
}
