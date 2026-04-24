package io.github.kizulog_community.kizulog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;

@SpringBootApplication(exclude = {
	    SecurityAutoConfiguration.class,
	    SecurityFilterAutoConfiguration.class,
	    UserDetailsServiceAutoConfiguration.class
	})
public class KizulogApplication {

	public static void main(String[] args) {
		SpringApplication.run(KizulogApplication.class, args);
	}

}
