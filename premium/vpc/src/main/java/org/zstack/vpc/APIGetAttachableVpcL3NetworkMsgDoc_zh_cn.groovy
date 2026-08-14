package org.zstack.vpc

import org.zstack.vpc.APIGetAttachableVpcL3NetworkReply

doc {
	title "GetAttachableVpcL3Network"

	category "vpc"

	desc """获取VPC云路由可加载的三层网络"""

	rest {
		request {
			url "POST /v1/vpc/virtual-routers/{uuid}/attachable-vpc-l3s"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAttachableVpcL3NetworkMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIGetAttachableVpcL3NetworkReply.class
		}
	}
}