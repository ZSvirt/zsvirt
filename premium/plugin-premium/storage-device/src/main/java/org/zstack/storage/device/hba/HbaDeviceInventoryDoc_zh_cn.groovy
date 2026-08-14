package org.zstack.storage.device.hba



doc {

	title "HBA 卡实体清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.10.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.10.6"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "4.10.6"
	}
	field {
		name "portName"
		desc "端口名字"
		type "String"
		since "4.10.6"
	}
	field {
		name "portState"
		desc "端口状态"
		type "String"
		since "4.10.6"
	}
	field {
		name "hbaType"
		desc "类型"
		type "String"
		since "4.10.6"
	}
	field {
		name "speed"
		desc "速度"
		type "String"
		since "4.10.6"
	}
	field {
		name "supportedSpeeds"
		desc "支持的速度"
		type "String"
		since "4.10.6"
	}
	field {
		name "symbolicName"
		desc "符号名称"
		type "String"
		since "4.10.6"
	}
	field {
		name "supportedClasses"
		desc "服务类别"
		type "String"
		since "4.10.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "String"
		since "4.10.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "String"
		since "4.10.6"
	}
}
