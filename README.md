cloudbees-event-api
===================

Java client library to publish, read and query events.

An event can be published and queried for a Cloud Resource. A Service Platform resource or subscription is a cloud
resource as well. The URL pattern is:

For Services Platform resource:
CR URL: https://services-platform.cloudbees.com/api/services/resources/{service}/{account}/{resourceId}

CR type:https://types.cloudbees.com/resource/services-platform/resource

For Services Platform subscription:

CR URL: https://services-platform.cloudbees.com/api/services/subscriptions/{service}/{account}

CR type:https://types.cloudbees.com/resource/services-platform/subscription

Using the code
==============
To use this library, add the following dependency to your Maven POM:

* Stable Release
    <dependency>
     <groupId>com.cloudbees.event</groupId>
     <artifactId>cloudbees-event-api</artifactId>
     <version>1.1</version>
    </dependency>


* Snapshot

    <dependency>
     <groupId>com.cloudbees.event</groupId>
     <artifactId>cloudbees-event-api</artifactId>
     <version>1.2-SNAPSHOT</version>
    </dependency>

Authentication
--------------
Event API require an OAuth token with following scopes:

    * To publish an event https://api.cloudbees.com/services/api/events/write
    * To read/Query an event https://api.cloudbees.com/services/api/events/read
    * CloudResource READ capability scope. For example for a git CR, https://acme.org/testdb, the token must
      be granted with OAuth scope: crs://acme.org/!https://types.cloudbees.com/resource/read

Usage
-----

        OauthClient oauthClient = new BeesClient(clientId,clientSecret).getOauthClient();
        TokenRequest tr = new TokenRequest();
                tr.withAccountName("cloudbees").withGenerateRequestToken(true)
                        .withScope(new URL("http://localhost:9090/vivek/websolr-play-sample"),
                                new Capability("https://types.cloudbees.com/resource/read"))
                        .withScope("https://api.cloudbees.com/services/api/events/write")
                        .withScope("https://api.cloudbees.com/services/api/events/read");

        // Generated token also carries refreshToken, store somewhere safe and next time around
        // call oauthClient.exchangeToAccessToken(refreshToken) to get new accessToken
        OauthToken token = oauthClient.createToken(tr);

        EventApi eventApi = new EventApi(token.accessToken);

Publish Event
-------------
The Target URL must identify the cloud resource. 

        // Publish event events for an application on RUN@cloud as cloud resource
        Event.Target target = new Event.Target(new URL("https://services-platform.cloudbees.com/api/services/resources/cb-app/acme/helloworld"),
                                               "https://types.cloudbees.com/resource/services-platform/resource");

        Event eventReq = new Event(target, "info");

        Event.EventData eventData = new Event.EventData.Builder("Application helloworld deployed")
                        .description("Application helloworld event")
                        .url(new URL("https://run.cloudbees.com/a/acme"))
                        .build();

        eventReq.setEvent(eventData);

        //Returns Event URL, you can use this URL to get event detail
        String eventUrl = eventApi.publish(eventReq);



* Alternate way to Publish Event for a Services Platform Resource

Here we are using a different Target constructor providing service, account name and resourceId, everything else remains the same.

        // Publish event events for an application on RUN@cloud
        Event.Target target = new Event.Target.SpTargetBuilder("cb-app").
                resourceId("helloworld","cloudbees").
                resourceType("application").
                build();

 * HTTP Request
 
        > POST https://services-platform.cloudbees.com/api/events/
        > Content-Type: application/json
        > Authorization: Bearer XXXXXXXXXXXX==
        {
          "target" : {
            "url" : "https://services-platform.cloudbees.com/api/services/resources/cb-app/acme/helloworld"
          },
          "type" : "info",
          "event" : {
            "title" : "Application helloworld deployed",
            "description" : "Application helloworld event",
            "url" : "https://run.cloudbees.com/a/acme"
          }
        }
 * HTTP Response
 
        < 201
        < Date: Sun, 17 Nov 2013 20:33:13 GMT
        < Content-Length: 0
        < Location: https://services-platform.cloudbees.com/api/events/4e0fd0b48f744959b043dc11bd78f412

Read an event
-------------
        // Read an event,
        // using enventUrl with eventId,
        // for example https://services-platform.cloudbees.com/api/events/4e0fd0b48f744959b043dc11bd78f412
        Event event = eventApi.readEvent(eventUrl);


  * HTTP Request
  
        > GET https://services-platform.cloudbees.com/api/events/4e0fd0b48f744959b043dc11bd78f412
        > Authorization: Bearer XXXXXXXXXXXX==

  * HTTP Response
 
        < 200
        < Content-Type: application/json
        <
        {
          "type" : "info",
          "id" : "bf6d8e935fc143468c3e6b5b459ea330",
          "source" : {
            "url" : "https://services-platform.cloudbees.com/api/services/resources/cb-app/cloudbees/helloworld",
            "types" : [ "https://types.cloudbees.com/resource/services-platform/resource/application" ]
          },
          "event" : {
            "title" : "Application helloworld deployed",
            "description" : "Application helloworld event",
            "url" : "https://run.cloudbees.com/a/acme"
          },
          "activation_time" : 1384721849
        }

Delete event
------------
        // Delete an event, eventUrl with event id
        // for example, https://services-platform.cloudbees.com/api/events/4e0fd0b48f744959b043dc11bd78f412
        eventApi.delete(eventUrl);

  * HTTP Request
 
        > DELETE http://services-dev.apps.cloudbees.com/api/events/4e0fd0b48f744959b043dc11bd78f412
        > Authorization: Bearer XXXXXXXXXXXX==

  * HTTP Response
 
        < 200
        < Date: Sun, 17 Nov 2013 20:33:13 GMT
        < Content-Length: 0

Query events
------------
        //Build query using CR url
        QueryEventObject qeo = new QueryEventObject.QueryBuilder(eventApi.eventApiUrl)
                .cloudResource("https://services-platform.cloudbees.com/api/services/resources/cb-app/cloudbees/helloworld")
                .build();

  * HTTP Request
  
        > GET https://services-platform.cloudbees.com/api/events/?cloud_resource=https://services-platform.cloudbees.com/api/services/resources/cb-app/cloudbees/helloworld
        > Authorization: Bearer XXXXXXXXXXXX==

        //Build query using SP specific parameters (account, service etc.)
        QueryEventObject qeo = new QueryEventObject.QueryBuilder(eventApi.eventApiUrl)
                .account("cloudbees")
                .service("cb-app")
                .resource("helloworld")
                .build();

  * HTTP Request
  
        > GET http://services-dev.apps.cloudbees.com/api/events/?account=cloudbees&service=cb-app&resource=helloworld
        > Authorization: Bearer XXXXXXXXXXXX==

        // Execute query
        List<Event> events = eventApi.query(qeo.toUri());


  * HTTP Response
  
        < 200
        < Content-Type: application/json

        [ {
          "type" : "info",
          "id" : "bf6d8e935fc143468c3e6b5b459ea330",
          "source" : {
            "url" : "https://services-dev.apps.cloudbees.com/api/services/resources/cb-app/cloudbees/helloworld",
            "types" : [ "https://types.cloudbees.com/resource/services-platform/resource/application" ]
          },
          "event" : {
            "title" : "Application helloworld deployed",
            "description" : "Application helloworld event",
            "url" : "https://run.cloudbees.com/a/acme"
          },
          "activation_time" : 1384721849
        } ]

