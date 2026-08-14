package org.zstack.header.vipQos

import java.lang.Integer
import java.lang.Long
import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "VIPQos清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.2"
	}
	field {
		name "vipUuid"
		desc "VIP UUID"
		type "String"
		since "2.2"
	}
	field {
		name "port"
		desc ""
		type "Integer"
		since "2.2"
	}
	field {
		name "inboundBandwidth"
		desc ""
		type "Long"
		since "2.2"
	}
	field {
		name "outboundBandwidth"
		desc ""
		type "Long"
		since "2.2"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "2.2"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.2"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.2"
	}
}
