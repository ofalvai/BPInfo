package com.ofalvai.bpinfo.model

sealed class Resource<T> {
    class Success<T>(val value: T) : Resource<T>()
    class Loading<T>(val value: T? = null) : Resource<T>()
    class Error<T>(val throwable: Throwable, val value: T? = null) : Resource<T>()
}