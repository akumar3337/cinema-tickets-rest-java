package uk.gov.dwp.engineering.recruitment;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.dwp.engineering.recruitment.CinemaTicketsApplication;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@CucumberContextConfiguration
@SpringBootTest(classes = CinemaTicketsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    @MockitoSpyBean
    private PaymentService paymentService;

    @MockitoSpyBean
    private SeatReservationService seatReservationService;
}

