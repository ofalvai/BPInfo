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

package com.example.bkkinfoplus;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Config {

    public static final String FUTAR_API_BASE_URL = "http://futar.bkk.hu/bkk-utvonaltervezo-api/ws/otp/api/where/";

    /**
     * Time after the alert list is considered outdated when the user returns to the activity
     */
    public static final int REFRESH_THRESHOLD_SEC = 30;

    public static final DateTimeFormatter FORMATTER_TIME = DateTimeFormat.forPattern("HH:mm");

    public static final DateTimeFormatter FORMATTER_DATE = DateTimeFormat.forPattern("MMM d. EEEE ");

    public static final DateTimeFormatter FORMATTER_DATE_YEAR = DateTimeFormat.forPattern("YYYY MMM d. EEEE ");

    public static final String DATE_SEPARATOR = " ➔ ";

    public static final String SOURCE_CODE_URL = "https://github.com/ofalvai/bkk-info-plus";

    public static final String INSTABUG_TOKEN = BuildConfig.INSTABUG_TOKEN;

}
