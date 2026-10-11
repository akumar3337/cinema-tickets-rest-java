package uk.gov.dwp.engineering.recruitment.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketPriceCalculatorTest {

  private static final BigDecimal VALID = new BigDecimal("1.00");

  private final TicketPriceCalculator calculator = new TicketPriceCalculator(
      new BigDecimal("25.99"), new BigDecimal("17.50"), new BigDecimal("0.00"));

  @Test
  void adultTicketCosts2599() {
    assertThat(calculator.totalCost(new TicketCounts(1, 0, 0)))
        .isEqualTo(new BigDecimal("25.99"));
  }

  @Test
  void childTicketCosts1750() {
    assertThat(calculator.totalCost(new TicketCounts(0, 1, 0)))
        .isEqualTo(new BigDecimal("17.50"));
  }

  @Test
  void infantTicketIsFree() {
    assertThat(calculator.totalCost(new TicketCounts(1, 0, 1)))
        .isEqualTo(new BigDecimal("25.99"));
  }

  @Test
  void noTicketsCostsNothing() {
    assertThat(calculator.totalCost(new TicketCounts(0, 0, 0)))
        .isEqualTo(new BigDecimal("0.00"));
  }

  @Test
  void usesConfiguredPricesRatherThanFixedOnes() {
    final TicketPriceCalculator other = new TicketPriceCalculator(
        new BigDecimal("10.00"), new BigDecimal("5.00"), new BigDecimal("1.00"));

    assertThat(other.totalCost(new TicketCounts(2, 1, 1))).isEqualTo(new BigDecimal("26.00"));
  }

  @Test
  void normalisesConfiguredPricesToTwoDecimalPlaces() {
    final TicketPriceCalculator other = new TicketPriceCalculator(
        new BigDecimal("25.9"), new BigDecimal("17.5"), BigDecimal.ZERO);

    assertThat(other.totalCost(new TicketCounts(1, 1, 0))).isEqualTo(new BigDecimal("43.40"));
  }

  @ParameterizedTest
  @CsvSource({
      "2,1,1,69.48",
      "3,0,0,77.97",
      "0,3,0,52.50",
      "20,5,0,607.30",
      "13,0,12,337.87"
  })
  void sumsMixedTicketsWithoutRoundingErrors(final long adults, final long children,
      final long infants, final String expectedCost) {
    assertThat(calculator.totalCost(new TicketCounts(adults, children, infants)))
        .isEqualTo(new BigDecimal(expectedCost));
  }

  @Test
  void rejectsMissingPrice() {
    assertThatThrownBy(() -> new TicketPriceCalculator(VALID, null, VALID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cinema.tickets.prices.child must be configured");
  }

  @Test
  void rejectsNegativePrice() {
    assertThatThrownBy(() -> new TicketPriceCalculator(new BigDecimal("-0.01"), VALID, VALID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cinema.tickets.prices.adult must not be negative");
  }

  @Test
  void rejectsMoreThanTwoDecimalPlaces() {
    assertThatThrownBy(() -> new TicketPriceCalculator(VALID, VALID, new BigDecimal("0.005")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cinema.tickets.prices.infant must have at most two decimal places");
  }
}
