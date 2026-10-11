package uk.gov.dwp.engineering.recruitment.booking;

import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

@Component
public class TicketRequestAggregator {

    public TicketCounts aggregate(final TicketRequest... ticketRequests)
            throws InvalidBookingException {
        if (ticketRequests == null || ticketRequests.length == 0) {
            throw new InvalidBookingException("At least one ticket request is required");
        }

        long adults = 0;
        long children = 0;
        long infants = 0;

        for (final TicketRequest request : ticketRequests) {
            if (request == null) {
                throw new InvalidBookingException("Ticket requests must not be null");
            }
            if (request.type() == null) {
                throw new InvalidBookingException("Ticket type is required");
            }
            if (request.ticketCount() < 0) {
                throw new InvalidBookingException("Ticket count must not be negative");
            }

            switch (request.type()) {
                case ADULT -> adults += request.ticketCount();
                case CHILD -> children += request.ticketCount();
                case INFANT -> infants += request.ticketCount();
                default -> throw new IllegalStateException("Unsupported ticket type: " + request.type());
            }
        }

        return new TicketCounts(adults, children, infants);
    }
}
