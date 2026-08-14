package org.zstack.core.checkpoint;

import java.io.Serializable;

public class CheckPointContext implements Serializable {
    private Object output;
    private Object[] inputs;
    
    CheckPointContext(Object[] inputs, Object output) {
        this.output = output;
        this.inputs = inputs;
    }
    
    public Object getOutput() {
        return output;
    }
    

    public Object[] getInputs() {
        return inputs;
    }
}
