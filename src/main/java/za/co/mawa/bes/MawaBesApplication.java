package za.co.mawa.bes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;


@SpringBootApplication
@Configuration
 public class MawaBesApplication extends SpringBootServletInitializer {
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(MawaBesApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(MawaBesApplication.class, args);
	}

}
