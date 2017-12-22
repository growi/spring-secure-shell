package de.growi.spring.shell.secure;

import org.apache.sshd.server.SshServer;
import org.jline.reader.History;
import org.jline.reader.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.shell.jline.JLineShellAutoConfiguration.CompleterAdapter;

@SpringBootApplication
public class SpringSecureShellApplication {
	
	
	public static void main(String[] args) {
		SpringApplication.run(SpringSecureShellApplication.class, args);
	}
	
	@Bean
	public InputProviderFactory inputProviderFactory(@Lazy History history, CompleterAdapter completerAdapter, Parser parser, PromptProvider promptProvider) {
		InputProviderFactory ipf = new InputProviderFactory();
		ipf.history = history;
		ipf.completerAdapter = completerAdapter;
		ipf.parser = parser;
		ipf.promptProvider = promptProvider;
		return ipf;
	}
}
