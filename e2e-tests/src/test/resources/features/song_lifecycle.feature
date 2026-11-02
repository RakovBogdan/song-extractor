Feature: Song lifecycle across the whole system

  Scenario: Uploaded MP3 gets its metadata extracted and can be deleted everywhere
    When I upload "sample.mp3" through the API gateway
    Then I receive a resource id
    And within 30 seconds song metadata for that resource is available
    And the song metadata has a non-empty name and duration
    When I delete the resource through the API gateway
    Then the resource is no longer available
    And the song metadata is no longer available
