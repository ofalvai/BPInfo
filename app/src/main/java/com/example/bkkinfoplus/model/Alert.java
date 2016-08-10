/*
 * Copyright 2016 Olivér Falvai
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.example.bkkinfoplus.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

        this.affectedRoutes = new ArrayList<>();
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
