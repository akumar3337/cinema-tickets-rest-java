package uk.gov.dwp.engineering.recruitment.booking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TicketCountsTest {

  @Test
  void totalIncludesInfants() {
    assertThat(new TicketCounts(2, 3, 1).total()).isEqualTo(6);
  }

  @Test
  void seatsExcludeInfants() {
    assertThat(new TicketCounts(2, 3, 1).seats()).isEqualTo(5);
  }
}
