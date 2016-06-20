package com.example.bkkinfoplus;

import java.io.Serializable;
import java.util.List;

/**
 * Created by oli on 2016. 06. 14..
 */
public class Alert implements Serializable {

    private String id;

    private long start;

    private long end;

    private long timestamp;

    private List<String> stopIds;

    private List<String> routeIds;

    private String url;

    private String header;

    private String description;

    private List<Route> affectedRoutes;

    public void setAffectedRoutes(List<Route> affectedRoutes) {
        this.affectedRoutes = affectedRoutes;
    }

    public Alert(String id, long start, long end, long timestamp, List<String> stopIds,
                 List<String> routeIds, String url, String header, String description) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.timestamp = timestamp;
        this.stopIds = stopIds;
        this.routeIds = routeIds;
        this.url = url;
        this.header = header;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getStopIds() {
        return stopIds;
    }

    public void setStopIds(List<String> stopIds) {
        this.stopIds = stopIds;
    }

    public List<String> getRouteIds() {
        return routeIds;
    }

    public void setRouteIds(List<String> routeIds) {
        this.routeIds = routeIds;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Route> getAffectedRoutes() {
        return affectedRoutes;
    }
}
