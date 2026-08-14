package org.zstack.header.vm

import org.zstack.header.vm.APICreateTemplatedVmInstanceFromVmInstanceEvent

doc {
	title "CreateTemplatedVmInstanceFromVmInstance"

	category "mevoco"

	desc """从虚拟机模板创建虚拟机"""

	rest {
		request {
			url "POST /v1/vm-instances/{vmInstanceUuid}/create-templated-vmInstance"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateTemplatedVmInstanceFromVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "虚拟机模板名称"
					location "body"
					type "String"
					optional false
					since "4.2.6"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "虚拟机UUID"
					location "url"
					type "String"
					optional false
					since "4.2.6"
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional true
					since "4.2.6"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "4.2.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.10.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.2.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.2.6"
				}
			}
		}

		response {
			clz APICreateTemplatedVmInstanceFromVmInstanceEvent.class
		}
	}
}