package org.zstack.header.configuration

import org.zstack.header.configuration.APICreateInstanceOfferingEvent

doc {
	title "CreateInstanceOffering"

	category "configuration"

	desc """创建云主机规格"""

	rest {
		request {
			url "POST /v1/instance-offerings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateInstanceOfferingMsg.class

			desc """创建云主机规格"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "cpuNum"
					enclosedIn "params"
					desc "CPU数目"
					location "body"
					type "int"
					optional false
					since "zsv 0.6"
				}
				column {
					name "memorySize"
					enclosedIn "params"
					desc "内存大小, 单位Byte"
					location "body"
					type "long"
					optional false
					since "zsv 0.6"
				}
				column {
					name "allocatorStrategy"
					enclosedIn "params"
					desc "分配策略"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "sortKey"
					enclosedIn "params"
					desc "排序键"
					location "body"
					type "int"
					optional true
					since "zsv 0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
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
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4.0"
				}
				column {
					name "reservedMemorySize"
					enclosedIn "params"
					desc ""
					location "body"
					type "long"
					optional true
					since "zsv 3.17.21"
				}
			}
		}

		response {
			clz APICreateInstanceOfferingEvent.class
		}
	}
}