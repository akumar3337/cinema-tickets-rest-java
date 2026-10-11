package uk.gov.dwp.engineering.recruitment.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cinema.tickets.prices")
public record TicketPrices(BigDecimal adult, BigDecimal child, BigDecimal infant) {

    public TicketPrices {
        adult = normalise("adult", adult);
        child = normalise("child", child);
        infant = normalise("infant", infant);
    }

    private static BigDecimal normalise(final String name, final BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Ticket price '" + name + "' must be configured");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Ticket price '" + name + "' must not be negative");
        }
        try {
            return price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (final ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Ticket price '" + name + "' must have at most two decimal places", e);
        }
    }
}
