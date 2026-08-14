package org.zstack.vpc

import org.zstack.vpc.APIGetVpcAttachedVipReply

doc {
	title "GetVpcAttachedVip"

	category "vpc"

	desc """获取VPC云路由已关联的虚拟IP"""

	rest {
		request {
			url "POST /v1/vpc/virtual-routers/{uuid}/attached-vip"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVpcAttachedVipMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.11"
				}
				column {
					name "limit"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "3.11"
				}
				column {
					name "start"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "3.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.11"
				}
			}
		}

		response {
			clz APIGetVpcAttachedVipReply.class
		}
	}
}