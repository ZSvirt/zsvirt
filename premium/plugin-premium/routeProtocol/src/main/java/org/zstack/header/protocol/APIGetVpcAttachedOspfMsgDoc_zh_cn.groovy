package org.zstack.header.protocol

import org.zstack.header.protocol.APIGetVpcAttachedOspfReply

doc {
	title "GetVpcAttachedOspf"

	category "routeProtocol"

	desc """获取VPC云路由已关联的Ospf"""

	rest {
		request {
			url "POST /v1/vpc/virtual-routers/{uuid}/attached-ospf"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVpcAttachedOspfMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "limit"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "start"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
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
			clz APIGetVpcAttachedOspfReply.class
		}
	}
}