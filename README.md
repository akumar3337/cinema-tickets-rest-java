# Cinema Tickets

A REST service for purchasing cinema tickets. `POST /cinema/bookings` validates
a ticket purchase, takes payment through the `PaymentService` and reserves
seats through the `SeatReservationService`.

## Getting started
 > Required softwares
 > java 21 and Maven

## Running the service

```shell
   mvn spring-boot:run
```

## Running the tests

```shell
  mvn verify
```

## Approach

`CinemaTicketsServiceImpl` only orchestrates. Each responsibility lives in a
small class of its own:

- `TicketRequestAggregator` aggregates the ticket requests into `TicketCounts`
  (adult, child and infant) and rejects malformed ticket requests (null ticket request, null type,
  negative count).
- `BookingValidator` enforces the business rules on those counts.
- `TicketPriceCalculator` works out the total cost.
- `CinemaTicketsServiceImpl` validates, pays, reserves seats, then returns a
  `BookingConfirmation`.
- `RestExceptionHandler` turns an `InvalidBookingException` into problem detail with HTTP status 400.

Every class is a Spring bean and receives its collaborators and configuration
through constructor injection. Nothing is created with `new` or reached
through static calls in production code.

The whole booking is validated before either external service is called, so an
invalid request never causes a payment or a seat reservation.

Ticket prices and the booking limit are configuration, not code. They are read
with `@Value` using the exact property names, in the constructors of
`TicketPriceCalculator` and `BookingValidator`:

- `cinema.tickets.prices.adult` (25.99)
- `cinema.tickets.prices.child` (17.50)
- `cinema.tickets.prices.infant` (0.00)
- `cinema.tickets.limits.max-per-booking` (25)

They can be overridden per environment, for example with the
`CINEMA_TICKETS_PRICES_ADULT` environment variable. There are no defaults in
code: a missing property, a negative price, a price with more than two decimal
places or a limit below 1 stops the service starting. Error messages use the
configured limit.

## Business rules

- INFANT tickets cost £0.00, CHILD tickets £17.50 and ADULT tickets £25.99
  (the configured values).
- A booking may contain at most 25 tickets (the configured limit).
- INFANT tickets are free and do not get a seat; they sit on an adult's lap.
- CHILD and INFANT tickets cannot be bought without an ADULT ticket.
- Seats reserved = ADULT tickets + CHILD tickets.
- Total cost = (ADULT x 25.99) + (CHILD x 17.50).

## Rejected requests

Every rule below results in an `InvalidBookingException`, which the API returns
as HTTP 400 with the reason in the `detail` field.

- The account id is missing, zero or negative.
- There are no ticket request entries (missing, null or empty).
- A request is null, has no ticket type, or has a negative count.
- The booking contains no tickets at all (for example only zero-count ticket requests).
- More than 25 tickets in total, counted after summing all ticket requests.
- CHILD or INFANT tickets without at least one ADULT ticket.
- More INFANT tickets than ADULT tickets.

Malformed JSON, an unknown ticket type, a missing body and an unsupported media
type are rejected by Spring MVC before the service is reached (HTTP 400, or 415
for the media type).

## Assumptions and ambiguities

The brief leaves some points open. These are the decisions taken and why.

1. **Infants count towards the 25 ticket limit.** The rules say an infant "does
   not pay for a ticket", which implies infants hold a (free) ticket. The limit
   is on tickets, not seats, so a booking of 13 adults and 13 infants is
   rejected. Counting only seats instead would be a one-entry record change in
   `BookingValidator`.
2. **Infants cannot outnumber adults.** An infant sits on an adult's lap, so one
   adult cannot carry two infants. The brief does not state this explicitly; it
   is inferred from the lap rule and enforced.
3. **Repeated ticket requests of one type are summed.** Two ADULT ticket request of 1 are treated
   as 2 ADULT tickets, and every rule applies to the aggregate. A client
   splitting a booking across ticket requests cannot bypass the 25 ticket limit.
4. **Zero-count ticket requests are accepted and ignored.** The OpenAPI contract allows a
   minimum count of 0. A booking must still contain at least one ADULT ticket,
   so a booking made only of zero-count ticket requests is rejected.
5. **Negative counts and null values are rejected**, not silently corrected.
6. **Account ids** must be non-null and greater than zero, as given in the
   brief. No other account checks are made.
7. **Money uses `BigDecimal`** with two decimal places. Binary floating point
   cannot represent these prices exactly.
8. **Payment is taken before seats are reserved**, and only after full
   validation. If the payment call throws, no seats are reserved and the
   exception propagates.
9. **Responses from the payment and reservation services are not inspected.**
   The brief states that both always succeed. If the reservation call fails
   after payment has been taken there is no refund, because `PaymentService`
   offers no way to reverse a debit; this is out of scope.
10. **Seat counts are sent as `Long`** to match `SeatReservationService`.
11. **HTTP status for invalid bookings is 400.** The initial exception handler
    returned 501 (Not Implemented), which contradicts the 400 response
    documented in `cinema-tickets.yaml`, so it was changed.
12. **`BookingConfirmation` was extended** with `seatCount` and `totalCost` to
    match the OpenAPI schema. The original one-argument constructor and the
    `accountId()` accessor are unchanged, so the change is non-breaking.
13. **No idempotency, duplicate detection or authentication** is provided. A
    repeated request makes a second payment and reservation.

## Known discrepancy: JSON field names

The `domain` package must not be modified, and its records define the JSON
property names the API accepts: `ticketRequests` (a list of objects with `type`
and `ticketCount`). The OpenAPI file names these `tickets` and `count`. The
implementation follows the Java records. Aligning the two would need either a
change to the domain records or the OpenAPI file, or a Jackson mixin.

## Testing

The service and controller tests build a Spring context, bind prices and limits
from test properties, and replace only the two external services with
`@MockitoBean`.

- `TicketRequestAggregatorTest`, `TicketCountsTest`, `BookingValidatorTest`
  and `TicketPriceCalculatorTest` cover aggregation, each business rule with
  its boundaries, the pricing, and validation of the configured prices and
  limit.
- `CinemaTicketsServiceImplTest` checks the amount paid, the seats reserved,
  payment-before-reservation ordering, and that every rejected request makes no
  calls to either external service.
- `CinemaTicketsControllerTest` drives `POST /cinema/bookings` with MockMvc
  through the real service, aggregator, validator, calculator and exception
  handler, with only the two external services mocked. It covers every business rule,
  every rejected request, and the framework-level errors (400 and 415).

### Running unit tests
The unit tests can be run using `mvn clean verify`, or alternatively through intellij.

### Running functional tests
Run the functional tests to validate the behaviour

```shell
  mvn verify "-Dtest=NoSuchTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dit.test=CucumberRunnerFT"
```

## Technology stack
java / Spring boot / Maven