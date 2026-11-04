package org.zstack.header.identity

doc {

	title "登录会话信息"

	field {
		name "uuid"
		desc "会话 UUID"
		type "String"
		since "zsv_0.6"
	}
	field {
		name "accountUuid"
		desc "会话所属账号 UUID"
		type "String"
		since "zsv_0.6"
	}
	field {
		name "expiredDate"
		desc "会话过期的时间戳"
		type "Timestamp"
		since "zsv_0.6"
	}
	field {
		name "createDate"
		desc "会话创建时间"
		type "Timestamp"
		since "zsv_0.6"
	}
}