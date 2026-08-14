package org.zstack.header.vm

import org.zstack.header.vm.APICreateVmInstanceFromVolumeEvent

doc {
	title "CreateVmInstanceFromVolume"

	category "vmInstance"

	desc """从云盘创建虚拟机"""

	rest {
		request {
			url "POST /v1/vm-instances/from/volume"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmInstanceFromVolumeMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 3.10.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "instanceOfferingUuid"
					enclosedIn "params"
					desc "计算规格UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "cpuNum"
					enclosedIn "params"
					desc "CPU数量"
					location "body"
					type "Integer"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "memorySize"
					enclosedIn "params"
					desc "内存大小"
					location "body"
					type "Long"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc "三层网络UUID"
					location "body"
					type "List"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "vmNicParams"
					enclosedIn "params"
					desc "网卡信息"
					location "body"
					type "String"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "云主机类型"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
					values ("UserVm","ApplianceVm")
				}
				column {
					name "volumeUuid"
					enclosedIn "params"
					desc "云盘UUID"
					location "body"
					type "String"
					optional false
					since "zsv 3.10.0"
				}
				column {
					name "platform"
					enclosedIn "params"
					desc "云盘系统平台"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
					values ("Linux","Windows","Other","Paravirtualization","WindowsVirtio")
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn "params"
					desc "默认三层网络UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc "云主机创建策略"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
					values ("InstantStart","CreateStopped")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "reservedMemorySize"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "zsv 3.17.21"
				}
			}
		}

		response {
			clz APICreateVmInstanceFromVolumeEvent.class
		}
	}
}