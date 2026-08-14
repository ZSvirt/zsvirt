package org.zstack.core.defer;

import java.util.EmptyStackException;
import java.util.Stack;


class DeferStack<Runnable> extends Stack<Runnable> {
    private Stack<Integer> stackTop = new Stack<Integer>();
    
    void pushTop() {
        stackTop.push(this.size());
    }
    
    int popTop() {
        try {
            return stackTop.pop();
        } catch (EmptyStackException e) {
            return 0;
        }
    }
    
    int getTop() {
        try {
            return stackTop.peek();
        } catch (EmptyStackException e) {
            return 0;
        }
    }
    
    void revert(int top) {
        if (this.size() > top) {
            this.removeRange(top, this.size()); 
        }
    }
}
