package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIDeleteCertificateEvent

doc {
	title "DeleteCertificate"

	category "loadBalancer"

	desc """删除证书"""

	rest {
		request {
			url "DELETE /v1/certificates/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteCertificateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIDeleteCertificateEvent.class
		}
	}
}