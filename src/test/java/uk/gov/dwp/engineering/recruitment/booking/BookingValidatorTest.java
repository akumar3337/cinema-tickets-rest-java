package uk.gov.dwp.engineering.recruitment.booking;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

class BookingValidatorTest {

  private static final Long VALID_ACCOUNT_ID = 1L;

  private final BookingValidator validator = new BookingValidator(25);

  @ParameterizedTest
  @ValueSource(longs = {0, -1, Long.MIN_VALUE})
  void rejectsAccountIdsNotGreaterThanZero(final long accountId) {
    assertThatThrownBy(() -> validator.validate(accountId, new TicketCounts(1, 0, 0)))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("A valid account id (greater than zero) is required");
  }

  @Test
  void rejectsNullAccountId() {
    assertThatThrownBy(() -> validator.validate(null, new TicketCounts(1, 0, 0)))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("A valid account id (greater than zero) is required");
  }

  @Test
  void acceptsSmallestValidAccountId() {
    assertThatCode(() -> validator.validate(1L, new TicketCounts(1, 0, 0)))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsBookingWithNoTickets() {
    assertThatThrownBy(() -> validator.validate(VALID_ACCOUNT_ID, new TicketCounts(0, 0, 0)))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("At least one ticket must be requested");
  }

  @ParameterizedTest
  @CsvSource({"25,0,0", "13,12,0", "13,0,12", "10,5,10"})
  void acceptsExactlyTheMaximumNumberOfTickets(
      final long adults, final long children, final long infants) {
    assertThatCode(() -> validator.validate(VALID_ACCOUNT_ID,
        new TicketCounts(adults, children, infants))).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource({"26,0,0", "20,5,1", "13,0,13", "1,25,0"})
  void rejectsMoreThanTheMaximumNumberOfTicketsIncludingInfants(
      final long adults, final long children, final long infants) {
    assertThatThrownBy(() -> validator.validate(VALID_ACCOUNT_ID,
        new TicketCounts(adults, children, infants)))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("A maximum of 25 tickets can be purchased at a time");
  }

  @ParameterizedTest
  @CsvSource({"0,1,0", "0,0,1", "0,2,2"})
  void rejectsChildAndInfantTicketsWithoutAnAdult(
      final long adults, final long children, final long infants) {
    assertThatThrownBy(() -> validator.validate(VALID_ACCOUNT_ID,
        new TicketCounts(adults, children, infants)))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("Child and infant tickets cannot be purchased without an adult ticket");
  }

  @ParameterizedTest
  @CsvSource({"1,0,2", "2,5,3"})
  void rejectsMoreInfantsThanAdults(final long adults, final long children, final long infants) {
    assertThatThrownBy(() -> validator.validate(VALID_ACCOUNT_ID,
        new TicketCounts(adults, children, infants)))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("Each infant must sit on an adult's lap: infants cannot outnumber adults");
  }

  @ParameterizedTest
  @CsvSource({"1,0,0", "1,0,1", "2,0,2", "2,3,1"})
  void acceptsValidCombinations(final long adults, final long children, final long infants) {
    assertThatCode(() -> validator.validate(VALID_ACCOUNT_ID,
        new TicketCounts(adults, children, infants))).doesNotThrowAnyException();
  }

  @Test
  void usesTheConfiguredLimit() {
    final BookingValidator smallLimit = new BookingValidator(3);

    assertThatCode(() -> smallLimit.validate(VALID_ACCOUNT_ID, new TicketCounts(3, 0, 0)))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> smallLimit.validate(VALID_ACCOUNT_ID, new TicketCounts(4, 0, 0)))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("A maximum of 3 tickets can be purchased at a time");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void rejectsLimitBelowOne(final int limit) {
    assertThatThrownBy(() -> new BookingValidator(limit))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cinema.tickets.limits.max-per-booking must be at least 1");
  }
}
