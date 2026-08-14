package org.zstack.identity.imports.message;

import org.zstack.header.message.MessageReply;
import org.zstack.identity.imports.header.ImportAccountResult;

import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
public class ImportThirdPartyAccountReply extends MessageReply {
    private List<ImportAccountResult> results;

    public List<ImportAccountResult> getResults() {
        return results;
    }

    public void setResults(List<ImportAccountResult> results) {
        this.results = results;
    }
}
