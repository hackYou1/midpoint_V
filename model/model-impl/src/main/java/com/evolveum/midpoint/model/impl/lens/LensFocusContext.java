/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.common.LinkManager;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ArchetypeTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class LensFocusContext<O extends ObjectType> extends LensElementContext<O> {

    private static final Trace LOGGER = TraceManager.getTrace(LensFocusContext.class);

    /**
     * True if the focus object was deleted by our processing.
     *
     * (Note we do not currently provide this kind of flag on the projection contexts, because of not being
     * sure if deleted projection cannot be somehow "resurrected" during the processing. For focal objects nothing like
     * this should happen.)
     */
    protected boolean deleted;

    private boolean primaryDeltaConsolidated;

    private transient ArchetypePolicyType archetypePolicy;

    private transient List<ArchetypeType> archetypes;

    private boolean primaryDeltaExecuted;

    // extracted from the template(s)
    // this is not to be serialized into XML, but let's not mark it as transient
    @NotNull private PathKeyedMap<ObjectTemplateItemDefinitionType> itemDefinitionsMap = new PathKeyedMap<>();

    public LensFocusContext(Class<O> objectTypeClass, LensContext<O> lensContext) {
        super(objectTypeClass, lensContext);
    }

    public LensFocusContext(ElementState<O> elementState, LensContext<O> lensContext) {
        super(elementState, lensContext);
    }

    public ArchetypePolicyType getArchetypePolicy() {
        return archetypePolicy;
    }

    public void setArchetypePolicy(ArchetypePolicyType value) {
        this.archetypePolicy = value;
    }

    public ArchetypeType getArchetype() {
        List<PrismObject<ArchetypeType>> prismArchetypes = archetypes.stream()
                .map(ArchetypeType::asPrismObject)
                .collect(Collectors.toList());
        try {
            return PrismObject.asObjectable(
                    ArchetypeTypeUtil.getStructuralArchetype(prismArchetypes));
        } catch (SchemaException e) {
            LOGGER.error("Cannot get structural archetype, {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<ArchetypeType> getArchetypes() {
        return archetypes;
    }

    public void setArchetypes(List<ArchetypeType> archetypes) {
        this.archetypes = archetypes;
    }

    public LifecycleStateModelType getLifecycleModel() {
        if (archetypePolicy == null) {
            return null;
        }
        return archetypePolicy.getLifecycleStateModel();
    }

    public boolean isDelete() {
        return ObjectDelta.isDelete(state.getPrimaryDelta());
    }

    @Override
    public ObjectDelta<O> getSummarySecondaryDelta() {
        return state.getSummarySecondaryDelta();
    }

    public boolean isAdd() {
        return ObjectDelta.isAdd(state.getPrimaryDelta());
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted() {
        deleted = true;
    }

    /**
     * Returns object-delta-object structure based on the current state.
     * I.e. objectCurrent - currentDelta - objectNew.
     */
    @NotNull
    public ObjectDeltaObject<O> getObjectDeltaObjectRelative() {
        return new ObjectDeltaObject<>(getObjectCurrent(), getCurrentDelta(), getObjectNew(), getObjectDefinition())
                .normalizeValuesToDelete(true); // FIXME temporary solution
    }

    @NotNull
    public ObjectDeltaObject<O> getObjectDeltaObjectAbsolute() {
        return new ObjectDeltaObject<>(getObjectOld(), getSummaryDelta(), getObjectNew(), getObjectDefinition())
                .normalizeValuesToDelete(true); // FIXME temporary solution
    }

    // This method may be useful for hooks. E.g. if a hook wants to insert a special secondary delta to avoid
    // splitting the changes to several audit records. It is not entirely clean and we should think about a better
    // solution in the future. But it is good enough for now.
    //
    // The name is misleading but we keep it for compatibility reasons.
    @SuppressWarnings("unused")
    @Deprecated
    public void swallowToWave0SecondaryDelta(ItemDelta<?,?> itemDelta) throws SchemaException {
        swallowToSecondaryDelta(itemDelta);
    }

    @Override
    public void cleanup() {
        // Clean up only delta in current wave. The deltas in previous waves are already done.
        // FIXME: this somehow breaks things. don't know why. but don't really care. the waves will be gone soon anyway
//        if (secondaryDeltas.get(getWave()) != null) {
//            secondaryDeltas.remove(getWave());
//        }
    }

//    @Override
//    public void reset() {
//        super.reset();
//        secondaryDeltas = new ObjectDeltaWaves<O>();
//    }

    /**
     * Returns true if there is any change in organization membership.
     * I.e. in case that there is a change in parentOrgRef.
     */
    public boolean hasOrganizationalChange() {
        return hasChangeInItem(SchemaConstants.PATH_PARENT_ORG_REF);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean hasChangeInItem(ItemPath itemPath) {
        if (isAdd()) {
            PrismObject<O> objectNew = getObjectNew();
            if (objectNew == null) {
                return false;
            }
            Item<PrismValue,ItemDefinition> item = objectNew.findItem(itemPath);
            return item != null && !item.getValues().isEmpty();
        } else if (isDelete()) {
            // We do not care any more
            return false;
        } else {
            ObjectDelta<O> summaryDelta = getSummaryDelta();
            return summaryDelta != null && summaryDelta.hasItemDelta(itemPath);
        }
    }

    public LensFocusContext<O> clone(LensContext<O> lensContext) {
        LensFocusContext<O> clone = new LensFocusContext<>(state.clone(), lensContext);
        copyValues(clone);
        return clone;
    }

    private void copyValues(LensFocusContext<O> clone) {
        super.copyValues(clone);
        clone.deleted = deleted;
        clone.primaryDeltaConsolidated = primaryDeltaConsolidated;
        clone.archetypePolicy = archetypePolicy;
        clone.archetypes = archetypes != null ? new ArrayList<>(archetypes) : null;
        clone.primaryDeltaExecuted = primaryDeltaExecuted;
    }

    public String dump(boolean showTriples) {
        return debugDump(0, showTriples);
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, true);
    }

    public String debugDump(int indent, boolean showTriples) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getDebugDumpTitle());
        if (!isFresh()) {
            sb.append(", NOT FRESH");
        }
        if (deleted) {
            sb.append(", DELETED");
        }
        sb.append(", oid=");
        sb.append(getOid());
        if (getIteration() != 0) {
            sb.append(", iteration=").append(getIteration()).append(" (").append(getIterationToken()).append(")");
        }
        sb.append("\n");

        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("old"), getObjectOld(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("current"), getObjectCurrent(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("new"), getObjectNew(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("deleted"), deleted, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("primary delta"), getPrimaryDelta(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("secondary delta"), getSecondaryDelta(), indent+1);
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append(getDebugDumpTitle("older secondary deltas")).append(":");
        ObjectDeltaWaves<O> secondaryDeltas = state.getArchivedSecondaryDeltas();
        if (secondaryDeltas.isEmpty()) {
            sb.append(" empty");
        } else {
            sb.append("\n");
            sb.append(secondaryDeltas.debugDump(indent + 2));
        }
        sb.append("\n");

        DebugUtil.debugDumpWithLabelLn(sb, getDebugDumpTitle("executed deltas"), getExecutedDeltas(), indent+1);
        DebugUtil.debugDumpWithLabel(sb, "Policy rules context", policyRulesContext, indent + 1);
        return sb.toString();
    }

    @Override
    protected String getElementDefaultDesc() {
        return "focus";
    }

    @Override
    public String toString() {
        return "LensFocusContext(" + getObjectTypeClass().getSimpleName() + ":" + getOid() + ")";
    }

    public String getHumanReadableName() {
        StringBuilder sb = new StringBuilder();
        sb.append("focus(");
        PrismObject<O> object = getObjectNew();
        if (object == null) {
            object = getObjectOld();
        }
        if (object == null) {
            sb.append(getOid());
        } else {
            sb.append(object);
        }
        sb.append(")");
        return sb.toString();
    }

    LensFocusContextType toLensFocusContextType(LensContext.ExportType exportType) throws SchemaException {
        LensFocusContextType rv = new LensFocusContextType(PrismContext.get());
        super.storeIntoLensElementContextType(rv, exportType);
        if (exportType != LensContext.ExportType.MINIMAL) {
            rv.setSecondaryDeltas(state.getArchivedSecondaryDeltas().toObjectDeltaWavesBean());
        }
        return rv;
    }

    static <O extends ObjectType> LensFocusContext<O> fromLensFocusContextType(
            LensFocusContextType focusContextType, LensContext lensContext, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

        String objectTypeClassString = focusContextType.getObjectTypeClass();
        if (StringUtils.isEmpty(objectTypeClassString)) {
            throw new SystemException("Object type class is undefined in LensFocusContextType");
        }
        LensFocusContext<O> lensFocusContext;
        try {
            //noinspection unchecked
            lensFocusContext = new LensFocusContext(Class.forName(objectTypeClassString), lensContext);
        } catch (ClassNotFoundException e) {
            throw new SystemException("Couldn't instantiate LensFocusContext because object type class couldn't be found", e);
        }

        lensFocusContext.retrieveFromLensElementContextBean(focusContextType, task, result);
        ObjectDeltaWaves.fillObjectDeltaWaves(
                lensFocusContext.state.getArchivedSecondaryDeltas(),
                focusContextType.getSecondaryDeltas());

        // fixing provisioning type in delta (however, this is not usually needed, unless primary object is shadow or resource
        Objectable object;
        if (lensFocusContext.getObjectNew() != null) {
            object = lensFocusContext.getObjectNew().asObjectable();
        } else if (lensFocusContext.getObjectOld() != null) {
            object = lensFocusContext.getObjectOld().asObjectable();
        } else {
            object = null;
        }
        for (ObjectDelta<O> delta : lensFocusContext.state.getArchivedSecondaryDeltas()) {
            if (delta != null) {
                lensFocusContext.applyProvisioningDefinition(delta, object, task, result);
            }
        }

        return lensFocusContext;
    }

    @Override
    public void checkConsistence(String desc) {
        state.checkConsistence(this, desc);

        // all executed deltas should have the same oid (if any)
        String oid = null;
        for (LensObjectDeltaOperation<?> operation : getExecutedDeltas()) {
            String oid1 = operation.getObjectDelta().getOid();
            if (oid == null) {
                if (oid1 != null) {
                    oid = oid1;
                }
            } else {
                if (oid1 != null && !oid.equals(oid1)) {
                    String m = "Different OIDs in focus executed deltas: " + oid + ", " + oid1;
                    LOGGER.error("{}: context = \n{}", m, this.debugDump());
                    throw new IllegalStateException(m);
                }
            }
        }
    }

    @Override
    void doExtraObjectConsistenceCheck(@NotNull PrismObject<O> object, String elementDesc, String contextDesc) {
    }

    public void setItemDefinitionsMap(@NotNull PathKeyedMap<ObjectTemplateItemDefinitionType> itemDefinitionsMap) {
        this.itemDefinitionsMap = itemDefinitionsMap;
    }

    @NotNull
    public PathKeyedMap<ObjectTemplateItemDefinitionType> getItemDefinitionsMap() {
        return itemDefinitionsMap;
    }

    // preliminary implementation
    public LinkTypeDefinitionType getSourceLinkTypeDefinition(@NotNull String linkTypeName, LinkManager linkManager,
            OperationResult result) throws SchemaException, ConfigurationException {
        PrismObject<O> objectAny = getObjectAny();
        return objectAny != null ? linkManager.getSourceLinkTypeDefinition(linkTypeName, objectAny, result) : null;
    }

    // preliminary implementation
    public LinkTypeDefinitionType getTargetLinkTypeDefinition(@NotNull String linkTypeName, LinkManager linkManager,
            OperationResult result) throws SchemaException, ConfigurationException {
        PrismObject<O> objectAny = getObjectAny();
        return objectAny != null ? linkManager.getTargetLinkTypeDefinition(linkTypeName, objectAny, result) : null;
    }

    public boolean isPrimaryDeltaConsolidated() {
        return primaryDeltaConsolidated;
    }

    public void setPrimaryDeltaConsolidated(boolean primaryDeltaConsolidated) {
        this.primaryDeltaConsolidated = primaryDeltaConsolidated;
    }

    /**
     * Updates the state to reflect that a delta was executed.
     *
     * CURRENTLY CALLED ONLY FOR FOCUS. ASSUMES SUCCESSFUL EXECUTION.
     */
    void updateAfterExecution() {
        state.updateAfterExecution(lensContext.getExecutionWave());
    }

    boolean primaryItemDeltaExists(ItemPath path) {
        ObjectDelta<O> primaryDelta = getPrimaryDelta();
        return primaryDelta != null &&
                !ItemDelta.isEmpty(primaryDelta.findItemDelta(path));
    }

    public void deleteEmptyPrimaryDelta() {
        state.deleteEmptyPrimaryDelta();
    }
}
