/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.test.DummyResourceCollection;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Configuration and behavior-controlling data for resource-dummy-interrupted-sync resources.
 * EXPERIMENTAL
 */
public class DummyInterruptedSyncImpreciseResource extends AbstractResourceDummyInterruptedSync {

    private static final File TEST_DIR = new File("src/test/resources/tasks/livesync"); // TODO

    private static final File FILE = new File(TEST_DIR, "resource-dummy-interrupted-sync-imprecise.xml");
    private static final String OID = "e396b76e-e010-46ed-bbf5-a3da78d358ea";
    private static final String NAME = "interruptedSyncImprecise";

    @Override
    public void init(DummyResourceCollection collection, Task task, OperationResult result) throws Exception {
        controller = collection.initDummyResource(NAME, FILE, OID, null, task, result);
        controller.setSyncStyle(DummySyncStyle.DUMB);
    }

    // behavior control, referenced from Groovy code in the resources

    public static long delay;
    public static String errorOn;

    static {
        reset();
    }

    public static void reset() {
        delay = 1;
        errorOn = null;
    }
}
