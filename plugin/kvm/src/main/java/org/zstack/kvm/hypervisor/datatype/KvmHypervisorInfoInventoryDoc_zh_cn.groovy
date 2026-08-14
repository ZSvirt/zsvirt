package org.zstack.kvm.hypervisor.datatype

import java.sql.Timestamp

doc {

	title "当前主机 / 虚拟机使用的监控软件的信息"

	field {
		name "uuid"
		desc "资源的UUID，为主机 / 虚拟机的 UUID"
		type "String"
		since "3.16.21"
	}
	field {
		name "hypervisor"
		desc "虚拟化软件名称，比如 \"qemu-kvm\""
		type "String"
		since "3.16.21"
	}
	field {
		name "version"
		desc "版本号，比如 \"4.2.0-632.g6a6222b.el7\""
		type "String"
		since "3.16.21"
	}
	field {
		name "matchState"
		desc "和预期版本号的匹配状态，可能的值为 \"Matched\", \"Unmatched\", \"Unknown\""
		type "String"
		since "3.16.21"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.16.21"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.16.21"
	}
}
