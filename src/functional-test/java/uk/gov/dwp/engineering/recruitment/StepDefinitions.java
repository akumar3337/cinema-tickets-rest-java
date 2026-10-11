package uk.gov.dwp.engineering.recruitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

public class StepDefinitions {

    private static final String BOOKINGS_PATH = "/cinema/bookings";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SeatReservationService seatReservationService;

    private Long accountId;

    private final List<TicketLine> ticketLines = new ArrayList<>();

    private ResponseEntity<String> response;

    @Before
    public void resetExternalServices() {
        Mockito.reset(paymentService, seatReservationService);
    }

    @Given("customer provided accountId {long}")
    public void customerProvidedAccountId(final long id) {
        accountId = id;
    }

    @Given("customer has not provided account id")
    public void customerHasNotProvidedAccountId() {
        accountId = null;
    }

    @Given("the payment service is unavailable")
    public void thePaymentServiceIsUnavailable() {
        doThrow(new IllegalStateException("payment service unavailable"))
                .when(paymentService).debitAccount(any(), any());
    }

    @Given("the seat reservation service is unavailable")
    public void theSeatReservationServiceIsUnavailable() {
        doThrow(new IllegalStateException("seat reservation service unavailable"))
                .when(seatReservationService).reserveSeats(any(), any());
    }

    @When("requested {int} adult, {int} child and {int} infant tickets")
    public void requestedTicketsByType(final int adults, final int children, final int infants) {
        ticketLines.add(new TicketLine("ADULT", String.valueOf(adults)));
        ticketLines.add(new TicketLine("CHILD", String.valueOf(children)));
        ticketLines.add(new TicketLine("INFANT", String.valueOf(infants)));
        submit(bookingJson());
    }

    @When("requested the following tickets:")
    public void requestedTheFollowingTickets(final DataTable table) {
        table.asMaps().forEach(row -> ticketLines.add(new TicketLine(row.get("type"),
                row.get("count"))));
        submit(bookingJson());
    }

    @When("submit a booking with no ticket lines")
    public void submitABookingWithNoTicketLines() {
        submit(bookingJson());
    }

    @When("submit a booking request that is not valid JSON")
    public void submitABookingRequestThatIsNotValidJson() {
        submit("{\"accountId\":1234,\"ticketRequests\":[");
    }

    @Then("the booking is confirmed")
    public void theBookingIsConfirmed() {
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    @Then("the confirmation shows {int} seat(s) and a total cost of £{bigdecimal}")
    public void theConfirmationShows(final int seats, final BigDecimal cost) {
        final Number seatCount = JsonPath.read(response.getBody(), "$.seatCount");
        final Number totalCost = JsonPath.read(response.getBody(), "$.totalCost");

        assertThat(seatCount.intValue()).isEqualTo(seats);
        assertThat(new BigDecimal(totalCost.toString())).isEqualByComparingTo(cost);
    }

    @Then("payment is done for £{bigdecimal}")
    public void amountPaidFor(final BigDecimal amount) {
        verify(paymentService).debitAccount(accountId, amount);
    }

    @Then("{int} seat(s) is/are reserved")
    public void seatsAreReserved(final int seats) {
        verify(seatReservationService).reserveSeats(accountId, (long) seats);
    }

    @Then("the booking is rejected with the message {string}")
    public void theBookingIsRejectedWithTheMessage(final String message) {
        theBookingIsRejected();
        final String detail = JsonPath.read(response.getBody(), "$.detail");
        assertThat(detail).isEqualTo(message);
    }

    @Then("the booking is rejected")
    public void theBookingIsRejected() {
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Then("the booking fails with a server error")
    public void theBookingFailsWithAServerError() {
        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Then("customer is not charged")
    public void customerNotCharged() {
        verifyNoInteractions(paymentService);
    }

    @Then("no seats are reserved")
    public void noSeatsAreReserved() {
        verifyNoInteractions(seatReservationService);
    }

    private String bookingJson() {
        final String lines = ticketLines.stream()
                .map(line -> "{\"type\":\"%s\",\"ticketCount\":%s}".formatted(line.type(), line.count()))
                .collect(Collectors.joining(","));
        final String account = accountId == null ? "" : "\"accountId\":%s,".formatted(accountId);
        return "{" + account + "\"ticketRequests\":[" + lines + "]}";
    }

    private void submit(final String json) {
        response = RestClient.create("http://localhost:" + port)
                .post()
                .uri(BOOKINGS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> { })
                .toEntity(String.class);
    }

    private record TicketLine(String type, String count) {
    }
}
