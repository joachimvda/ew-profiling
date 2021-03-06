/*
 * This file is part of ew-profiling, a library for in-app, runtime profiling.
 * Copyright (c) Eliwan bvba, Belgium, http://eliwan.be
 *
 * The software is available in open source according to the Apache License, Version 2.0.
 * For full licensing details, see LICENSE.txt in the project root.
 */

package be.eliwan.profiling.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Wrapping JDBC object which can be used to profile the time spent communicating with the database.
 */
public class ProfilingInvocationHandler implements InvocationHandler {

    private String groupPrefix;
    private Object delegate;
    private String query;

    /**
     * Constructor.
     *
     * @param groupPrefix group prefix
     * @param delegate the "real" prepared statement which is profiled.
     */
    public ProfilingInvocationHandler(String groupPrefix, Object delegate) {
        this.groupPrefix = groupPrefix;
        this.delegate = delegate;
    }

    /**
     * Constructor.
     *
     * @param groupPrefix group prefix
     * @param delegate the "real" prepared statement which is profiled.
     * @param query query
     */
    public ProfilingInvocationHandler(String groupPrefix, Object delegate, String query) {
        this.groupPrefix = groupPrefix;
        this.delegate = delegate;
        this.query = query;
    }

    @Override
    // CHECKSTYLE THROWS_THROWABLE: OFF
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isGetterOrSetter(method)) {
            return method.invoke(delegate, args);
        } else {
            long start = System.currentTimeMillis();
            String query = extractQuery(method, args);
            try {
                return method.invoke(delegate, args);
            } finally {
                ProfilingDriver.register(groupPrefix + method.getName(), System.currentTimeMillis() - start);
                if (null != query) {
                    ProfilingDriver.registerQuery(groupPrefix + method.getName(), query, System.currentTimeMillis() - start);
                }
            }
        }
    }
    // CHECKSTYLE THROWS_THROWABLE: ON

    private String extractQuery(Method method, Object[] args) {
        String res = null;
        String methodName = method.getName();
        if (("execute".equals(methodName) || "executeQuery".equals(methodName) || "executeUpdate".equals(methodName))
                && null != args && 1 == args.length && args[0] instanceof String) {
            res = (String) args[0];
        }
        if (("execute".equals(methodName) || "executeQuery".equals(methodName) || "executeUpdate".equals(methodName))
                && (null == args || 0 == args.length)) {
            res = query;
        }
        return res;
    }

    private boolean isGetterOrSetter(Method method) {
        try {
            String methodName = method.getName();
            return (methodName.startsWith("get") && !("getResultSet".equals(methodName) || "getMoreResults".equals(methodName)))
                    || methodName.startsWith("is") || methodName.startsWith("set");
        } catch (Exception ex) {
            return false; // just in case, better be safe than sorry
        }
    }

}
