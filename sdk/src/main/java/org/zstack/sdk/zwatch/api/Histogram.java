package org.zstack.sdk.zwatch.api;



public class Histogram  {

    public long time;
    public void setTime(long time) {
        this.time = time;
    }
    public long getTime() {
        return this.time;
    }

    public long count;
    public void setCount(long count) {
        this.count = count;
    }
    public long getCount() {
        return this.count;
    }

    public java.util.List tags;
    public void setTags(java.util.List tags) {
        this.tags = tags;
    }
    public java.util.List getTags() {
        return this.tags;
    }

}
