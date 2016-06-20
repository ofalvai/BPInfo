package com.example.bkkinfoplus;

import java.io.Serializable;

/**
 * Created by oli on 2016. 06. 15..
 */
public class Route implements Serializable {
    private String id;

    private String shortName;

    private String longName;

    private String description;

    private String type; // TODO: enum

    private String URL;

    private String color;

    private String textColor;

    public Route(String id, String shortName, String longName, String description, String type,
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
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
