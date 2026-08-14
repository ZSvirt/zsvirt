package org.zstack.guesttools.advanced

import org.zstack.guesttools.advanced.APICreateVmCustomSpecificationEvent

doc {
	title "CreateVmCustomSpecification"

	category "guest.tools"

	desc """创建虚拟机自定义操作系统规范"""

	rest {
		request {
			url "POST /v1/vm-custom-specifications"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmCustomSpecificationMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.10.18"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "platform"
					enclosedIn "params"
					desc "虚拟机操作系统平台类型"
					location "body"
					type "String"
					optional false
					since "4.10.18"
					values ("Linux","Windows","WindowsVirtio","Other","Paravirtualization")
				}
				column {
					name "hostname"
					enclosedIn "params"
					desc "主机名称"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "rootPassword"
					enclosedIn "params"
					desc "管理员密码"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "generateSID"
					enclosedIn "params"
					desc "是否生成SID"
					location "body"
					type "Boolean"
					optional true
					since "4.10.18"
				}
				column {
					name "domainMode"
					enclosedIn "params"
					desc "加域模式"
					location "body"
					type "String"
					optional true
					since "4.10.18"
					values ("WorkGroup","Domain")
				}
				column {
					name "domainName"
					enclosedIn "params"
					desc "域名称或工作组名称"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "domainUsername"
					enclosedIn "params"
					desc "域用户名"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "domainPassword"
					enclosedIn "params"
					desc "域用户密码"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "organization"
					enclosedIn "params"
					desc "组织单位"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
					since "4.10.18"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.18"
				}
			}
		}

		response {
			clz APICreateVmCustomSpecificationEvent.class
		}
	}
}