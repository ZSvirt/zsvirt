package org.zstack.vpc

import org.zstack.vpc.APIUpdateVirtualRouterSoftwareVersionEvent

doc {
	title "UpdateVirtualRouterSoftwareVersion"

	category "vpc"

	desc """VPC软件升级"""

	rest {
		request {
			url "POST /v1/vpc/virtual-routers/{uuid}/softwareversion"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVirtualRouterSoftwareVersionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.15"
				}
				column {
					name "softwareName"
					enclosedIn "params"
					desc "VPC软件名称"
					location "body"
					type "String"
					optional false
					since "3.15"
					values ("IPsec")
				}
				column {
					name "targetVersion"
					enclosedIn "params"
					desc "目标版本"
					location "body"
					type "String"
					optional false
					since "3.15"
					values ("4.5.2","5.9.4")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.15"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.15"
				}
			}
		}

		response {
			clz APIUpdateVirtualRouterSoftwareVersionEvent.class
		}
	}
}