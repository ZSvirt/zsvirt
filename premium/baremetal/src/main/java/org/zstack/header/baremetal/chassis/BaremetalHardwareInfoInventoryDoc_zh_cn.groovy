package org.zstack.header.baremetal.chassis

doc {

	title "裸机硬件配置清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.6.0"
	}
	field {
		name "chassisUuid"
		desc "裸机设备"
		type "String"
		since "2.6.0"
	}
	field {
		name "type"
		desc "类型"
		type "String"
		since "2.6.0"
	}
	field {
		name "content"
		desc "内容"
		type "String"
		since "2.6.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.6.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.6.0"
	}
}
