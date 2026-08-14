package org.zstack.header.host;

import java.util.List;

/**
 * Created by MaJin on 2020/1/8.
 */
public class AddHostsFromFileResult {
    private boolean canceled;
    private List<AddHostFromFileResult> results;

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public List<AddHostFromFileResult> getResults() {
        return results;
    }

    public void setResults(List<AddHostFromFileResult> results) {
        this.results = results;
    }
}
