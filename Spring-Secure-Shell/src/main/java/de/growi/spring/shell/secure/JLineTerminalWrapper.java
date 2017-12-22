
 
package de.growi.spring.shell.secure;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.apache.sshd.common.RuntimeSshException;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.ValidateUtils;
import org.apache.sshd.common.util.io.IoUtils;
import org.apache.sshd.common.util.logging.AbstractLoggingBean;
import org.apache.sshd.common.util.threads.ThreadUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.shell.InputProvider;
import org.springframework.shell.Shell;

public class JLineTerminalWrapper extends AbstractLoggingBean implements Command, SessionAware {

    private Shell shell;
    private InputProviderFactory inputProviderFactory;
    private Terminal term;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;

    public JLineTerminalWrapper(Shell shell, InputProviderFactory inputProviderFactory) {
    	this.shell = shell;
    	this.inputProviderFactory = inputProviderFactory;
    }
    
    @Override
    public void setInputStream(InputStream in) {
    	this.log.info("setting in: " + in);
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
    	this.log.info("setting out: " + out);
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
    	this.log.info("setting err: " + err);
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
    	this.log.info("setting callback: " + callback);
    }

    @Override
    public void setSession(ServerSession session) {
    	this.log.info("setting session: " + session);
        //TODO: add session handling
        //shell.setSession(session);
    }

    @Override
    public synchronized void start(Environment env) throws IOException {
    	this.log.info("start");
    	term = TerminalBuilder.builder().streams(in, out).type("Secure Shell").jna(false).jansi(false).build();
    	InputProvider ip = inputProviderFactory.create(term, shell);
    	new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					shell.run(ip);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
    }

    @Override
    public synchronized void destroy() throws Exception {
    	this.log.info("destroy");
        Throwable err = null;
        try {
        	term.close();
        } catch (Throwable e) {
            log.warn("destroy({}) failed ({}) to destroy shell: {}",
                     this, e.getClass().getSimpleName(), e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("destroy(" + this + ") shell destruction failure details", e);
            }
            err = GenericUtils.accumulateException(err, e);
        }

        if (err != null) {
            if (err instanceof Exception) {
                throw (Exception) err;
            } else {
                throw new RuntimeSshException(err);
            }
        }
    }
   
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + String.valueOf(term);
    }
    
    public Terminal getTerminal() {
    	return term;
    }
}