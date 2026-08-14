package org.zstack.header.agent;

public interface ReloadableCommand {
    /***
     *
     * @param identificationCode agent will identify reloadable task by it.
     */
    void setIdentificationCode(String identificationCode);
}
