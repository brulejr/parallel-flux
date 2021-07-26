package io.jrb.labs.parallelflux;

import io.jrb.labs.parallelflux.datafill.TodoDatafill;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ TodoDatafill.class })
public class ParallelFluxApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParallelFluxApplication.class, args);
	}

}
