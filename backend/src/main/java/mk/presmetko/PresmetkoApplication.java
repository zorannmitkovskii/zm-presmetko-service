package mk.presmetko;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Еден JAR, два профила: {@code api} (HTTP) и {@code worker} (задачи, scheduler, УЈП). Креденцијалите за УЈП
 * постојат само во {@code worker}.
 */
@SpringBootApplication
public class PresmetkoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PresmetkoApplication.class, args);
    }
}
