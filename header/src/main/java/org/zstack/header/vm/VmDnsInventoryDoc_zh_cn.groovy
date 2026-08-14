package org.zstack.header.vm

import java.lang.Integer
import java.sql.Timestamp

doc {

	title "云主机DNS清单"

	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "zsv 4.10.10"
	}
	field {
		name "vmNicUuid"
		desc "云主机网卡UUID"
		type "String"
		since "zsv 4.10.10"
	}
	field {
		name "dns"
		desc "DNS地址"
		type "String"
		since "zsv 4.10.10"
	}
	field {
		name "ipVersion"
		desc "ip协议号"
		type "Integer"
		since "zsv 4.10.10"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.10.10"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.10.10"
	}
}
