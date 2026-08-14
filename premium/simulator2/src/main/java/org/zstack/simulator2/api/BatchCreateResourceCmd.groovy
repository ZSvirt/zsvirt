package org.zstack.simulator2.api

/**
 * Created by xing5 on 2017/9/19.
 */
class BatchCreateResourceCmd {
    static class BatchResource {
        String type
        LinkedHashMap data
    }

    List<BatchResource> resources
}
