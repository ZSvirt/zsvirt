package org.zstack.network.service.lb

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.service.lb.CertificateInventory

doc {

	title "证书清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.network.service.lb.APIUpdateCertificateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.network.service.lb.APIUpdateCertificateEvent.inventory"
		desc "null"
		type "CertificateInventory"
		since "2.3"
		clz CertificateInventory.class
	}
}
