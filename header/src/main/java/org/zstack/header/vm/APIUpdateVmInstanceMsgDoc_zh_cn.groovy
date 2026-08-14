package org.zstack.header.vm

import org.zstack.header.vm.APIUpdateVmInstanceEvent

doc {
	title "更新云主机信息(UpdateVmInstance)"

	category "vmInstance"

	desc """更新一个云主机的信息"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVmInstance"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateVmInstance"
					desc "云主机名"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateVmInstance"
					desc "云主机详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "state"
					enclosedIn "updateVmInstance"
					desc "云主机状态。**注意**，通常是不应该直接更新云主机状态，否则会导致ZStack对云主机状态发生误判。该字段只应该用在云主机真实状态和ZStack记录状态发生了不一致，而ZStack的同步机制已经失效时（通常意味着bug），并管理员应该完全理解使用该字段的后果时。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("Stopped","Running")
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn "updateVmInstance"
					desc "默认三层网络UUID"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "platform"
					enclosedIn "updateVmInstance"
					desc "云主机平台类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("Linux","Windows","Other","Paravirtualization","WindowsVirtio")
				}
				column {
					name "cpuNum"
					enclosedIn "updateVmInstance"
					desc "云主机CPU数量。需停止/启动云主机后生效"
					location "body"
					type "Integer"
					optional true
					since "zsv 0.6"
				}
				column {
					name "memorySize"
					enclosedIn "updateVmInstance"
					desc "云主机内存大小。需停止/启动云主机后生效"
					location "body"
					type "Long"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "guestOsType"
					enclosedIn "updateVmInstance"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.11.2"
				}
				column {
					name "reservedMemorySize"
					enclosedIn "updateVmInstance"
					desc ""
					location "body"
					type "Long"
					optional true
					since "zsv 3.17.21"
				}
				column {
					name "allocatorStrategy"
					enclosedIn "updateVmInstance"
					desc "虚拟机分配策略"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.10"
				}
			}
		}

		response {
			clz APIUpdateVmInstanceEvent.class
		}
	}
}