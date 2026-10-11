Feature: Purchase cinema tickets
  As a cinema customer
  I want to buy tickets for my group
  So that I am charged the right amount and my seats are reserved

  Background:
    Given customer provided accountId 1234

  Scenario: Buying a single adult ticket
    When requested the following tickets:
      | type  | count |
      | ADULT | 1     |
    Then the booking is confirmed
    And payment is done for £25.99
    And 1 seat is reserved
    And the confirmation shows 1 seat and a total cost of £25.99

  Scenario: Buying adult and child tickets
    When requested the following tickets:
      | type  | count |
      | ADULT | 1     |
      | CHILD | 1     |
    Then the booking is confirmed
    And payment is done for £43.49
    And 2 seats are reserved

  Scenario: Infants are free and do not take a seat
    When requested the following tickets:
      | type   | count |
      | ADULT  | 1     |
      | INFANT | 1     |
    Then the booking is confirmed
    And payment is done for £25.99
    And 1 seat is reserved
    And the confirmation shows 1 seat and a total cost of £25.99

  Scenario: Several lines for the same ticket type are added together
    When requested the following tickets:
      | type  | count |
      | ADULT | 1     |
      | ADULT | 1     |
      | CHILD | 1     |
    Then the booking is confirmed
    And payment is done for £69.48
    And 3 seats are reserved

  Scenario: A line with no tickets is ignored
    When requested the following tickets:
      | type  | count |
      | ADULT | 1     |
      | CHILD | 0     |
    Then the booking is confirmed
    And payment is done for £25.99
    And 1 seat is reserved

  Scenario Outline: Cost and seats for <adults> adult, <children> child and <infants> infant tickets
    When requested <adults> adult, <children> child and <infants> infant tickets
    Then the booking is confirmed
    And payment is done for £<cost>
    And <seats> seats are reserved
    And the confirmation shows <seats> seats and a total cost of £<cost>

    Examples:
      | adults | children | infants | cost   | seats |
      | 1      | 0        | 0       | 25.99  | 1     |
      | 3      | 0        | 0       | 77.97  | 3     |
      | 1      | 3        | 0       | 78.49  | 4     |
      | 2      | 1        | 1       | 69.48  | 3     |
      | 2      | 0        | 2       | 51.98  | 2     |

  Scenario Outline: Bookings up to the maximum of 25 tickets are accepted
    When requested <adults> adult, <children> child and <infants> infant tickets
    Then the booking is confirmed
    And payment is done for £<cost>
    And <seats> seats are reserved

    Examples: Exactly 25 tickets, with infants counted towards the limit
      | adults | children | infants | cost   | seats |
      | 25     | 0        | 0       | 649.75 | 25    |
      | 20     | 5        | 0       | 607.30 | 25    |
      | 13     | 0        | 12      | 337.87 | 13    |
      | 10     | 10       | 5       | 434.90 | 20    |
