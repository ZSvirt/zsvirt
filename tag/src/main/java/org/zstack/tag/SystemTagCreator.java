package org.zstack.tag;

import org.zstack.header.tag.SystemTagInventory;

import java.util.Map;

/**
 * Created by xing5 on 2016/12/3.
 */
public abstract class SystemTagCreator {
    protected String resourceUuid;
    public Class resourceClass;
    public boolean inherent;
    /**
     * delete existing tags and recreate
     */
    public boolean recreate;
    public boolean quickRecreate = true;
    /**
     * return null if there has been a same tag
     */
    public boolean ignoreIfExisting;
    public String tag;
    /**
     * exception is raised if there has been a same tag and @ignoreIfExisting = false
     */
    public boolean unique = true;

    public abstract SystemTagInventory create();
    public abstract void setTagByTokens(Map tokens);
}
