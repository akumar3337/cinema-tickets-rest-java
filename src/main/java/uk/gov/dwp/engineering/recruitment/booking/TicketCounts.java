package uk.gov.dwp.engineering.recruitment.booking;

public record TicketCounts(long adults, long children, long infants) {

  public long total() {
    return adults + children + infants;
  }

  public long seats() {
    return adults + children;
  }
}
