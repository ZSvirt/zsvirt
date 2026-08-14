package org.zstack.core.puppet;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PokePuppetAgentJob implements Job {
	private static final CLogger logger = Utils.getLogger(PokePuppetAgentJob.class);

	@JobContext
	private PuppetPokeAgentMsg msg;
	@JobContext
	private String puppetMasterCertName;
	@Autowired
	private CloudBus bus;

	PokePuppetAgentJob(PuppetPokeAgentMsg msg, String puppetMasterCertName) {
		this.msg = msg;
		this.puppetMasterCertName = puppetMasterCertName;
	}

	@Override
	public void run(ReturnValueCompletion<Object> complete) {
		PuppetPokeAgentReply reply = new PuppetPokeAgentReply();
        /*
		try {
			String cmd = String.mediaType("puppet agent --certname %s --no-daemonize --onetime --server %s --verbose", msg.getNodeName(), puppetMasterCertName);
			String log = Ssh.run(msg.getHostname(), msg.getUsername(), msg.getPassword(), cmd);
			logger.debug(log);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
            reply.setSuccess(false);
		}
		*/
		bus.reply(msg, reply);
		complete.success(null);
	}
}
