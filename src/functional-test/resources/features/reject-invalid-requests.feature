Feature: Reject invalid booking requests

  Scenario Outline: Account ids must be greater than zero
    Given customer provided accountId <accountId>
    When requested the following tickets:
      | type  | count |
      | ADULT | 1     |
    Then the booking is rejected with the message "A valid account id (greater than zero) is required"
    And customer is not charged
    And no seats are reserved

    Examples:
      | accountId |
      | 0         |
      | -1        |

  Scenario: An account id is required
    Given customer has not provided account id
    When requested the following tickets:
      | type  | count |
      | ADULT | 1     |
    Then the booking is rejected with the message "A valid account id (greater than zero) is required"
    And customer is not charged
    And no seats are reserved

  Scenario: At least one ticket request line is required
    Given customer provided accountId 1234
    When submit a booking with no ticket lines
    Then the booking is rejected with the message "At least one ticket request is required"
    And customer is not charged
    And no seats are reserved

  Scenario: An unknown ticket type is rejected
    Given customer provided accountId 1234
    When requested the following tickets:
      | type   | count |
      | SENIOR | 1     |
    Then the booking is rejected
    And customer is not charged
    And no seats are reserved

  Scenario: A request that is not valid JSON is rejected
    Given customer provided accountId 1234
    When submit a booking request that is not valid JSON
    Then the booking is rejected
    And customer is not charged
    And no seats are reserved
