package com.airbnb.android.showkase.exceptions

/**
 * Used to throw an exception for Showkase specific errors.
 */
internal class ShowkaseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
