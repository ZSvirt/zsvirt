package org.zstack.header.vm

import org.zstack.header.vm.APICreateVmInstanceFromTemplatedVmInstanceEvent

doc {
	title "CreateVmInstanceFromTemplatedVmInstance"

	category "mevoco"

	desc """从虚拟机模板创建虚拟机"""

	rest {
		request {
			url "POST /v1/vm-instances/{templatedVmInstanceUuid}/create-vmInstance-from-templated-vmInstance"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmInstanceFromTemplatedVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "names"
					enclosedIn "params"
					desc "虚拟机名字清单"
					location "body"
					type "List"
					optional false
					since "zsv 4.2.6"
				}
				column {
					name "templatedVmInstanceUuid"
					enclosedIn "params"
					desc "模板虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.6"
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc "创建策略"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
					values ("InstantStart","JustCreate","CreateStopped")
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "cpuNum"
					enclosedIn "params"
					desc "CPU核数"
					location "body"
					type "Integer"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "memorySize"
					enclosedIn "params"
					desc "内存大小"
					location "body"
					type "Long"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "reservedMemorySize"
					enclosedIn "params"
					desc "保留内存大小"
					location "body"
					type "Long"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc "三层网络UUID列表。可以指定一个或多个三层网络，云主机会在每个网络上创建一个网卡"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn "params"
					desc "默认三层网络UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "vmNicParams"
					enclosedIn "params"
					desc "网卡信息"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "diskAOs"
					enclosedIn "params"
					desc "硬盘"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "instanceOfferingUuid"
					enclosedIn "params"
					desc "计算规格UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "虚拟机类型"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "vmCustomSpecification"
					enclosedIn "params"
					desc "虚拟机自定义操作系统"
					location "body"
					type "VmCustomSpecificationStruct"
					optional true
					since "4.10.18"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.6"
				}
				column {
					name "resetTpm"
					enclosedIn "params"
					desc "创建的虚拟机是否重置 TPM 状态"
					location "body"
					type "Boolean"
					optional true
					since "5.0.0"
				}
			}
		}

		response {
			clz APICreateVmInstanceFromTemplatedVmInstanceEvent.class
		}
	}
}