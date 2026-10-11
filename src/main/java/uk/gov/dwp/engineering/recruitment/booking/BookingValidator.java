package uk.gov.dwp.engineering.recruitment.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

/**
 * Enforces the booking business rules.
 */
@Component
public class BookingValidator {

    private final int maxTicketsPerBooking;

    public BookingValidator(
            @Value("${cinema.tickets.limits.max-per-booking}") final int maxTicketsPerBooking) {
        if (maxTicketsPerBooking < 1) {
            throw new IllegalArgumentException(
                    "cinema.tickets.limits.max-per-booking must be at least 1");
        }
        this.maxTicketsPerBooking = maxTicketsPerBooking;
    }

    public void validate(final Long accountId, final TicketCounts counts)
            throws InvalidBookingException {
        if (accountId == null || accountId <= 0) {
            throw new InvalidBookingException("A valid account id (greater than zero) is required");
        }
        if (counts.total() == 0) {
            throw new InvalidBookingException("At least one ticket must be requested");
        }
        if (counts.total() > maxTicketsPerBooking) {
            throw new InvalidBookingException(
                    "A maximum of " + maxTicketsPerBooking + " tickets can be purchased at a time");
        }
        if (counts.adults() == 0) {
            throw new InvalidBookingException(
                    "Child and infant tickets cannot be purchased without an adult ticket");
        }
        if (counts.infants() > counts.adults()) {
            throw new InvalidBookingException(
                    "Each infant must sit on an adult's lap: infants cannot outnumber adults");
        }
    }
}
