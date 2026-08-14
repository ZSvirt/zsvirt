package org.zstack.header.allocator

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.allocator.datatypes.CpuMemoryCapacityData
import org.zstack.header.errorcode.ErrorCode

doc {

	title "cpu和内存容量"

	ref {
		name "error"
		path "org.zstack.header.allocator.APIGetCpuMemoryCapacityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "totalCpu"
		desc "cpu总数"
		type "long"
		since "zsv 0.6"
	}
	field {
		name "availableCpu"
		desc "可用cpu数量"
		type "long"
		since "zsv 0.6"
	}
	field {
		name "totalMemory"
		desc "内存总量"
		type "long"
		since "zsv 0.6"
	}
	field {
		name "availableMemory"
		desc "可用内存"
		type "long"
		since "zsv 0.6"
	}
	field {
		name "managedCpuNum"
		desc "受管理的物理CPU数量"
		type "long"
		since "zsv 2.5.0"
	}
	ref {
		name "capacityData"
		path "org.zstack.header.allocator.APIGetCpuMemoryCapacityReply.capacityData"
		desc "所有被查询的资源的CPU和内存容量信息"
		type "List"
		since "zsv 3.9.0"
		clz CpuMemoryCapacityData.class
	}
	field {
		name "resourceType"
		desc "所查资源的类型（物理机、集群、区域）"
		type "String"
		since "zsv 3.9.0"
	}
	field {
		name "success"
		desc "成功"
		type "boolean"
		since "zsv 0.6"
	}
}
