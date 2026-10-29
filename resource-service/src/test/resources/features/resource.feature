Feature: Resource service manages MP3 resources

  Background:
    Given resource storage is empty

  Scenario: Uploading a valid MP3 stores it and publishes an event
    When I upload "sample.mp3" as "audio/mpeg"
    Then the response status is 200
    And the response contains a resource id
    And the stored resource can be downloaded and is identical to "sample.mp3"
    And a resource stored event with that id is published

  Scenario: Uploading a non-MP3 file is rejected
    When I upload the text "not an mp3" as "audio/mpeg"
    Then the response status is 400
    And nothing is stored and no event is published

  Scenario: Deleting resources also deletes song metadata
    Given song-service accepts metadata deletion
    And "sample.mp3" has been uploaded 2 times
    When I delete the uploaded resources together with id 999999
    Then the response status is 200
    And the response lists only the uploaded ids
    And song-service was asked to delete metadata for the uploaded ids

  Scenario Outline: Requesting a resource with id "<id>"
    When I request resource "<id>"
    Then the response status is <status>

    Examples:
      | id     | status |
      | abc    | 400    |
      | 0      | 400    |
      | 999999 | 404    |
