package org.zstack.billing.generator

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "账单明细"

	field {
		name "id"
		desc "账单ID"
		type "long"
		since "3.7"
	}
	field {
		name "billingType"
		desc "账单类型"
		type "String"
		since "3.7"
	}
	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "3.7"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "3.7"
	}
	field {
		name "resourceName"
		desc "资源名称"
		type "String"
		since "3.7"
	}
	field {
		name "spending"
		desc "费用"
		type "double"
		since "3.7"
	}
	field {
		name "startTime"
		desc "资源计费开始时间"
		type "long"
		since "3.7"
	}
	field {
		name "endTime"
		desc "资源计费结束时间"
		type "long"
		since "3.7"
	}
	field {
		name "hypervisorType"
		desc "资源虚拟化类型"
		type "String"
		since "3.7"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.7"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.7"
	}
}
