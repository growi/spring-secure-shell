package de.growi.spring.shell.secure;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.sshd.common.Factory;
import org.apache.sshd.common.util.threads.ThreadUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.InvertedShellWrapper;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.ResultHandler;
import org.springframework.shell.Shell;
import org.springframework.shell.result.TerminalAwareResultHandler;
import org.springframework.stereotype.Component;

@Component
public class SshdConfiguration {

	private static Logger logger = LoggerFactory.getLogger(SshdConfiguration.class);

	@Autowired
	@Qualifier("main")
	ResultHandler resultHandler;

	@Autowired
	Shell shell;

	@Autowired
	InputProviderFactory inputProviderFactory;

	List<Shell> shellRegistry = new ArrayList<>();

	@Bean
	public SshServer shhServer() {

		logger.info("Initializing Secure Shell Server");

		SshServer sshd = null;

		sshd = SshServer.setUpDefaultServer();
		sshd.setPort(8022);
		sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
			@Override
			public boolean authenticate(String username, String password, ServerSession session) {
				boolean accept = "root".equals(username) && "root".equals(password);

				String action = accept ? "Accepted" : "Rejected";
				String message = action + " user=" + username + ",password=" + password + " from "
						+ session.getIoSession().getRemoteAddress();
				logger.info(message);
				return accept;
			}
		});
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser")));

		sshd.setShellFactory(new Factory<Command>() {

			@Override
			public Command create() {

				JLineTerminalWrapper wrapper = new JLineTerminalWrapper(shell, inputProviderFactory);

				return wrapper;
			}
		});

		sshd.setCommandFactory(new ScpCommandFactory());
		try {
			sshd.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sshd;
	}
}
