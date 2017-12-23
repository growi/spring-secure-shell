package de.growi.spring.shell.secure;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.jline.reader.Highlighter;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.Shell;
import org.springframework.shell.TerminalBackedInputProvider;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.shell.jline.JLineShellAutoConfiguration.CompleterAdapter;
import org.springframework.stereotype.Component;


public class InputProviderFactory {

	
	public  History history;
	
	public CompleterAdapter completerAdapter;

	public Parser parser;

	public PromptProvider promptProvider;

	public TerminalBackedInputProvider create(Terminal terminal, Shell shell) throws IOException {
		return new SshInputProvider(terminal, shell);
	}

	private LineReader createLineReader(Terminal terminal, Shell shell) {
		LineReaderBuilder lineReaderBuilder = LineReaderBuilder.builder().terminal(terminal).appName("Spring Shell")
				.completer(completerAdapter).history(history).highlighter(new Highlighter() {

					@Override
					public AttributedString highlight(LineReader reader, String buffer) {
						int l = 0;
						String best = null;
						for (String command : shell.listCommands().keySet()) {
							if (buffer.startsWith(command) && command.length() > l) {
								l = command.length();
								best = command;
							}
						}
						if (best != null) {
							return new AttributedStringBuilder(buffer.length()).append(best, AttributedStyle.BOLD)
									.append(buffer.substring(l)).toAttributedString();
						} else {
							return new AttributedString(buffer,
									AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
						}
					}
				}).parser(parser);

		return lineReaderBuilder.build();
	}

	/**
	 * Sanitize the buffer input given the customizations applied to the JLine
	 * parser (<em>e.g.</em> support for line continuations, <em>etc.</em>)
	 */
	static List<String> sanitizeInput(List<String> words) {
		words = words.stream().map(s -> s.replaceAll("^\\n+|\\n+$", "")) // CR at beginning/end of line introduced by
																			// backslash continuation
				.map(s -> s.replaceAll("\\n+", " ")) // CR in middle of word introduced by return inside a quoted string
				.collect(Collectors.toList());
		return words;
	}

	public class SshInputProvider implements TerminalBackedInputProvider {

		private Terminal terminal;
		private LineReader lineReader;

		public SshInputProvider(Terminal terminal, Shell shell) throws IOException {
			this.terminal = terminal;
			this.lineReader = createLineReader(terminal, shell);
		}

		@Override
		public Input readInput() {
			try {
				AttributedString prompt = promptProvider.getPrompt();
				lineReader.readLine(prompt.toAnsi(lineReader.getTerminal()));
			} catch (UserInterruptException e) {
				if (e.getPartialLine().isEmpty()) {
					throw new ExitRequest(1);
				} else {
					return Input.EMPTY;
				}
			}

			return new Input() {

				private ParsedLine parsedLine = lineReader.getParsedLine();

				@Override
				public String rawText() {
					return parsedLine.line();
				}

				@Override
				public List<String> words() {
					return InputProviderFactory.sanitizeInput(parsedLine.words());
				}
			};
		}

		@Override
		public Terminal getTerminal() {
			return terminal;
		}
	}
}
