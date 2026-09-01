package org.zstack.identity.imports.message;

import org.zstack.header.message.MessageReply;
import org.zstack.identity.imports.header.UnbindThirdPartyAccountResult;

import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/06/03
 */
public class UnbindThirdPartyAccountReply extends MessageReply {
    private List<UnbindThirdPartyAccountResult> results;

    public List<UnbindThirdPartyAccountResult> getResults() {
        return results;
    }

    public void setResults(List<UnbindThirdPartyAccountResult> results) {
        this.results = results;
    }
}
