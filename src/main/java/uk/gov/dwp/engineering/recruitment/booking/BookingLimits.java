package uk.gov.dwp.engineering.recruitment.booking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cinema.tickets.limits")
public record BookingLimits(Integer maxPerBooking) {

    public BookingLimits {
        if (maxPerBooking == null) {
            throw new IllegalArgumentException("Booking limit 'max-per-booking' must be configured");
        }
        if (maxPerBooking < 1) {
            throw new IllegalArgumentException("Booking limit 'max-per-booking' must be at least 1");
        }
    }
}
