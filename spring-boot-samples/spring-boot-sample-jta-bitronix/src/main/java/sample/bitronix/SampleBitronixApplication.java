package sample.bitronix;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import sample.bitronix.service.AccountService;

/**
 * This sample uses the <A href="http://docs.codehaus.org/display/BTM/Home">Bitronix
 * JTA</A> engine. Bitronix is a single Maven dependency. You can use the
 * {@code org.springframework.boot:spring-boot-starter-jta-bitronix} starter or simply
 * import the dependency itself. This code works against the very stable Bitronix 2.1
 * line.
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleBitronixApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleBitronixApplication.class, args);
	}

	@Bean
	public CommandLineRunner jpa(AccountService accountService) {
		return new AccountServiceCommandLineRunner(accountService);
	}
}
