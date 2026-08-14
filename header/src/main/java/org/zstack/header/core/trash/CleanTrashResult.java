package org.zstack.header.core.trash;

import org.zstack.header.rest.SDK;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/12/26.
 */
@SDK
public class CleanTrashResult {
    private List<String> resourceUuids = Collections.synchronizedList(new ArrayList<>());
    private List<String> details = new ArrayList<>();
    private Long size = 0L;

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
