package uk.gov.dwp.engineering.recruitment.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TicketPriceCalculator {

  private final BigDecimal adultPrice;

  private final BigDecimal childPrice;

  private final BigDecimal infantPrice;

  public TicketPriceCalculator(
          @Value("${cinema.tickets.prices.adult}") final BigDecimal adultPrice,
          @Value("${cinema.tickets.prices.child}") final BigDecimal childPrice,
          @Value("${cinema.tickets.prices.infant}") final BigDecimal infantPrice) {
    this.adultPrice = validated("adult", adultPrice);
    this.childPrice = validated("child", childPrice);
    this.infantPrice = validated("infant", infantPrice);
  }

  public BigDecimal totalCost(final TicketCounts counts) {
    return adultPrice.multiply(BigDecimal.valueOf(counts.adults()))
            .add(childPrice.multiply(BigDecimal.valueOf(counts.children())))
            .add(infantPrice.multiply(BigDecimal.valueOf(counts.infants())));
  }

  private static BigDecimal validated(final String type, final BigDecimal price) {
    final String property = "cinema.tickets.prices." + type;
    if (price == null) {
      throw new IllegalArgumentException(property + " must be configured");
    }
    if (price.signum() < 0) {
      throw new IllegalArgumentException(property + " must not be negative");
    }
    try {
      return price.setScale(2, RoundingMode.UNNECESSARY);
    } catch (final ArithmeticException e) {
      throw new IllegalArgumentException(property + " must have at most two decimal places", e);
    }
  }
}
