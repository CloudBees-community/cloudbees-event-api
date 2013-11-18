package com.cloudbees.api.event;

import javax.annotation.Nonnull;
import javax.ws.rs.core.UriBuilder;
import java.util.Date;

/**
 * @author Vivek Pandey
 */
public class QueryEventObject {
    private String account;
    private String service;
    private String resource;
    private String cloudResourceType;
    private String cloudResource;
    private Date from;
    private Date to;
    private String[] types;
    private int limit;
    private int offset;
    private String queryUrl;

    public String toUri(){
        return queryUrl;
    }

    public static class QueryBuilder{

        private final QueryEventObject qeo;
        private final UriBuilder uriBuilder;

        public QueryBuilder(String eventUri){
            this.uriBuilder = UriBuilder.fromUri(eventUri);
            this.qeo = new QueryEventObject();
        }

        public QueryBuilder account(String account){
            this.qeo.account = account;
            return this;
        }

        public QueryBuilder service(String service){
            this.qeo.service = service;
            return this;
        }

        public QueryBuilder resource(String resource){
            this.qeo.resource = resource;
            return this;
        }

        public QueryBuilder from(Date from){
            this.qeo.from = from;
            return this;
        }

        public QueryBuilder to(Date to){
            this.qeo.to = to ;
            return this;
        }

        public QueryBuilder cloudResourceType(String cloudResourceType){
            this.qeo.cloudResourceType = cloudResourceType;
            return this;
        }

        public QueryBuilder limit(int limit){
            this.qeo.limit = limit;
            return this;
        }

        public QueryBuilder offset(int offset){
            this.qeo.offset = offset;
            return this;
        }

        /**
         * If cloudResource is provided, service,resource,account are ignored. service, resource and account
         * are applicable only if you are querying about Services Platform specific resources.
         *
         * @param cloudResource CR URL
         */
        public QueryBuilder cloudResource(@Nonnull String cloudResource){
            this.qeo.cloudResource = cloudResource;

            return this;
        }

        /**
         * Event type, For example info, alert etc.
         *
         * @param types array of types
         */
        public QueryBuilder types(String...types){
            this.qeo.types = types;
            return this;
        }


        public QueryEventObject build() throws EventApiException {
            if(qeo.account != null){
                uriBuilder.queryParam("account", qeo.account);
            }
            if(qeo.service != null){
                uriBuilder.queryParam("service",qeo.service);
            }
            if(qeo.resource != null){
                if(qeo.service == null){
                    throw new EventApiException("service must be set with resource: "+ qeo.resource);
                }
                if(qeo.account == null){
                    throw new EventApiException("account must be set with resource: "+ qeo.resource);
                }
                uriBuilder.queryParam("resource", qeo.resource);
            }

            if(qeo.from != null){
                uriBuilder.queryParam("from", qeo.from.getTime());
            }

            if(qeo.to != null){
                uriBuilder.queryParam("to", qeo.to.getTime());
            }
            if(qeo.limit > 0){
                uriBuilder.queryParam("limit", qeo.limit);
            }

            if(qeo.offset > 0){
                uriBuilder.queryParam("offset",qeo.offset);
            }

            if(qeo.cloudResource != null){
                uriBuilder.queryParam("cloud_resource", qeo.cloudResource);
            }

            if(qeo.cloudResourceType != null){
                uriBuilder.queryParam("cloud_resource_type", qeo.cloudResourceType);
            }
            StringBuffer sb = new StringBuffer();
            if(qeo.types != null){
                for(String t : qeo.types){
                    if(sb.length() > 0){
                        sb.append(",");
                    }
                    sb.append(t);
                }
            }
            if(sb.length() > 0){
                uriBuilder.queryParam(sb.toString());
            }

            qeo.queryUrl = uriBuilder.build().toString();
            return this.qeo;
        }
    }
}
