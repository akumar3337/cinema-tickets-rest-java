package uk.gov.dwp.engineering.recruitment;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import uk.gov.dwp.engineering.recruitment.booking.BookingValidator;
import uk.gov.dwp.engineering.recruitment.booking.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.booking.TicketRequestAggregator;
import uk.gov.dwp.engineering.recruitment.exception.RestExceptionHandler;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@SpringJUnitWebConfig
@TestPropertySource(properties = {
        "cinema.tickets.prices.adult=25.99",
        "cinema.tickets.prices.child=17.50",
        "cinema.tickets.prices.infant=0.00",
        "cinema.tickets.limits.max-per-booking=25"
})
class CinemaTicketsControllerTest {

    private static final String BOOKINGS_URL = "/cinema/bookings";
    private static final Long ACCOUNT_ID = 1234L;

    private static final String ACCOUNT_MESSAGE =
            "A valid account id (greater than zero) is required";
    private static final String NO_REQUESTS_MESSAGE = "At least one ticket request is required";
    private static final String NO_TICKETS_MESSAGE = "At least one ticket must be requested";
    private static final String MAX_MESSAGE = "A maximum of 25 tickets can be purchased at a time";
    private static final String ADULT_REQUIRED_MESSAGE =
            "Child and infant tickets cannot be purchased without an adult ticket";
    private static final String LAP_MESSAGE =
            "Each infant must sit on an adult's lap: infants cannot outnumber adults";

    /**
     * Spring wires the real controller, exception handler, service, aggregator, validator and
     * calculator, with prices and limits read from the test properties.
     */
    @Configuration
    @EnableWebMvc
    @Import({CinemaTicketsServiceImpl.class, TicketRequestAggregator.class, BookingValidator.class,
            TicketPriceCalculator.class, CinemaTicketsController.class,
            RestExceptionHandler.class})
    static class TestConfig {
    }

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private SeatReservationService seatReservationService;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("acceptedBookings")
    void createsBookingForValidRequests(final String description, final String body,
                                        final int expectedSeats, final String expectedCost) throws Exception {
        mockMvc.perform(post(BOOKINGS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID.intValue()))
                .andExpect(jsonPath("$.seatCount").value(expectedSeats))
                .andExpect(jsonPath("$.totalCost").value(Double.parseDouble(expectedCost)));

        verify(paymentService).debitAccount(ACCOUNT_ID, new BigDecimal(expectedCost));
        verify(seatReservationService).reserveSeats(ACCOUNT_ID, (long) expectedSeats);
    }

    static Stream<Arguments> acceptedBookings() {
        return Stream.of(
                Arguments.of("one adult", booking(ACCOUNT_ID, ticket("ADULT", 1)), 1, "25.99"),
                Arguments.of("adult and child",
                        booking(ACCOUNT_ID, ticket("ADULT", 1), ticket("CHILD", 1)), 2, "43.49"),
                Arguments.of("infant is free and has no seat",
                        booking(ACCOUNT_ID, ticket("ADULT", 1), ticket("INFANT", 1)), 1, "25.99"),
                Arguments.of("two adults, one child, one infant",
                        booking(ACCOUNT_ID, ticket("ADULT", 2), ticket("CHILD", 1), ticket("INFANT", 1)),
                        3, "69.48"),
                Arguments.of("repeated lines are aggregated",
                        booking(ACCOUNT_ID, ticket("ADULT", 1), ticket("ADULT", 1)), 2, "51.98"),
                Arguments.of("zero-count line is ignored",
                        booking(ACCOUNT_ID, ticket("ADULT", 1), ticket("CHILD", 0)), 1, "25.99"),
                Arguments.of("exactly 25 adults and children",
                        booking(ACCOUNT_ID, ticket("ADULT", 20), ticket("CHILD", 5)), 25, "607.30"),
                Arguments.of("exactly 25 tickets including infants",
                        booking(ACCOUNT_ID, ticket("ADULT", 13), ticket("INFANT", 12)), 13, "337.87"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rejectedBookings")
    void rejectsInvalidRequestsWithBadRequest(final String description, final String body,
                                              final String expectedDetail) throws Exception {
        mockMvc.perform(post(BOOKINGS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(expectedDetail));

        verifyNoInteractions(paymentService, seatReservationService);
    }

    static Stream<Arguments> rejectedBookings() {
        return Stream.of(
                Arguments.of("zero account id", booking(0, ticket("ADULT", 1)), ACCOUNT_MESSAGE),
                Arguments.of("negative account id", booking(-1, ticket("ADULT", 1)), ACCOUNT_MESSAGE),
                Arguments.of("missing account id",
                        "{\"ticketRequests\":[" + ticket("ADULT", 1) + "]}", ACCOUNT_MESSAGE),
                Arguments.of("empty ticket requests", booking(ACCOUNT_ID), NO_REQUESTS_MESSAGE),
                Arguments.of("missing ticket requests", "{\"accountId\":1234}", NO_REQUESTS_MESSAGE),
                Arguments.of("null ticket requests",
                        "{\"accountId\":1234,\"ticketRequests\":null}", NO_REQUESTS_MESSAGE),
                Arguments.of("null ticket request line", booking(ACCOUNT_ID, "null"),
                        "Ticket requests must not be null"),
                Arguments.of("null ticket type",
                        booking(ACCOUNT_ID, "{\"type\":null,\"ticketCount\":1}"), "Ticket type is required"),
                Arguments.of("negative ticket count", booking(ACCOUNT_ID, ticket("ADULT", -1)),
                        "Ticket count must not be negative"),
                Arguments.of("only zero-count tickets", booking(ACCOUNT_ID, ticket("ADULT", 0)),
                        NO_TICKETS_MESSAGE),
                Arguments.of("child without adult", booking(ACCOUNT_ID, ticket("CHILD", 1)),
                        ADULT_REQUIRED_MESSAGE),
                Arguments.of("infant without adult", booking(ACCOUNT_ID, ticket("INFANT", 1)),
                        ADULT_REQUIRED_MESSAGE),
                Arguments.of("child and infant without adult",
                        booking(ACCOUNT_ID, ticket("CHILD", 1), ticket("INFANT", 1)), ADULT_REQUIRED_MESSAGE),
                Arguments.of("26 adults", booking(ACCOUNT_ID, ticket("ADULT", 26)), MAX_MESSAGE),
                Arguments.of("infants count towards the maximum",
                        booking(ACCOUNT_ID, ticket("ADULT", 20), ticket("CHILD", 5), ticket("INFANT", 1)),
                        MAX_MESSAGE),
                Arguments.of("repeated lines exceed the maximum",
                        booking(ACCOUNT_ID, ticket("ADULT", 13), ticket("ADULT", 13)), MAX_MESSAGE),
                Arguments.of("counts that would overflow an int",
                        booking(ACCOUNT_ID, ticket("ADULT", Integer.MAX_VALUE),
                                ticket("ADULT", Integer.MAX_VALUE)), MAX_MESSAGE),
                Arguments.of("more infants than adults",
                        booking(ACCOUNT_ID, ticket("ADULT", 1), ticket("INFANT", 2)), LAP_MESSAGE),
                Arguments.of("more infants than adults alongside children",
                        booking(ACCOUNT_ID, ticket("ADULT", 1), ticket("CHILD", 3), ticket("INFANT", 2)),
                        LAP_MESSAGE));
    }

    @Test
    void rejectsUnknownTicketType() throws Exception {
        mockMvc.perform(post(BOOKINGS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(booking(ACCOUNT_ID, ticket("SENIOR", 1))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post(BOOKINGS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":1234,\"ticketRequests\":["))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void rejectsMissingRequestBody() throws Exception {
        mockMvc.perform(post(BOOKINGS_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService, seatReservationService);
    }

    @Test
    void rejectsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post(BOOKINGS_URL).contentType(MediaType.TEXT_PLAIN)
                        .content(booking(ACCOUNT_ID, ticket("ADULT", 1))))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(paymentService, seatReservationService);
    }

    /**
     * JSON property names follow the Java records in the domain package ({@code ticketRequests} and
     * {@code ticketCount}), which must not be modified.
     */
    private static String ticket(final String type, final int count) {
        return "{\"type\":\"%s\",\"ticketCount\":%d}".formatted(type, count);
    }

    private static String booking(final Object accountId, final String... tickets) {
        return "{\"accountId\":%s,\"ticketRequests\":[%s]}"
                .formatted(accountId, String.join(",", tickets));
    }
}
