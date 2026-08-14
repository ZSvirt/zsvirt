package org.zstack.core.job;

public class JobQueueNameRoundRobinGenerator {
	private final String[] queueNames;
	private volatile int index = 0;
	
	public JobQueueNameRoundRobinGenerator(String[] queueNames) {
	    super();
	    this.queueNames = queueNames;
	    assert queueNames != null && queueNames.length > 0;
    }
	
	public String getNextQueueName() {
		String name = queueNames[index++];
		index = (index >= queueNames.length ? 0 : index);
		return name;
	}
}
