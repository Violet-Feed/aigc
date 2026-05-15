package violet.aigc.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AigcApplication {

    public static void main(String[] args) {
        SpringApplication.run(AigcApplication.class, args);
    }

}
