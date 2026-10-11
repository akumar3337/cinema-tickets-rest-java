package uk.gov.dwp.engineering.recruitment.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BookingLimitsTest {

  @Test
  void acceptsPositiveLimit() {
    assertThat(new BookingLimits(1).maxPerBooking()).isEqualTo(1);
  }

  @Test
  void rejectsMissingLimit() {
    assertThatThrownBy(() -> new BookingLimits(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Booking limit 'max-per-booking' must be configured");
  }

  @Test
  void rejectsLimitBelowOne() {
    assertThatThrownBy(() -> new BookingLimits(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Booking limit 'max-per-booking' must be at least 1");
  }
}
