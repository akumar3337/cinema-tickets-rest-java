package uk.gov.dwp.engineering.recruitment.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

class TicketRequestAggregatorTest {

  private final TicketRequestAggregator aggregator = new TicketRequestAggregator();

  @Test
  void aggregatesTicketsByType() {
    final TicketCounts counts = aggregator.aggregate(
        new TicketRequest(ADULT, 2), new TicketRequest(CHILD, 3), new TicketRequest(INFANT, 1));

    assertThat(counts).isEqualTo(new TicketCounts(2, 3, 1));
    assertThat(counts.total()).isEqualTo(6);
  }

  @Test
  void infantsAreNotAllocatedSeats() {
    final TicketCounts counts = aggregator.aggregate(
        new TicketRequest(ADULT, 2), new TicketRequest(CHILD, 3), new TicketRequest(INFANT, 1));

    assertThat(counts.seats()).isEqualTo(5);
  }

  @Test
  void sumsRepeatedRequestLinesOfTheSameType() {
    final TicketCounts counts = aggregator.aggregate(
        new TicketRequest(ADULT, 1), new TicketRequest(ADULT, 2), new TicketRequest(CHILD, 1),
        new TicketRequest(CHILD, 1));

    assertThat(counts).isEqualTo(new TicketCounts(3, 2, 0));
  }

  @Test
  void zeroCountLinesContributeNothing() {
    final TicketCounts counts = aggregator.aggregate(
        new TicketRequest(ADULT, 1), new TicketRequest(CHILD, 0), new TicketRequest(INFANT, 0));

    assertThat(counts).isEqualTo(new TicketCounts(1, 0, 0));
  }

  @Test
  void doesNotOverflowWhenSummingLargeCounts() {
    final TicketCounts counts = aggregator.aggregate(
        new TicketRequest(ADULT, Integer.MAX_VALUE), new TicketRequest(ADULT, Integer.MAX_VALUE));

    assertThat(counts.adults()).isEqualTo(2L * Integer.MAX_VALUE);
  }

  @Test
  void rejectsNullRequestArray() {
    assertThatThrownBy(() -> aggregator.aggregate((TicketRequest[]) null))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("At least one ticket request is required");
  }

  @Test
  void rejectsEmptyRequestArray() {
    assertThatThrownBy(() -> aggregator.aggregate())
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("At least one ticket request is required");
  }

  @Test
  void rejectsNullRequestLine() {
    assertThatThrownBy(() -> aggregator.aggregate((TicketRequest) null))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("Ticket requests must not be null");
  }

  @Test
  void rejectsRequestLineWithoutType() {
    assertThatThrownBy(() -> aggregator.aggregate(new TicketRequest(null, 1)))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("Ticket type is required");
  }

  @Test
  void rejectsNegativeCount() {
    assertThatThrownBy(() -> aggregator.aggregate(new TicketRequest(ADULT, -1)))
        .isInstanceOf(InvalidBookingException.class)
        .hasMessage("Ticket count must not be negative");
  }
}
