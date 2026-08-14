package org.zstack.core.errorcode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
@RestResponse(allTo = "categories")
public class APIGetElaborationCategoriesReply extends APIReply {
    private List<ElaborationCategory> categories = new ArrayList<>();

    public List<ElaborationCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<ElaborationCategory> categories) {
        this.categories = categories;
    }

    public static APIGetElaborationCategoriesReply __example__() {
        APIGetElaborationCategoriesReply reply = new APIGetElaborationCategoriesReply();
        List<ElaborationCategory> c = new ArrayList<>();
        c.add(new ElaborationCategory("ACCOUNT", 5));
        c.add(new ElaborationCategory("BS", 3));
        c.add(new ElaborationCategory("VM", 20));

        reply.setCategories(c);
        return reply;
    }
}
