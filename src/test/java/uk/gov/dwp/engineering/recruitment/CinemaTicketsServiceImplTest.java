package uk.gov.dwp.engineering.recruitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import uk.gov.dwp.engineering.recruitment.booking.BookingValidator;
import uk.gov.dwp.engineering.recruitment.booking.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.booking.TicketRequestAggregator;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@SpringJUnitConfig
@TestPropertySource(properties = {
        "cinema.tickets.prices.adult=25.99",
        "cinema.tickets.prices.child=17.50",
        "cinema.tickets.prices.infant=0.00",
        "cinema.tickets.limits.max-per-booking=25"
})
class CinemaTicketsServiceImplTest {

    private static final Long ACCOUNT_ID = 1234L;

    @Configuration
    @Import({CinemaTicketsServiceImpl.class, TicketRequestAggregator.class, BookingValidator.class,
            TicketPriceCalculator.class})
    static class TestConfig {
    }

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private SeatReservationService seatReservationService;

    @Autowired
    private CinemaTicketsService service;

    @Test
    void paysForAdultsAndChildrenAndReservesTheirSeats() {
        service.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 2), new TicketRequest(CHILD, 1));

        verify(paymentService).debitAccount(ACCOUNT_ID, new BigDecimal("69.48"));
        verify(seatReservationService).reserveSeats(ACCOUNT_ID, 3L);
    }

    @Test
    void infantsAreFreeAndAreNotAllocatedSeats() {
        service.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1), new TicketRequest(INFANT, 1));

        verify(paymentService).debitAccount(ACCOUNT_ID, new BigDecimal("25.99"));
        verify(seatReservationService).reserveSeats(ACCOUNT_ID, 1L);
    }

    @Test
    void aggregatesRepeatedRequestLinesBeforeCalculatingCostAndSeats() {
        service.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1), new TicketRequest(ADULT, 1),
                new TicketRequest(CHILD, 1));

        verify(paymentService).debitAccount(ACCOUNT_ID, new BigDecimal("69.48"));
        verify(seatReservationService).reserveSeats(ACCOUNT_ID, 3L);
    }

    @Test
    void ignoresZeroCountLines() {
        service.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1), new TicketRequest(CHILD, 0));

        verify(paymentService).debitAccount(ACCOUNT_ID, new BigDecimal("25.99"));
        verify(seatReservationService).reserveSeats(ACCOUNT_ID, 1L);
    }

    @Test
    void returnsConfirmationWithAccountSeatCountAndTotalCost() {
        final BookingConfirmation confirmation = service.purchaseTickets(ACCOUNT_ID,
                new TicketRequest(ADULT, 2), new TicketRequest(CHILD, 1), new TicketRequest(INFANT, 1));

        assertThat(confirmation)
                .isEqualTo(new BookingConfirmation(ACCOUNT_ID, 3L, new BigDecimal("69.48")));
    }

    @Test
    void takesPaymentBeforeReservingSeats() {
        service.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1));

        final InOrder inOrder = inOrder(paymentService, seatReservationService);
        inOrder.verify(paymentService).debitAccount(ACCOUNT_ID, new BigDecimal("25.99"));
        inOrder.verify(seatReservationService).reserveSeats(ACCOUNT_ID, 1L);
    }

    @Test
    void doesNotReserveSeatsWhenPaymentFails() {
        final IllegalStateException failure = new IllegalStateException("payment failed");
        doThrow(failure).when(paymentService).debitAccount(ACCOUNT_ID, new BigDecimal("25.99"));

        assertThatThrownBy(() -> service.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1)))
                .isSameAs(failure);

        verifyNoInteractions(seatReservationService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBookings")
    void rejectsInvalidBookingsWithoutPayingOrReserving(final String description,
                                                        final Long accountId, final TicketRequest[] ticketRequests, final String expectedMessage) {
        assertThatThrownBy(() -> service.purchaseTickets(accountId, ticketRequests))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessage(expectedMessage);

        verifyNoInteractions(paymentService, seatReservationService);
    }

    static Stream<Arguments> invalidBookings() {
        final String accountMessage = "A valid account id (greater than zero) is required";
        final String maxMessage = "A maximum of 25 tickets can be purchased at a time";
        final String adultMessage =
                "Child and infant tickets cannot be purchased without an adult ticket";
        final String lapMessage =
                "Each infant must sit on an adult's lap: infants cannot outnumber adults";

        return Stream.of(
                Arguments.of("null account id", null, requests(new TicketRequest(ADULT, 1)),
                        accountMessage),
                Arguments.of("zero account id", 0L, requests(new TicketRequest(ADULT, 1)),
                        accountMessage),
                Arguments.of("negative account id", -1L, requests(new TicketRequest(ADULT, 1)),
                        accountMessage),
                Arguments.of("null requests", ACCOUNT_ID, null,
                        "At least one ticket request is required"),
                Arguments.of("no requests", ACCOUNT_ID, requests(),
                        "At least one ticket request is required"),
                Arguments.of("null request line", ACCOUNT_ID, requests((TicketRequest) null),
                        "Ticket requests must not be null"),
                Arguments.of("null ticket type", ACCOUNT_ID, requests(new TicketRequest(null, 1)),
                        "Ticket type is required"),
                Arguments.of("negative count", ACCOUNT_ID, requests(new TicketRequest(ADULT, -1)),
                        "Ticket count must not be negative"),
                Arguments.of("only zero-count tickets", ACCOUNT_ID, requests(new TicketRequest(ADULT, 0)),
                        "At least one ticket must be requested"),
                Arguments.of("child without adult", ACCOUNT_ID, requests(new TicketRequest(CHILD, 1)),
                        adultMessage),
                Arguments.of("infant without adult", ACCOUNT_ID, requests(new TicketRequest(INFANT, 1)),
                        adultMessage),
                Arguments.of("child and infant without adult", ACCOUNT_ID,
                        requests(new TicketRequest(CHILD, 1), new TicketRequest(INFANT, 1)), adultMessage),
                Arguments.of("26 adults", ACCOUNT_ID, requests(new TicketRequest(ADULT, 26)), maxMessage),
                Arguments.of("infants count towards the maximum", ACCOUNT_ID,
                        requests(new TicketRequest(ADULT, 20), new TicketRequest(CHILD, 5),
                                new TicketRequest(INFANT, 1)), maxMessage),
                Arguments.of("repeated lines exceed the maximum", ACCOUNT_ID,
                        requests(new TicketRequest(ADULT, 13), new TicketRequest(ADULT, 13)), maxMessage),
                Arguments.of("counts that would overflow an int", ACCOUNT_ID,
                        requests(new TicketRequest(ADULT, Integer.MAX_VALUE),
                                new TicketRequest(ADULT, Integer.MAX_VALUE)), maxMessage),
                Arguments.of("more infants than adults", ACCOUNT_ID,
                        requests(new TicketRequest(ADULT, 1), new TicketRequest(INFANT, 2)), lapMessage));
    }

    private static TicketRequest[] requests(final TicketRequest... ticketRequests) {
        return ticketRequests;
    }
}
