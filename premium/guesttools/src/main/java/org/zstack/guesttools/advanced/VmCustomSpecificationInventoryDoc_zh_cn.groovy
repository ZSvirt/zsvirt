package org.zstack.guesttools.advanced

import java.lang.Boolean
import org.zstack.header.configuration.VmCustomSpecificationDomainMode
import java.sql.Timestamp

doc {

	title "虚拟机自定义操作系统规范清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.10.18"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "4.10.18"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.10.18"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.10.18"
	}
	field {
		name "platform"
		desc "虚拟机操作系统平台类型"
		type "String"
		since "4.10.18"
	}
	field {
		name "hostname"
		desc "主机名称"
		type "String"
		since "4.10.18"
	}
	field {
		name "generateSID"
		desc "是否生成SID"
		type "Boolean"
		since "4.10.18"
	}
	ref {
		name "domainMode"
		path "org.zstack.guesttools.advanced.VmCustomSpecificationInventory.domainMode"
		desc "null"
		type "VmCustomSpecificationDomainMode"
		since "4.10.18"
		clz VmCustomSpecificationDomainMode.class
	}
	field {
		name "domainName"
		desc "域名称或工作组名称"
		type "String"
		since "4.10.18"
	}
	field {
		name "domainUsername"
		desc "域用户名"
		type "String"
		since "4.10.18"
	}
	field {
		name "organization"
		desc "组织单位"
		type "String"
		since "4.10.18"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.10.18"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.10.18"
	}
}
