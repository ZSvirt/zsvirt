package org.zstack.header.baremetal.instance

import org.zstack.header.baremetal.instance.APICreateBaremetalInstanceEvent

doc {
	title "CreateBaremetalInstance"

	category "baremetal.instance"

	desc """创建裸机实例"""

	rest {
		request {
			url "POST /v1/baremetal/instances"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateBaremetalInstanceMsg.class

			desc """创建裸机实例"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "chassisUuid"
					enclosedIn "params"
					desc "裸机设备UUID"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "imageUuid"
					enclosedIn "params"
					desc "镜像UUID"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "templateUuid"
					enclosedIn "params"
					desc "预配置模板UUID"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "username"
					enclosedIn "params"
					desc "系统用户"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "系统密码"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "nicCfgs"
					enclosedIn "params"
					desc "裸机网络配置"
					location "body"
					type "Map"
					optional true
					since "2.6.0"
				}
				column {
					name "bondingCfgs"
					enclosedIn "params"
					desc "网卡绑定配置"
					location "body"
					type "Map"
					optional true
					since "3.4.0"
				}
				column {
					name "customConfigurations"
					enclosedIn "params"
					desc "自定义变量"
					location "body"
					type "Map"
					optional true
					since "3.4.0"
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc "裸机实例创建策略"
					location "body"
					type "String"
					optional true
					since "2.6.0"
					values ("InstantStart","JustCreate")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，则裸机实例会以该字段值作为UUID。"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.6.0"
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
					name "platform"
					enclosedIn "params"
					desc "裸金属实例系统盘平台类型"
					location "body"
					type "String"
					optional true
					since "4.10.13"
					values ("Linux","Windows","WindowsVirtio","Other","Paravirtualization")
				}
			}
		}

		response {
			clz APICreateBaremetalInstanceEvent.class
		}
	}
}