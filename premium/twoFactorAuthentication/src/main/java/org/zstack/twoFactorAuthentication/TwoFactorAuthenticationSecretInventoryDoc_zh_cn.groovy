package org.zstack.twoFactorAuthentication

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "双因子认证密匙清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.10.0"
	}
	field {
		name "secret"
		desc "双因子认证密匙"
		type "String"
		since "4.10.0"
	}
	ref {
		name "status"
		desc "状态"
		type "String"
		since "4.10.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.10.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.10.0"
	}
	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "4.10.0"
	}
}
