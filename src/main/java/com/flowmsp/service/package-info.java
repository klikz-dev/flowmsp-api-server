/**
 * The service package contains two types of classes.
 *
 * The first set of classes are those that implement business logic that require access to
 * multiple Dao classes, for example the SignupService which is used to create a new customer,
 * user and password during the signup process. The second set of classes are those that
 * implement some functionality that does not directly interact with the Dao classes themselves.
 * Typically these classes interact with some external service of function, for example
 * of lat/lon lookup for zipcodes and geographies.
 *
 * Classes whose name ends in "Service" are initialized via a constructor and are used via this
 * instance. Classes whose name ends in "Util" are static classes where all methods are static.
 * These classes should be implemented as Java 8 interfaces with static methods.
 */
package com.flowmsp.service;