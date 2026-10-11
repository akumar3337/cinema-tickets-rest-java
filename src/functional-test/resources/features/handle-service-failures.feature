Feature: Handle failures of the payment and seat reservation services
  As the cinema
  I want a failing external service to be reported as a server error
  So that a customer is never told a booking worked when it did not

  Scenario: Seats are not reserved when payment fails
    Given customer provided accountId 1234
    And the payment service is unavailable
    When requested the following tickets:
      | type  | count |
      | ADULT | 1     |
    Then the booking fails with a server error
    And no seats are reserved

  Scenario: A failed seat reservation is reported even though payment was taken
    Given customer provided accountId 1234
    And the seat reservation service is unavailable
    When requested the following tickets:
      | type  | count |
      | ADULT | 1     |
    Then the booking fails with a server error
    And payment is done for £25.99
