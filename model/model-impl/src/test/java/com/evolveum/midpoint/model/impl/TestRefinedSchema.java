/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRefinedSchema extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/refinedschema");

    public static final File TASK_RECONCILE_DUMMY_OBJECTCLASS_FILE = new File(TEST_DIR,
            "task-reconcile-dummy-objectclass.xml");
    public static final String TASK_RECONCILE_DUMMY_OBJECTCLASS_OID = "bed15976-e604-11e5-a181-af0dade5e5a0";

    public static final File TASK_RECONCILE_DUMMY_KIND_INTENT_FILE = new File(TEST_DIR,
            "task-reconcile-dummy-kind-intent.xml");
    public static final String TASK_RECONCILE_DUMMY_KIND_INTENT_OID = "d4cd18f2-e60c-11e5-a806-3faae6c13aff";

    public static final File TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_FILE = new File(TEST_DIR,
            "task-reconcile-dummy-kind-intent-objectclass.xml");
    public static final String TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_OID = "3f2a1140-e60e-11e5-adb7-776abfbb2227";

    private ResourceSchema refinedSchema;
    private ResourceSchema refinedSchemaModel;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        InternalMonitor.reset();
    }

    @Test
    public void test000Sanity() throws Exception {
        // WHEN
        refinedSchema = ResourceSchemaFactory.getCompleteSchema(getDummyResourceType());

        displayDumpable("Dummy refined schema", refinedSchema);

        // THEN
        getDummyResourceController().assertRefinedSchemaSanity(refinedSchema);
    }

    @Test
    public void test010SanityModel() throws Exception {
        // WHEN
        refinedSchemaModel = ResourceSchemaFactory.getCompleteSchema(getDummyResourceType(), LayerType.MODEL);

        displayDumpable("Dummy refined schema (MODEL)", refinedSchemaModel);

        // THEN
        getDummyResourceController().assertRefinedSchemaSanity(refinedSchemaModel);

        assertEquals("Wrong layer", LayerType.MODEL, refinedSchemaModel.getCurrentLayer());
    }

    @Test
    public void test100EntitlementRefinedObjectClasses() {
        // WHEN
        Collection<? extends ResourceObjectTypeDefinition> entitlementROcDefs = refinedSchema.getObjectTypeDefinitions(ShadowKindType.ENTITLEMENT);

        display("entitlement rOcDefs", entitlementROcDefs);

        // THEN
        for (ResourceObjectTypeDefinition entitlementROcDef : entitlementROcDefs) {
            assertEquals("Wrong kind in " + entitlementROcDef, ShadowKindType.ENTITLEMENT, entitlementROcDef.getKind());
        }

        assertEquals("Wrong number of entitlement rOcDefs", 6, entitlementROcDefs.size());
    }

    @Test
    public void test101EntitlementRefinedObjectClassesModel() {
        // WHEN
        Collection<? extends ResourceObjectTypeDefinition> entitlementROcDefs = refinedSchemaModel.getObjectTypeDefinitions(ShadowKindType.ENTITLEMENT);

        display("entitlement rOcDefs", entitlementROcDefs);

        // THEN
        for (ResourceObjectTypeDefinition entitlementROcDef : entitlementROcDefs) {
            assertEquals("Wrong kind in " + entitlementROcDef, ShadowKindType.ENTITLEMENT, entitlementROcDef.getKind());
        }

        assertEquals("Wrong number of entitlement rOcDefs", 6, entitlementROcDefs.size());
    }

    @Test
    public void test110DetermineObjectClassObjectClass() throws Exception {
        OperationResult result = createOperationResult();

        importObjectFromFile(TASK_RECONCILE_DUMMY_OBJECTCLASS_FILE);

        Task task = taskManager.getTaskPlain(TASK_RECONCILE_DUMMY_OBJECTCLASS_OID, result);
        display("Task", task);

        // WHEN
        ResourceObjectDefinition objectClass = ModelImplUtils.determineObjectDefinition(refinedSchema, task);

        // THEN
        displayDumpable("Object class", objectClass);

        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_OBJECTCLASS_OID);

        assertObjectClass(objectClass, RESOURCE_DUMMY_ACCOUNT_OBJECTCLASS_QNAME);
    }

    @Test
    public void test112DetermineObjectClassKindIntent() throws Exception {
        OperationResult result = createOperationResult();

        importObjectFromFile(TASK_RECONCILE_DUMMY_KIND_INTENT_FILE);

        Task task = taskManager.getTaskPlain(TASK_RECONCILE_DUMMY_KIND_INTENT_OID, result);
        display("Task", task);

        // WHEN
        ResourceObjectDefinition objectClass = ModelImplUtils.determineObjectDefinition(refinedSchema, task);

        // THEN
        displayDumpable("Object class", objectClass);

        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_KIND_INTENT_OID);

        assertRefinedObjectClass(objectClass, RESOURCE_DUMMY_PRIVILEGE_OBJECTCLASS_QNAME, ShadowKindType.ENTITLEMENT, "privilege");
    }

    @Test
    public void test114DetermineObjectClassKindIntentObjectClass() throws Exception {
        OperationResult result = createOperationResult();

        importObjectFromFile(TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_FILE);

        Task task = taskManager.getTaskPlain(TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_OID, result);
        display("Task", task);

        // WHEN
        ResourceObjectDefinition objectClass = ModelImplUtils.determineObjectDefinition(refinedSchema, task);

        // THEN
        displayDumpable("Object class", objectClass);

        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_OID);

        assertRefinedObjectClass(objectClass, RESOURCE_DUMMY_PRIVILEGE_OBJECTCLASS_QNAME, ShadowKindType.ENTITLEMENT, "privilege");
    }

    @Test
    public void test120DetermineObjectClassObjectClassModel() throws Exception {
        OperationResult result = createOperationResult();

        importObjectFromFile(TASK_RECONCILE_DUMMY_OBJECTCLASS_FILE);

        Task task = taskManager.getTaskPlain(TASK_RECONCILE_DUMMY_OBJECTCLASS_OID, result);
        display("Task", task);

        // WHEN
        ResourceObjectDefinition objectClass = ModelImplUtils.determineObjectDefinition(refinedSchemaModel, task);

        // THEN
        displayDumpable("Object class", objectClass);
        displayValue("Object class (toString)", objectClass.toString());

        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_OBJECTCLASS_OID);

        assertObjectClass(objectClass, RESOURCE_DUMMY_ACCOUNT_OBJECTCLASS_QNAME);
    }

    @Test
    public void test122DetermineObjectClassKindIntentModel() throws Exception {
        OperationResult result = createOperationResult();

        importObjectFromFile(TASK_RECONCILE_DUMMY_KIND_INTENT_FILE);

        Task task = taskManager.getTaskPlain(TASK_RECONCILE_DUMMY_KIND_INTENT_OID, result);
        display("Task", task);

        // WHEN
        ResourceObjectDefinition objectClass = ModelImplUtils.determineObjectDefinition(refinedSchemaModel, task);

        // THEN
        displayDumpable("Object class", objectClass);
        displayValue("Object class (toString)", objectClass.toString());

        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_KIND_INTENT_OID);

        assertLayerRefinedObjectClass(objectClass, RESOURCE_DUMMY_PRIVILEGE_OBJECTCLASS_QNAME,
                ShadowKindType.ENTITLEMENT, "privilege", LayerType.MODEL);
    }

    @Test
    public void test124DetermineObjectClassKindIntentObjectClassModel() throws Exception {
        OperationResult result = createOperationResult();

        importObjectFromFile(TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_FILE);

        Task task = taskManager.getTaskPlain(TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_OID, result);
        display("Task", task);

        // WHEN
        ResourceObjectDefinition objectClass = ModelImplUtils.determineObjectDefinition(refinedSchemaModel, task);

        // THEN
        displayDumpable("Object class", objectClass);
        displayValue("Object class (toString)", objectClass.toString());

        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_OID);

        assertLayerRefinedObjectClass(objectClass, RESOURCE_DUMMY_PRIVILEGE_OBJECTCLASS_QNAME,
                ShadowKindType.ENTITLEMENT, "privilege", LayerType.MODEL);
    }

    private void assertObjectClass(
            ResourceObjectDefinition objectClass, QName objectClassQName) {
        assertNotNull("No object class", objectClass);
        assertEquals("Wrong object class QName in object class " + objectClass, objectClassQName, objectClass.getTypeName());
    }

    private void assertRefinedObjectClass(ResourceObjectDefinition objectClass,
            QName objectClassQName, ShadowKindType kind, String intent) {
        assertNotNull("No object class", objectClass);
        if (!(objectClass instanceof ResourceObjectTypeDefinition)) {
            AssertJUnit.fail("Expected refined object class definition, but it was " + objectClass + " (" + objectClass.getClass() + ")");
        }
        ResourceObjectTypeDefinition rOcDef = (ResourceObjectTypeDefinition) objectClass;
        assertEquals("Wrong object class QName in rOcDef " + rOcDef, objectClassQName, rOcDef.getTypeName());
        assertEquals("Wrong kind in rOcDef " + rOcDef, kind, rOcDef.getKind());
        assertEquals("Wrong kind in rOcDef " + rOcDef, intent, rOcDef.getIntent());
    }

    private void assertLayerRefinedObjectClass(ResourceObjectDefinition objectClass,
            QName objectClassQName, ShadowKindType kind, String intent, LayerType layer) {
        assertRefinedObjectClass(objectClass, objectClassQName, kind, intent);
        assertEquals("Wrong layer", layer, objectClass.getCurrentLayer());
    }
}
