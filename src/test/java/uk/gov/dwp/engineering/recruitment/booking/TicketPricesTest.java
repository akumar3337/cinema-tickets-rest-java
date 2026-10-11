package uk.gov.dwp.engineering.recruitment.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TicketPricesTest {

  private static final BigDecimal VALID = new BigDecimal("1.00");

  @Test
  void normalisesPricesToTwoDecimalPlaces() {
    final TicketPrices prices = new TicketPrices(
        new BigDecimal("25.9"), new BigDecimal("17.5"), BigDecimal.ZERO);

    assertThat(prices.adult()).isEqualTo(new BigDecimal("25.90"));
    assertThat(prices.child()).isEqualTo(new BigDecimal("17.50"));
    assertThat(prices.infant()).isEqualTo(new BigDecimal("0.00"));
  }

  @Test
  void rejectsMissingPrice() {
    assertThatThrownBy(() -> new TicketPrices(VALID, null, VALID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Ticket price 'child' must be configured");
  }

  @Test
  void rejectsNegativePrice() {
    assertThatThrownBy(() -> new TicketPrices(new BigDecimal("-0.01"), VALID, VALID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Ticket price 'adult' must not be negative");
  }

  @Test
  void rejectsMoreThanTwoDecimalPlaces() {
    assertThatThrownBy(() -> new TicketPrices(VALID, VALID, new BigDecimal("0.005")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Ticket price 'infant' must have at most two decimal places");
  }
}
