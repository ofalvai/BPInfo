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

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

public class Route implements Serializable {

    @NonNull
    private String id;

    /**
     * Usually the route number (eg. 74, M3, 7E), sometimes its name (eg. Funicular)
     */
    @Nullable
    private String shortName;

    /**
     * Longer name of the route, when the short name is not obvious (eg. replacement routes)
     */
    @Nullable
    private String longName;

    /**
     * Currently the terminal stops of the route
     */
    @Nullable
    private String description;

    @NonNull
    private RouteType type;

    /**
     * Background color of the rectangle around the label
     */
    @ColorInt
    private int color;

    @ColorInt
    private int textColor;

    public Route(String id, String shortName, String longName, String description, RouteType type, @ColorInt int color, @ColorInt int textColor) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
        this.description = description;
        this.type = type;
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

    @ColorInt
    public int getColor() {
        return color;
    }

    public void setColor(@ColorInt int color) {
        this.color = color;
    }

    @ColorInt
    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(@ColorInt int textColor) {
        this.textColor = textColor;
    }
}
