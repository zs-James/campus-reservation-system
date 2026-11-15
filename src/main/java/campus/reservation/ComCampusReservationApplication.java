package campus.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ComCampusReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComCampusReservationApplication.class, args);
    }

}
