package org.zstack.header.identity

doc {

	title "配额清单"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "identityUuid"
		desc "身份UUID（账户UUID，用户UUID）"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "identityType"
		desc "身份类型（账户，用户）"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "value"
		desc "默认配额值"
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 0.6"
	}
}
