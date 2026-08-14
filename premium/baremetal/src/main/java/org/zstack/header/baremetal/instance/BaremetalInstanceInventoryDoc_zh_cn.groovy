package org.zstack.header.baremetal.instance

import org.zstack.header.baremetal.network.BaremetalNicInventory

doc {

	title "裸机实例清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.6.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.6.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.6.0"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "2.6.0"
	}
	field {
		name "clusterUuid"
		desc "集群UUID"
		type "String"
		since "2.6.0"
	}
	field {
		name "pxeServerUuid"
		desc ""
		type "String"
		since "3.1.1"
	}
	field {
		name "chassisUuid"
		desc "裸机设备UUID"
		type "String"
		since "2.6.0"
	}
	field {
		name "imageUuid"
		desc "镜像UUID"
		type "String"
		since "2.6.0"
	}
	field {
		name "platform"
		desc "系统平台"
		type "String"
		since "2.6.0"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "2.6.0"
	}
	field {
		name "status"
		desc ""
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
	ref {
		name "bmNics"
		path "org.zstack.header.baremetal.instance.BaremetalInstanceInventory.bmNics"
		desc "裸机网络配置"
		type "List"
		since "2.6.0"
		clz BaremetalNicInventory.class
	}
}
