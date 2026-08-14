package org.zstack.storage.ceph.primary

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp
import java.lang.Integer

doc {

	title "在这里输入结构的名称"

	field {
		name "hostname"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "monPort"
		desc ""
		type "Integer"
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
	field {
		name "primaryStorageUuid"
		desc "主存储UUID"
		type "String"
		since "0.6"
	}
	field {
		name "monAddr"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "sshUsername"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "sshPassword"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "sshPort"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "monUuid"
		desc ""
		type "String"
		since "0.6"
	}
}
