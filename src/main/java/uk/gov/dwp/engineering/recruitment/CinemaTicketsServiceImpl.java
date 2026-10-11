package uk.gov.dwp.engineering.recruitment;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.booking.BookingValidator;
import uk.gov.dwp.engineering.recruitment.booking.TicketCounts;
import uk.gov.dwp.engineering.recruitment.booking.TicketPriceCalculator;
import uk.gov.dwp.engineering.recruitment.booking.TicketRequestAggregator;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private final PaymentService paymentService;

  private final SeatReservationService seatReservationService;

  private final TicketRequestAggregator ticketRequestAggregator;

  private final BookingValidator bookingValidator;

  private final TicketPriceCalculator ticketPriceCalculator;

  public CinemaTicketsServiceImpl(final PaymentService paymentService,
                                  final SeatReservationService seatReservationService,
                                  final TicketRequestAggregator ticketRequestAggregator,
                                  final BookingValidator bookingValidator,
                                  final TicketPriceCalculator ticketPriceCalculator) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
    this.ticketRequestAggregator = ticketRequestAggregator;
    this.bookingValidator = bookingValidator;
    this.ticketPriceCalculator = ticketPriceCalculator;
  }

  @Override
  public BookingConfirmation purchaseTickets(final Long accountId,
                                             final TicketRequest... ticketRequests)
          throws InvalidBookingException {

    final TicketCounts counts = ticketRequestAggregator.aggregate(ticketRequests);
    bookingValidator.validate(accountId, counts);

    final BigDecimal totalCost = ticketPriceCalculator.totalCost(counts);
    final long seatCount = counts.seats();

    paymentService.debitAccount(accountId, totalCost);
    seatReservationService.reserveSeats(accountId, seatCount);

    return new BookingConfirmation(accountId, seatCount, totalCost);
  }
}
