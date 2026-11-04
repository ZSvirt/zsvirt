package org.zstack.header.vm

import java.lang.Integer
import java.sql.Timestamp

doc {

	title "虚拟机DNS清单"

	field {
		name "vmInstanceUuid"
		desc "虚拟机UUID"
		type "String"
		since "zsv_4.10.10"
	}
	field {
		name "vmNicUuid"
		desc "虚拟机网卡UUID"
		type "String"
		since "zsv_4.10.10"
	}
	field {
		name "dns"
		desc "DNS地址"
		type "String"
		since "zsv_4.10.10"
	}
	field {
		name "ipVersion"
		desc "ip协议号"
		type "Integer"
		since "zsv_4.10.10"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv_4.10.10"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv_4.10.10"
	}
}
