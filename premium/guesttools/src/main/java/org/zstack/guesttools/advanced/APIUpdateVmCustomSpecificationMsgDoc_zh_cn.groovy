package org.zstack.guesttools.advanced

import org.zstack.guesttools.advanced.APIUpdateVmCustomSpecificationEvent

doc {
	title "UpdateVmCustomSpecification"

	category "guest.tools"

	desc """更新虚拟机自定义操作系统规范"""

	rest {
		request {
			url "PUT /v1/vm-custom-specifications/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVmCustomSpecificationMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVmCustomSpecification"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.10.18"
				}
				column {
					name "name"
					enclosedIn "updateVmCustomSpecification"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "description"
					enclosedIn "updateVmCustomSpecification"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "hostname"
					enclosedIn "updateVmCustomSpecification"
					desc "主机名称"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "rootPassword"
					enclosedIn "updateVmCustomSpecification"
					desc "管理员密码"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "generateSID"
					enclosedIn "updateVmCustomSpecification"
					desc "是否生成SID"
					location "body"
					type "Boolean"
					optional true
					since "4.10.18"
				}
				column {
					name "domainMode"
					enclosedIn "updateVmCustomSpecification"
					desc "加域模式"
					location "body"
					type "String"
					optional true
					since "4.10.18"
					values ("WorkGroup","Domain")
				}
				column {
					name "domainName"
					enclosedIn "updateVmCustomSpecification"
					desc "域名称或工作组名称"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "domainUsername"
					enclosedIn "updateVmCustomSpecification"
					desc "域用户名"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "domainPassword"
					enclosedIn "updateVmCustomSpecification"
					desc "域用户密码"
					location "body"
					type "String"
					optional true
					since "4.10.18"
				}
				column {
					name "organization"
					enclosedIn "updateVmCustomSpecification"
					desc "组织单位"
					location "body"
					type "String"
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
			clz APIUpdateVmCustomSpecificationEvent.class
		}
	}
}