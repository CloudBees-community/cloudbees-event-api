package com.cloudbees.api.event;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Event API lets you publish and query for events. The events are related to a CloudBees services Subscription or
 * Resource or it could be for a Cloud Resource.
 *
 * @author Vivek Pandey
 */
public class EventApi {

    private static final Logger logger = LoggerFactory.getLogger(EventApi.class);
    static final String SP_PORD_BASE_URL = "https://services-platform.cloudbees.com/";
    private static final String EVENT_API_PATH = "/api/events/";

    private final String accessToken;
    public final String eventApiUrl;
    private final String eventEndpoint;
    private final RestClient restClient = new RestClient();

    /**
     * Create an EventApi instance using a valid Oauth access_token with following scopes:
     *
     * https://api.cloudbees.com/services/api/events/read - For reading/querying events
     * https://api.cloudbees.com/services/api/events/write - For publishing events
     *
     * @param accessToken valid access token with
     *
     * @throws IOException
     */
    public EventApi(@Nonnull String accessToken) throws IOException, EventApiException {
        this(accessToken, UriBuilder.fromUri(SP_PORD_BASE_URL).path(EVENT_API_PATH).build().toString());
    }


    /**
     * Constructs EventApi with provided event endpoint values.
     *
     * Possible values are
     *
     *  https://services-dev.apps.cloudbees.com - For development
     *  https://services-platform.cloudbees.com - For production
     *
     * @throws IOException
     */
    public EventApi(@Nonnull String accessToken, @Nonnull String eventEndpoint) throws EventApiException {
        this.accessToken = accessToken;
        this.eventEndpoint = eventEndpoint;
        this.eventApiUrl =UriBuilder.fromUri(this.eventEndpoint).path(EVENT_API_PATH).build().toString();
    }




    /**
     * Posts a given {@link com.cloudbees.api.event.Event} using given token
     *
     * @param eventRequest event
     *
     * @return Returns Location header of the newly created event. A GET on the Location header will give EventApi details.
     *
     * @throws IOException
     */
    public  String publish(Event eventRequest) throws EventApiException {
        if(eventRequest.getEvent() == null){
            throw new EventApiException("No event data to be sent. Please set event data before publishing");
        }else if(eventRequest.getEvent().get("title") == null){
            throw new EventApiException("'title' must be set in the event data");
        }
        WebResource wr = restClient.client.resource(UriBuilder.fromPath(eventApiUrl).build());
        wr.addFilter(new BearerTokenFilter(accessToken));
        ClientResponse cr = wr.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, eventRequest);
        if(cr.getStatus() >= 300){
            String error = String.format("EvenApi.publish() returned HTTP status: %s, message: %s",cr.getStatus(), cr.toString());
            logger.error(error);
            throw new EventApiException(error);
        }
        return cr.getLocation().toString();
    }

    /**
     * Reads an event given a URL identifying an event id
     *
     */
    public Event readEvent(@Nonnull String eventUrl) throws EventApiException {

        logger.info("GET " + eventUrl);

        WebResource wr = restClient.client.resource(UriBuilder.fromPath(eventUrl).build());
        wr.addFilter(new BearerTokenFilter(accessToken));
        try{
            wr.header("Authorization", createBearerAuthorizationHeader(accessToken));
            ClientResponse cr = wr.get(ClientResponse.class);
            if(cr.getStatus() >= 300){
                String error = String.format("EvenApi.readEvent() returned HTTP status: %s, message: %s",cr.getStatus(), cr.toString());
                logger.error(error);
                throw new EventApiException(error);
            }
            return restClient.objectMapper.readValue(cr.getEntityInputStream(), Event.class);
        }catch(IOException e){
            throw new EventApiException(e.getMessage(), e);
        }

    }

    /**
     *
     * @param evenQueryUrl - Event API with query parameter for events
     * @return
     * @throws IOException
     */
    public  List<Event> query(String evenQueryUrl) throws EventApiException {
        logger.info("Query: "+evenQueryUrl);
        WebResource wr = restClient.client.resource(evenQueryUrl);
        wr.addFilter(new BearerTokenFilter(accessToken));
        try{
            wr.header("Authorization", createBearerAuthorizationHeader(accessToken));
            ClientResponse cr = wr.get(ClientResponse.class);
            if(cr.getStatus() >= 300){
                String error = String.format("EvenApi.query() returned HTTP status: %s, message: %s",cr.getStatus(), cr.toString());
                logger.error(error);
                throw new EventApiException(error);
            }

            return restClient.objectMapper.readValue(cr.getEntityInputStream(), new TypeReference<List<Event>>() {});
        }catch(IOException e){
            throw new EventApiException(e.getMessage(), e);
        }
    }


    public boolean delete(@Nonnull String eventUrl) throws EventApiException {
        logger.info("Delete " + eventUrl);

        WebResource wr = restClient.client.resource(UriBuilder.fromPath(eventUrl).build());
        wr.addFilter(new BearerTokenFilter(accessToken));

        try {
            wr.header("Authorization", createBearerAuthorizationHeader(accessToken));
        } catch (UnsupportedEncodingException e) {
            throw new EventApiException(e.getMessage(), e);
        }
        ClientResponse cr = wr.delete(ClientResponse.class);
        if(cr.getStatus() >= 300){
            String error = String.format("EvenApi.delete() returned HTTP status: %s, message: %s",cr.getStatus(), cr.toString());
            logger.error(error);
            throw new EventApiException(error);
        }

        return cr.getStatus() == 200;
    }

    private  String createBearerAuthorizationHeader(String token) throws UnsupportedEncodingException {
        return String.format("Bearer %s", Base64.encode(token.getBytes("UTF-8")));
    }


    public static class RestClient{
        private Client client;
        private final ObjectMapper objectMapper = createObjectMapper();

        public RestClient() {
            ClientConfig cc = new DefaultClientConfig();
            JacksonConfigurator jc = new JacksonConfigurator(objectMapper);
            cc.getSingletons().add(jc);
            cc.getClasses().add(JacksonJsonProvider.class);
            this.client =  Client.create(cc);
            this.client.addFilter(new LoggingFilter());
        }

        public Client getJerseyClient() {
            return client;
        }

        public ObjectMapper getObjectMapper() {
            return objectMapper;
        }
    }

    private static ObjectMapper createObjectMapper(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private class  BearerTokenFilter extends ClientFilter{

        private final String accessToken;

        private BearerTokenFilter(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
            if (!cr.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                try {
                    cr.getHeaders().add(HttpHeaders.AUTHORIZATION, createBearerAuthorizationHeader(accessToken));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
            return getNext().handle(cr);

        }
    }
}
