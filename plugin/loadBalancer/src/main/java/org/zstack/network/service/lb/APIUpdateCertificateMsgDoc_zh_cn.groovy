package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIUpdateCertificateEvent

doc {
	title "UpdateCertificate"

	category "loadBalancer"

	desc """更新证书信息"""

	rest {
		request {
			url "PUT /v1/certificates/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateCertificateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateCertificate"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "name"
					enclosedIn "updateCertificate"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "description"
					enclosedIn "updateCertificate"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "resourceUuid"
					enclosedIn "updateCertificate"
					desc ""
					location "body"
					type "String"
					optional true
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
				column {
					name "tagUuids"
					enclosedIn "updateCertificate"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIUpdateCertificateEvent.class
		}
	}
}