Feature: Incident Management
  As a ServiceNow consumer
  I want to manage incident records via the Table API
  So that incidents are created, retrievable, updatable, and deletable end-to-end

  # ── Create ──────────────────────────────────────────────────────────────────

  Scenario: Create an incident with the minimum required fields
    When I create an incident with description "Disk failure on server" and short description "Storage issue"
    Then the incident should be created successfully
    And the response should match the incident schema

  Scenario Outline: Create incidents with varied descriptions
    When I create an incident with description "<description>" and short description "<short_description>"
    Then the incident should be created successfully

    Examples:
      | description   | short_description                       |
      | APISessionJAN | Adding new record using POST POJO       |
      | APISessionFEB | Adding new record using POST POJO       |
      | APISessionMar | Adding new record using POST POJO Mar   |

  # ── Fetch ────────────────────────────────────────────────────────────────────

  Scenario: Fetch a created incident by its incident number
    # Creates the record, then queries the list endpoint using the
    # human-readable number (e.g. INC0001234) returned in the 201 response.
    # Verifies end-to-end persistence: what we POST we can GET back by number.
    When I create an incident with description "Network outage on floor 3" and short description "Internet unreachable"
    Then the incident should be created successfully
    When I fetch the created incident by its number
    Then the fetched incident number should match the created incident

  # ── Update ───────────────────────────────────────────────────────────────────

  Scenario: Update the short description of an existing incident
    # Creates the record, then issues a PUT with a new short_description.
    # The @After hook deletes the record once the scenario completes.
    When I create an incident with description "CPU overload on app server" and short description "Server hot"
    Then the incident should be created successfully
    When I update the incident short description to "Server CPU running at 100% — reboot scheduled"
    Then the incident update should succeed

  # ── Delete ───────────────────────────────────────────────────────────────────

  Scenario: Delete a created incident directly
    # The delete step clears ctx.createdSysId so the @After hook does not
    # attempt a second DELETE on a record that no longer exists (which would
    # return 404 and print a misleading error after an otherwise-passing test).
    When I create an incident with description "Temporary test record" and short description "To be deleted"
    Then the incident should be created successfully
    When I delete the created incident
    Then the incident deletion should return no content
