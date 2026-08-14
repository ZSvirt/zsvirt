package org.zstack.vpc

import org.zstack.vpc.APIGetVirtualRouterSoftwareVersionReply

doc {
	title "GetVirtualRouterSoftwareVersion"

	category "vpc"

	desc """获取VPC软件版本"""

	rest {
		request {
			url "GET /v1/vpc/virtual-routers/softwareversion"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVirtualRouterSoftwareVersionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "query"
					type "String"
					optional true
					since "3.15"
				}
				column {
					name "softwareName"
					enclosedIn ""
					desc "VPC软件名称"
					location "query"
					type "String"
					optional false
					since "3.15"
					values ("IPsec")
				}
				column {
					name "needUpdate"
					enclosedIn ""
					desc "是否需要升级"
					location "query"
					type "Boolean"
					optional true
					since "3.15"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.15"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.15"
				}
			}
		}

		response {
			clz APIGetVirtualRouterSoftwareVersionReply.class
		}
	}
}