package org.zstack.core.job;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;

import java.util.List;

public interface JobQueueFacade {
   void execute(String queueName, String owner, Job job); 
   
   <T> void execute(String queueName, String owner, Job job, ReturnValueCompletion<T> complete, Class<? extends T> returnType); 
   
   void execute(String queueName, String owner, Job job, Completion complete); 
   
   void deleteJobQueue(String queueName);
   
   void evictOwner(String owner);
   
   List<String> listAllQueue();
   
   List<String> listQueue(String namePattern);
   
   long getPendingJobNumber(String queueName);
   
   List<String> listQueueHasPendingJob();
   
   boolean startQueueIfPendingJob(String queueName, String owner);
   
   boolean startQueueIfPendingJob(String queueName, String owner, boolean newThread);
}
