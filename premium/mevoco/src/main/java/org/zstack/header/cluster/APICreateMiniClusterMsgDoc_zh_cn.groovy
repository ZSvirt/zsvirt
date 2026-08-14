package org.zstack.header.cluster

import org.zstack.header.cluster.APICreateMiniClusterEvent

doc {
	title "CreateMiniCluster"

	category "mevoco"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/mini-clusters"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateMiniClusterMsg.class

			desc """"""

			params {

				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "hostManagementIps"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "username"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "password"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "sshPort"
					enclosedIn "params"
					desc ""
					location "body"
					type "int"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "hypervisorType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("KVM","Simulator")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APICreateMiniClusterEvent.class
		}
	}
}