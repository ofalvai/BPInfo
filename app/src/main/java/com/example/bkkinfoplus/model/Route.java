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

public class Route implements Serializable {
    private String id;

    private String shortName;

    private String longName;

    private String description;

    private RouteType type;

    private String URL;

    private String color;

    private String textColor;

    public Route(String id, String shortName, String longName, String description, RouteType type,
                 String URL, String color, String textColor) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
        this.description = description;
        this.type = type;
        this.URL = URL;
        this.color = color;
        this.textColor = textColor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RouteType getType() {
        return type;
    }

    public void setType(RouteType type) {
        this.type = type;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }
}
