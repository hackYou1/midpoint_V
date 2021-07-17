/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Converts record ({@link Containerable}) to a semi-formatted row
 * ({@link ExportedReportDataRow} - basically, a string representation)
 * according to individual columns specifications.
 *
 * Responsibilities:
 *
 * 1. resolves paths, including following the references (if any),
 * 2. evaluates expressions,
 * 3. formats (pretty-prints) the values.
 *
 * Instantiated for each individual record.
 *
 * TODO better name
 */
class ColumnDataConverter<C extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(ColumnDataConverter.class);

    @NotNull private final C record;
    @NotNull private final ReportType report;
    @NotNull private final VariablesMap parameters;
    @NotNull private final ReportServiceImpl reportService;
    @NotNull private final RunningTask task;
    @NotNull private final OperationResult result;

    ColumnDataConverter(@NotNull C record, @NotNull ReportType report, @NotNull VariablesMap parameters,
            @NotNull ReportServiceImpl reportService, @NotNull RunningTask task, @NotNull OperationResult result) {
        this.record = record;
        this.report = report;
        this.parameters = parameters;
        this.reportService = reportService;
        this.task = task;
        this.result = result;
    }

    List<String> convertColumn(@NotNull GuiObjectColumnType column) {

        ItemPath itemPath = column.getPath() == null ? null : column.getPath().getItemPath();
        ExpressionType expression = column.getExport() != null ? column.getExport().getExpression() : null;

        argCheck(itemPath != null || expression != null,
                "Neither path nor expression for column %s is specified", column.getName());

        Collection<? extends PrismValue> dataValues;
        if (itemPath != null && !DefaultColumnUtils.isSpecialColumn(itemPath, record)) {
            dataValues = resolvePath(itemPath);
        } else {
            dataValues = List.of(record.asPrismContainerValue());
        }

        if (expression != null) {
            dataValues = evaluateExportExpressionOverPrismValues(expression, dataValues);
        }

        if (DisplayValueType.NUMBER.equals(column.getDisplayValue())) {
            return List.of(String.valueOf(dataValues.size()));
        }
        if (DefaultColumnUtils.isSpecialColumn(itemPath, record)) {
            return MiscUtil.singletonOrEmptyList(
                    DefaultColumnUtils.processSpecialColumn(itemPath, record, reportService.getLocalizationService()));
        }
        return prettyPrintValues(dataValues);
    }

    /**
     * Resolves the path for the record.
     *
     * @return List of values of the item found. (Or empty list of nothing was found.)
     */
    private @NotNull List<? extends PrismValue> resolvePath(ItemPath itemPath) {
        Item<?, ?> currentItem = null;
        Iterator<?> iterator = itemPath.getSegments().iterator();
        while (iterator.hasNext()) {
            ItemName name = ItemPath.toNameOrNull(iterator.next());
            if (name == null) {
                continue;
            }
            if (currentItem == null) {
                currentItem = record.asPrismContainerValue().findItem(name);
            } else {
                currentItem = (Item<?, ?>) currentItem.find(name);
            }
            if (currentItem == null) {
                break;
            }
            if (currentItem instanceof PrismProperty) {
                stateCheck(!iterator.hasNext(), "Cannot continue resolving path in prism property: %s", currentItem);
            } else if (currentItem instanceof PrismReference) {
                if (currentItem.isSingleValue()) {
                    Referencable ref = ((PrismReference) currentItem).getRealValue();
                    if (ref != null && iterator.hasNext()) {
                        currentItem = reportService.getObjectFromReference(ref, task, result);
                    }
                } else {
                    stateCheck(!iterator.hasNext(), "Cannot continue resolving path in multivalued reference: %s", currentItem);
                }
            }
        }
        return currentItem != null ? currentItem.getValues() : List.of();
    }

    private List<String> prettyPrintValues(Collection<? extends PrismValue> values) {
        return values.stream()
                .map(this::prettyPrintValue)
                .collect(Collectors.toList());
    }

    private String prettyPrintValue(PrismValue value) {
        if (value instanceof PrismPropertyValue) {
            Object realValue = ((PrismPropertyValue<?>) value).getRealValue();
            if (realValue == null) {
                return "";
            } else if (realValue instanceof Collection) {
                throw new IllegalStateException("Collection in a prism property? " + value);
            } else if (realValue instanceof Enum) {
                return ReportUtils.prettyPrintForReport((Enum<?>) realValue);
            } else if (realValue instanceof XMLGregorianCalendar) {
                return ReportUtils.prettyPrintForReport((XMLGregorianCalendar) realValue);
            } else if (realValue instanceof ObjectDeltaOperationType) {
                try {
                    return ReportUtils.printDelta(
                            DeltaConvertor.createObjectDeltaOperation((ObjectDeltaOperationType) realValue, PrismContext.get()));
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't convert delta from ObjectDeltaOperationType to ObjectDeltaOperation {}", realValue);
                    return "";
                }
            } else {
                return ReportUtils.prettyPrintForReport(realValue);
            }
        } else if (value instanceof PrismReferenceValue) {
            return getObjectNameFromRef(value.getRealValue());
        } else {
            return ReportUtils.prettyPrintForReport(value);
        }
    }

    private String getObjectNameFromRef(Referencable ref) {
        if (ref == null) {
            return "";
        }
        if (ref.getTargetName() != null && ref.getTargetName().getOrig() != null) {
            return ref.getTargetName().getOrig();
        }
        PrismObject<?> object = reportService.getObjectFromReference(ref, task, result);

        if (object == null) {
            return ref.getOid();
        }

        if (object.getName() == null || object.getName().getOrig() == null) {
            return "";
        }

        return object.getName().getOrig();
    }

    private Collection<? extends PrismValue> evaluateExportExpressionOverPrismValues(@NotNull ExpressionType expression,
            @NotNull Collection<? extends PrismValue> prismValues) {
        Object input;
        if (prismValues.isEmpty()) {
            input = null;
        } else if (prismValues.size() == 1) {
            input = prismValues.iterator().next().getRealValue();
        } else {
            input = prismValues.stream()
                    .filter(Objects::nonNull)
                    .map(PrismValue::getRealValue)
                    .collect(Collectors.toList());
        }
        return evaluateExportExpressionOverRealValues(expression, input);
    }

    private Collection<? extends PrismValue> evaluateExportExpressionOverRealValues(ExpressionType expression, Object input) {
        VariablesMap variables = new VariablesMap();
        variables.putAll(parameters);
        variables.put(ExpressionConstants.VAR_OBJECT, record, record.getClass()); // TODO or do we need the real definition here?
        if (input == null) { // TODO or should we put 'input' here only under specific conditions as it was originally?
            variables.put(ExpressionConstants.VAR_INPUT, null, Object.class);
        } else {
            variables.put(ExpressionConstants.VAR_INPUT, input, input.getClass());
        }
        try {
            return reportService.evaluateToCollection(report.asPrismObject(), expression, variables,
                    "value for column (export)", task, result);
        } catch (Exception e) {
            LOGGER.error("Couldn't execute expression " + expression, e);
            return List.of();
        }
    }
}
