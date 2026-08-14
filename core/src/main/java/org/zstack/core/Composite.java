package org.zstack.core;

import java.util.List;

public interface Composite {
    Composite getParent();
    
    List<Composite> getChildren();
    
    void accept(CompositeVisitor ops);
}
