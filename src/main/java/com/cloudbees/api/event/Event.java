package com.cloudbees.api.event;


import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vivek Pandey
 */
public class Event {
    private static final Logger logger = LoggerFactory.getLogger(Event.class);

    @JsonProperty("id")
    private String id;

    private Target target;

    @JsonProperty("source")
    private Target source;

    @JsonProperty("event")
    private Map eventData;

    @JsonProperty("type")
    private String type;

    @JsonProperty("expiry_time")
    private Long expiryTime;

    @JsonProperty("activation_time")
    private Long activationTime;


    @JsonCreator
    public Event(@Nonnull @JsonProperty("target") Target target, @Nonnull @JsonProperty("type") String type){
        this.target = target;
        this.type=type;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Target getSource() {
        return source;
    }

    public void setSource(Target source) {
        this.source = source;
    }

    public Target getTarget(){
        return target;
    }

    public Map getEvent() {
        return eventData;
    }

    public void setEvent(Map eventData) {
        this.eventData = eventData;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public Long getActivationTime() {
        return activationTime;
    }

    public void setActivationTime(Long activationTime) {
        this.activationTime = activationTime;
    }

    /**
     * Any cloud resource aware client posting events related to a cloud resource should simply be creating Target
     * using the url field. account and service fields should be used only by those clients who want to post events
     * related to a SP specific subscription or resource events.
     */
    public static class Target {
        /**
         * URL of the resource
         */
        @JsonProperty("url")
        private URL url;

        /**
         * Resource types
         */
        @JsonProperty("types")
        private List<String> types;


        @JsonCreator
        public Target(@JsonProperty("url") URL url, @JsonProperty("types") String... types) {
            this.url = url;
            this.types = Arrays.asList(types);
        }

        /**
         * This is a convenience Target build to construct Target for Services Platform resources.
         */
        public static class SpTargetBuilder {
            private  String account;
            private  final String service;
            private  String resourceId;
            private   String resourceType;

            /**
             *
             * @param service Name of the service, for example, dev-at-cloud or cb-app or cb-db
             */
            public SpTargetBuilder(@Nonnull String service) {
                this.service = service;
            }

            /**
             * If resourceId is given account also must be present.
             *
             * @param resourceId resourceId resource identifier
             * @param account Account or domain name
             */
            public SpTargetBuilder resourceId(@Nonnull String resourceId, @Nonnull String account){
                this.resourceId = resourceId;
                this.account = account;
                return this;
            }

            /**
             * Resource type of given resourceId. For example, 'application', 'database' etc.
             */
            public SpTargetBuilder resourceType(@Nonnull String resourceType){
                this.resourceType = resourceType;
                return this;
            }

            /**
             * @param account Account or domain name
             */
            public SpTargetBuilder account(@Nonnull String account){
                this.account = account;
                return this;
            }

            /**
             * Build Event Target using specific eventEndpoint. Use this method if you know which Services Platform endpoint
             * you plan to call.
             *
             * Possible values are
             * <br/>
             *  https://services-dev.apps.cloudbees.com - For development <br/>
             *  https://services-platform.cloudbees.com - For production
             *
             * @param eventEndpoint Can be null, defaults to production endpoint
             *
             * @throws EventApiException
             *
             */
            public Target build(@Nullable String eventEndpoint) throws EventApiException {
                if(eventEndpoint == null){
                    eventEndpoint = EventApi.SP_PORD_BASE_URL;
                }

                if(service == null){
                    throw new EventApiException("service must be non-null");
                }

                if(resourceId != null && account == null){
                    throw new EventApiException("account is needed if resourceId is given");
                }
                UriBuilder uriBuilder = UriBuilder.fromUri(eventEndpoint).path("api/services/");
                String type;
                if (resourceId != null) {
                    uriBuilder.path("resources").path(service).path(account).path(resourceId);
                    type = "https://types.cloudbees.com/resource/services-platform/resource";
                    if(resourceType != null){
                        type += "/" + resourceType;
                    }
                } else {
                    uriBuilder.path("subscriptions").path(service);
                    if(account!=null){
                        uriBuilder.path(account);
                    }
                    type = "https://types.cloudbees.com/resource/services-platform/service/"+service;
                }

                try {
                    return new Target(uriBuilder.build().toURL(), type);
                } catch (MalformedURLException e) {
                    String error = "Failed to construct Target url: "+e.getMessage();
                    logger.error(error);
                    throw new EventApiException(error, e);
                }
            }

            /**
             * Construct Target using default Event endpoint
             *
             * @throws EventApiException
             */
            public Target build() throws EventApiException {
                return this.build(null);
            }
        }
    }

    /**
     * Event related data
     *
     * There are four predefined elements. title, description, icon and url.
     *
     * title - Required. example, "Application helloworld deployed"
     * description - Optional. example, "Application helloworld event"
     * icon - Optional. URL of the icon related to this event
     * url - Optional. URL where more details of the event can be found
     */
    public static class EventData extends LinkedHashMap{
        private EventData(String title){
            super();
            put("title", title);
        }

        public static class Builder{
            private final EventData data;

            public Builder(String title) {
                this.data = new EventData(title);
            }

            public Builder description(String description){
                data.put("description", description);
                return this;
            }
            public Builder icon(URL icon){
                data.put("icon", icon);
                return this;
            }

            public Builder url(URL url){
                data.put("url", url);
                return this;
            }

            public EventData build(){
                return data;
            }
        }
    }

}
