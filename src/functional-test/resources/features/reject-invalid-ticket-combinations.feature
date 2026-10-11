Feature: Reject invalid ticket combinations
  As the cinema
  I want to refuse bookings that break the ticketing rules
  So that nobody is charged and no seats are reserved for an invalid booking

  Background:
    Given customer provided accountId 1234

  Scenario Outline: Child and infant tickets cannot be bought without an adult
    When requested <adults> adult, <children> child and <infants> infant tickets
    Then the booking is rejected with the message "Child and infant tickets cannot be purchased without an adult ticket"
    And customer is not charged
    And no seats are reserved

    Examples:
      | adults | children | infants |
      | 0      | 1        | 0       |
      | 0      | 0        | 1       |
      | 0      | 2        | 2       |

  Scenario Outline: No more than 25 tickets can be bought at a time
    When requested <adults> adult, <children> child and <infants> infant tickets
    Then the booking is rejected with the message "A maximum of 25 tickets can be purchased at a time"
    And customer is not charged
    And no seats are reserved

    Examples: One ticket over the limit, with infants counted towards it
      | adults | children | infants |
      | 26     | 0        | 0       |
      | 20     | 5        | 1       |
      | 13     | 0        | 13      |

  Scenario: Lines for the same ticket type count towards the limit together
    When requested the following tickets:
      | type  | count |
      | ADULT | 13    |
      | ADULT | 13    |
    Then the booking is rejected with the message "A maximum of 25 tickets can be purchased at a time"
    And customer is not charged
    And no seats are reserved

  Scenario: Huge ticket counts cannot sneak under the limit by overflowing
    When requested the following tickets:
      | type  | count      |
      | ADULT | 2147483647 |
      | ADULT | 2147483647 |
    Then the booking is rejected with the message "A maximum of 25 tickets can be purchased at a time"
    And customer is not charged
    And no seats are reserved

  Scenario Outline: Every infant needs an adult's lap
    When requested <adults> adult, <children> child and <infants> infant tickets
    Then the booking is rejected with the message "Each infant must sit on an adult's lap: infants cannot outnumber adults"
    And customer is not charged
    And no seats are reserved

    Examples:
      | adults | children | infants |
      | 1      | 0        | 2       |
      | 1      | 3        | 2       |

  Scenario: A booking must contain at least one ticket
    When requested 0 adult, 0 child and 0 infant tickets
    Then the booking is rejected with the message "At least one ticket must be requested"
    And customer is not charged
    And no seats are reserved

  Scenario: A negative number of tickets is rejected
    When requested the following tickets:
      | type  | count |
      | ADULT | -1    |
    Then the booking is rejected with the message "Ticket count must not be negative"
    And customer is not charged
    And no seats are reserved
