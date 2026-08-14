package org.zstack.header.agent;

/**
 * Created by MaJin on 2019/7/17.
 */
public interface CancelCommand {

    /***
     *
     * @param cancellationApiId agent will cancel jobs which started by it.
     */
    void setCancellationApiId(String cancellationApiId);
}
