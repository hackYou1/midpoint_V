/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.session;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */

@Component
public class RemoveUnusedSecurityFilterPublisher {

    private static final Trace LOGGER = TraceManager.getTrace(RemoveUnusedSecurityFilterPublisher.class);

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishCustomEvent(final MidpointAuthentication mpAuthentication) {
        LOGGER.trace("Publishing RemoveUnusedSecurityFilterEvent event. With authentication: " + mpAuthentication);
        RemoveUnusedSecurityFilterEvent customSpringEvent = new RemoveUnusedSecurityFilterEvent(this, mpAuthentication);
        applicationEventPublisher.publishEvent(customSpringEvent);
    }
}
