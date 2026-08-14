package org.zstack.header.vm

import org.zstack.header.vm.APIGetSpiceCertificatesReply

doc {
	title "获取spice的CA证书(GetSpiceCertificates)"

	category "vmInstance"

	desc """开启Spice TLS加密后，spice客户端需要下载CA证书才能访问云主机控制台"""

	rest {
		request {
			url "GET /v1/spice/certificates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetSpiceCertificatesMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 3.7"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 3.7"
				}
			}
		}

		response {
			clz APIGetSpiceCertificatesReply.class
		}
	}
}