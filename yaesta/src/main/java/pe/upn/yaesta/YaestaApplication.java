package pe.upn.yaesta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YaestaApplication {

	public static void main(String[] args) {
		SpringApplication.run(YaestaApplication.class, args);
	}

}
