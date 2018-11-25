package com.creations.livebox.util


/**
 * Returns `true` if the provided reference is `null` otherwise
 * returns `false`.
 *
 * @param obj a reference to be checked against `null`
 * @return `true` if the provided reference is `null` otherwise
 * `false`
 * @apiNote This method exists to be used adapt a
 * [java.util.function.Predicate], `filter(Objects::isNull)`
 * @see java.util.function.Predicate
 *
 * @since 1.8
 */
fun isNull(obj: Any?): Boolean = obj == null

/**
 * Returns `true` if the provided reference is non-`null`
 * otherwise returns `false`.
 *
 * @param obj a reference to be checked against `null`
 * @return `true` if the provided reference is non-`null`
 * otherwise `false`
 * @apiNote This method exists to be used adapt a
 * [java.util.function.Predicate], `filter(Objects::nonNull)`
 * @see java.util.function.Predicate
 *
 * @since 1.8
 */
fun nonNull(obj: Any?): Boolean = obj != null
