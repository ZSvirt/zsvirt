package org.zstack.core.errorcode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.string.ErrorCodeElaboration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
@RestResponse(allTo = "contents")
public class APIGetElaborationsReply extends APIReply {
    private List<ElaborationContent> contents = new ArrayList<>();

    public List<ElaborationContent> getContents() {
        return contents;
    }

    public void setContents(List<ElaborationContent> contents) {
        this.contents = contents;
    }

    public static APIGetElaborationsReply __example__() {
        APIGetElaborationsReply reply = new APIGetElaborationsReply();
        List<ElaborationContent> e = new ArrayList<>();
        ErrorCodeElaboration elaboration = new ErrorCodeElaboration();
        elaboration.setCategory("HOST");
        elaboration.setCode("1003");
        elaboration.setRegex("host[uuid:%s, name:%s] is in status[%s], cannot perform required operation");
        elaboration.setMessage_cn("物理机正处于[停止]状态，不能进行该操作");
        elaboration.setMessage_en("Host is in status[Stopped], cannot perform required operation");

        e.add(new ElaborationContent(elaboration));
        reply.setContents(e);
        return reply;
    }
}
