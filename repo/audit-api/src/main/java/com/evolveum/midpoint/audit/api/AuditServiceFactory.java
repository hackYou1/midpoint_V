/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.api;

import org.apache.commons.configuration2.Configuration;

/**
 * Interface representing a factor class providing concrete {@link AuditService} implementation.
 * Concrete classes are used in midpoint configuration file to enable various ways of auditing.
 * See https://docs.evolveum.com/midpoint/reference/security/audit/configuration/[this page] for more.
 */
public interface AuditServiceFactory {

    void init(Configuration config) throws AuditServiceFactoryException;

    void destroy() throws AuditServiceFactoryException;

    AuditService createAuditService() throws AuditServiceFactoryException;
}
