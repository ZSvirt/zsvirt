package org.zstack.appliancevm

import java.lang.Integer
import java.lang.Long
import java.lang.Integer
import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.vm.VmNicInventory
import org.zstack.header.volume.VolumeInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "applianceVmType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "managementNetworkUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "defaultRouteL3NetworkUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "agentPort"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "0.6"
	}
	field {
		name "clusterUuid"
		desc "集群UUID"
		type "String"
		since "0.6"
	}
	field {
		name "imageUuid"
		desc "镜像UUID"
		type "String"
		since "0.6"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "0.6"
	}
	field {
		name "lastHostUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "instanceOfferingUuid"
		desc "计算规格UUID"
		type "String"
		since "0.6"
	}
	field {
		name "rootVolumeUuid"
		desc "根云盘UUID"
		type "String"
		since "0.6"
	}
	field {
		name "platform"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "defaultL3NetworkUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "hypervisorType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "memorySize"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "cpuNum"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "cpuSpeed"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "allocatorStrategy"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "0.6"
	}
	ref {
		name "vmNics"
		path "org.zstack.appliancevm.ApplianceVmInventory.vmNics"
		desc "null"
		type "List"
		since "0.6"
		clz VmNicInventory.class
	}
	ref {
		name "allVolumes"
		path "org.zstack.appliancevm.ApplianceVmInventory.allVolumes"
		desc "null"
		type "List"
		since "0.6"
		clz VolumeInventory.class
	}
}
