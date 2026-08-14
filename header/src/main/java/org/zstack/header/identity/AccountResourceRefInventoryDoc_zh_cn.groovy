package org.zstack.header.identity

doc {

	title "账户与资源间引用关系清单"

	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "resourceType"
		desc "资源类型"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 0.6"
	}
}
