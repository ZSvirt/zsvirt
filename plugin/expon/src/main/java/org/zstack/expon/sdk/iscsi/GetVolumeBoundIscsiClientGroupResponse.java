package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.ExponResponse;

import java.util.List;


/**
 * @example {
 * "clients": [{
 * "chap_username": "",
 * "create_time": 1705314720041,
 * "description": "",
 * "id": "413f59f4-90ae-4ee7-9388-07065d25bd3e",
 * "is_chap": false,
 * "is_readonly": false,
 * "name": "iscsi_zstack_heartbeat",
 * "node_num": 3,
 * "update_time": 1708411159610,
 * "volume_id": "40b388e3-c7ee-4c44-bed9-6d4b88088f8f"
 * }],
 * "count": 1,
 * "message": "",
 * "ret_code": "0",
 * "total": 1
 * }
 */
public class GetVolumeBoundIscsiClientGroupResponse extends ExponResponse {
    private int count;
    private int total;
    private List<LunBoundIscsiClientGroupModule> clients;
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<LunBoundIscsiClientGroupModule> getClients() {
        return clients;
    }

    public void setClients(List<LunBoundIscsiClientGroupModule> clients) {
        this.clients = clients;
    }
}
