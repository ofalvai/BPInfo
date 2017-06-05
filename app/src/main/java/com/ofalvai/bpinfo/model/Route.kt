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

package com.ofalvai.bpinfo.model

import android.support.annotation.ColorInt

import java.io.Serializable

data class Route(val id: String,

                 /**
                  * Usually the route number (eg. 74, M3, 7E), sometimes its name (eg. Funicular)
                  */
                 val shortName: String?,

                 /**
                  * Longer name of the route, when the short name is not obvious (eg. replacement routes)
                  */
                 val longName: String?,

                 /**
                  * Currently the terminal stops of the route
                  */
                 val description: String?,

                 val type: RouteType,

                 /**
                  * Background color of the rectangle around the label
                  */
                 @ColorInt
                 val color: Int,

                 @ColorInt
                 val textColor: Int) : Serializable, Comparable<Route> {

    override fun compareTo(other: Route): Int {
        return this.id.compareTo(other.id)
    }
}
