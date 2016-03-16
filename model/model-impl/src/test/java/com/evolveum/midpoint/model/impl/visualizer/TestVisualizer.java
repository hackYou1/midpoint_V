/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.migrator.Migrator;
import com.evolveum.midpoint.model.impl.visualizer.output.SceneImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.util.TestUtil.*;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestVisualizer extends AbstractInternalModelIntegrationTest {

	@Autowired
	private Visualizer visualizer;

	@Autowired
	private PrismContext prismContext;

	@Autowired
	private TaskManager taskManager;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	@Test
	public void test100UserBasic() throws SchemaException {
		final String TEST_NAME = "test100UserBasic";
		Task task = createTask(TEST_NAME);

		PrismObject<UserType> u = prismContext.createObject(UserType.class);
		u.setOid("123");
		u.asObjectable().setName(new PolyStringType("user123"));
		u.asObjectable().setFullName(new PolyStringType("User User123"));

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualize(u, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test110UserWithContainers() throws SchemaException {
		final String TEST_NAME = "test101UserWithContainers";
		Task task = createTask(TEST_NAME);

		PrismObject<UserType> u = prismContext.createObject(UserType.class);
		UserType ut = u.asObjectable();
		u.setOid("456");
		ut.setName(new PolyStringType("user456"));
		ut.setFullName(new PolyStringType("User User456"));
		ut.setActivation(new ActivationType(prismContext));
		ut.getActivation().setAdministrativeStatus(ActivationStatusType.ENABLED);
		ut.getActivation().setValidTo(XmlTypeConverter.createXMLGregorianCalendar(2020, 1, 1, 0, 0, 0));
		AssignmentType ass1 = new AssignmentType(prismContext);
		ass1.setActivation(new ActivationType(prismContext));
		ass1.getActivation().setAdministrativeStatus(ActivationStatusType.ENABLED);
		ass1.getActivation().setValidTo(XmlTypeConverter.createXMLGregorianCalendar(2019, 1, 1, 0, 0, 0));
		ass1.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_SUPERUSER_OID, ObjectTypes.ROLE));
		ut.getAssignment().add(ass1);
		AssignmentType ass2 = new AssignmentType(prismContext);
		ass2.setTargetRef(ObjectTypeUtil.createObjectRef("777", ObjectTypes.ROLE));
		ut.getAssignment().add(ass2);
		AssignmentType ass3 = new AssignmentType(prismContext);
		ass3.setConstruction(new ConstructionType(prismContext));
		ass3.getConstruction().setResourceRef(ObjectTypeUtil.createObjectRef(RESOURCE_DUMMY_OID, ObjectTypes.RESOURCE));
		ut.getAssignment().add(ass3);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualize(u, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test200UserDeltaBasic() throws SchemaException {
		final String TEST_NAME = "test200UserDeltaBasic";
		Task task = createTask(TEST_NAME);

		ObjectDelta<?> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_NAME).replace("admin")
				.asObjectDelta(USER_ADMINISTRATOR_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta((ObjectDelta<? extends ObjectType>) delta, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test210UserDeltaContainers() throws SchemaException {
		final String TEST_NAME = "test210UserDeltaContainers";
		Task task = createTask(TEST_NAME);

		AssignmentType ass1 = new AssignmentType(prismContext);
		ass1.setActivation(new ActivationType(prismContext));
		ass1.getActivation().setAdministrativeStatus(ActivationStatusType.ENABLED);
		ass1.getActivation().setValidTo(XmlTypeConverter.createXMLGregorianCalendar(2017, 1, 1, 0, 0, 0));
		ass1.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_SUPERUSER_OID, ObjectTypes.ROLE));

		ObjectDelta<?> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_NAME).replace("admin")
				.item(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.ENABLED)
				.item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_TARGET_REF).replace(ObjectTypeUtil.createObjectRef("123", ObjectTypes.ROLE).asReferenceValue())
				.item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_DESCRIPTION).add("suspicious")
				.item(UserType.F_ASSIGNMENT).add(ass1)
				.asObjectDelta(USER_ADMINISTRATOR_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta((ObjectDelta<? extends ObjectType>) delta, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

}
