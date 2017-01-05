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

package com.ofalvai.bpinfo.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.util.List;

public class Alert implements Serializable {

    @NonNull
    private String id;

    /**
     * Start of the alert in miliseconds since the UNIX epoch
     */
    private long start;

    /**
     * End of the alert in miliseconds since the UNIX epoch
     * Might be 0, which means the end is not known yet.
     */
    private long end;

    /**
     * Last modification of alert data
     */
    private long timestamp;

    /**
     * Points to the alert's detail page at the mobile version of BKK Info
     */
    @Nullable
    private String url;

    /**
     * One line title of the alert (diversion, replacement, construction, etc.)
     */
    @Nullable
    private String header;

    /**
     * HTML description of the alert
     */
    @Nullable
    private String description;

    @NonNull
    private List<Route> affectedRoutes;

    public Alert(@NonNull String id, long start, long end, long timestamp, @Nullable String url,
                 @Nullable String header, @Nullable String description,
                 @NonNull List<Route> affectedRoutes) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.timestamp = timestamp;
        this.url = url;
        this.header = header;
        this.description = description;
        this.affectedRoutes = affectedRoutes;
    }

    @NonNull
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

    @Nullable
    public String getUrl() {
        return url;
    }

    public void setUrl(@Nullable String url) {
        this.url = url;
    }

    @Nullable
    public String getHeader() {
        return header;
    }

    public void setHeader(@Nullable String header) {
        this.header = header;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @NonNull
    public List<Route> getAffectedRoutes() {
        return affectedRoutes;
    }

    public void setAffectedRoutes(@NonNull List<Route> affectedRoutes) {
        this.affectedRoutes = affectedRoutes;
    }
}
