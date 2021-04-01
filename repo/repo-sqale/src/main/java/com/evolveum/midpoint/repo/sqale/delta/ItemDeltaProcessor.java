/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;

/**
 * TODO
 */
public abstract class ItemDeltaProcessor {

    protected final SqaleUpdateContext<?, ?, ?> context;

    protected ItemDeltaProcessor(SqaleUpdateContext<?, ?, ?> context) {
        this.context = context;
    }

    public abstract void process(ItemDelta<?, ?> modification) throws RepositoryException;
}