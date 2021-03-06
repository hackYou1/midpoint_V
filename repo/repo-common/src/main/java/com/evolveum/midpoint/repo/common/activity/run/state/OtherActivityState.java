/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * State of activity not connected to the current activity run.
 * Does not maintain live progress/statistics and assumes that the work state was already created.
 * (Otherwise it cannot use it.)
 */
public class OtherActivityState extends ActivityState {

    /**
     * Task in which this activity state resides.
     */
    @NotNull private final Task task;

    @NotNull private final ActivityPath activityPath;

    private ComplexTypeDefinition workStateComplexTypeDefinition;

    public OtherActivityState(@NotNull Task task, @NotNull TaskActivityStateType taskActivityState,
            @NotNull ActivityPath activityPath, @NotNull QName workStateTypeName, @NotNull CommonTaskBeans beans) {
        super(beans);
        this.task = task;
        this.activityPath = activityPath;
        this.stateItemPath = ActivityStateUtil.getStateItemPath(taskActivityState, activityPath);
        initialize(workStateTypeName);
    }

    public @NotNull Task getTask() {
        return task;
    }

    @Override
    public @NotNull ActivityPath getActivityPath() {
        return activityPath;
    }

    public void initialize(@NotNull QName workStateTypeName) {
        // TODO we can derive the definition also from the existing work state (if present)
        workStateComplexTypeDefinition = determineWorkStateDefinition(workStateTypeName);
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "Task", String.valueOf(task), indent + 1);
    }

    @Override
    public @NotNull ComplexTypeDefinition getWorkStateComplexTypeDefinition() {
        return workStateComplexTypeDefinition;
    }

    @Override
    public String toString() {
        return "OtherActivityState{" +
                "activityPath=" + activityPath +
                ", task=" + task +
                ", stateItemPath=" + stateItemPath +
                '}';
    }
}
