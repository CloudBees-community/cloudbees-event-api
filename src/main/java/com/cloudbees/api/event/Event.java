package com.cloudbees.api.event;


import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;
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
        @JsonIgnore
        private String account;

        @JsonIgnore
        private String service;

        /**
         * URL of the resource
         */
        @JsonProperty("url")
        private String url;

        /**
         * Resource types
         */
        @JsonProperty("types")
        private List<String> types;


        @JsonCreator
        public Target(@JsonProperty("url") String url, @JsonProperty("types") String... types) {
            this.url = url;
            this.types = Arrays.asList(types);
        }

        /**
         * Call this constructor if the event is targeted to Services Platform subscription or resource
         * <p/>
         * For testing you can set SP_URL Java system property or environment variable to Services Platform
         * URL of DEV.
         *
         * @param account    Account or domain name
         * @param service    Name of the service, for example, dev-at-cloud or cb-app or cb-db
         * @param resourceId resource identifier
         */
        public Target(@Nonnull String account, @Nonnull String service, @Nullable String resourceId) {

            String spUrl = System.getProperty("SP_URL");
            if (spUrl == null) {
                spUrl = System.getenv("SP_URL");
            }

            if (spUrl == null || (!spUrl.equals(EventApi.SP_DEV_BASE_URL) && !spUrl.equals(EventApi.SP_PORD_BASE_URL))) {
                spUrl = EventApi.SP_PORD_BASE_URL;
            }

            UriBuilder uriBuilder = UriBuilder.fromUri(spUrl).path("api/services/");
            if (resourceId != null) {
                uriBuilder.path("resources").path(service).path(account).path(resourceId);
            } else {
                uriBuilder.path("subscriptions").path(service).path(account);
            }

            this.url = uriBuilder.build().toString();
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
