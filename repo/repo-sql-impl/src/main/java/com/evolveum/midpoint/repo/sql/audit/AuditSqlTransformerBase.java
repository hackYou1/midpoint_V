/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit;

import java.util.Collection;

import com.querydsl.core.Tuple;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public abstract class AuditSqlTransformerBase<S, Q extends FlexibleRelationalPathBase<R>, R>
        implements SqlTransformer<S, Q, R> {

    protected final SqlTransformerContext transformerContext;
    protected final QueryTableMapping<S, Q, R> mapping;

    protected AuditSqlTransformerBase(
            SqlTransformerContext transformerContext, QueryTableMapping<S, Q, R> mapping) {
        this.transformerContext = transformerContext;
        this.mapping = mapping;
    }

    @Override
    public S toSchemaObject(
            Tuple tuple, Q entityPath, Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {
        S schemaObject = toSchemaObject(tuple.get(entityPath));
        processExtensionColumns(schemaObject, tuple, entityPath);
        return schemaObject;
    }

    protected void processExtensionColumns(S schemaObject, Tuple tuple, Q entityPath) {
        // empty by default, can be overridden
    }

    /**
     * Returns {@link ObjectReferenceType} with specified oid, proper type based on
     * {@link RObjectType} and, optionally, description.
     * Returns {@code null} if OID is null.
     * Fails if OID is not null and {@code repoObjectType} is null.
     */
    @Nullable
    protected ObjectReferenceType objectReferenceType(
            @Nullable String oid, RObjectType repoObjectType, String targetName) {
        if (oid == null) {
            return null;
        }
        if (repoObjectType == null) {
            throw new IllegalArgumentException(
                    "NULL object type provided for object reference with OID " + oid);
        }

        return new ObjectReferenceType()
                .oid(oid)
                .type(transformerContext.schemaClassToQName(repoObjectType.getJaxbClass()))
                .description(targetName)
                .targetName(targetName);
    }

    /**
     * Returns {@link RObjectType} from ordinal Integer or specified default value.
     */
    protected @NotNull RObjectType repoObjectType(
            @Nullable Integer repoObjectTypeId, @NotNull RObjectType defaultValue) {
        return repoObjectTypeId != null
                ? RObjectType.fromOrdinal(repoObjectTypeId)
                : defaultValue;
    }

    /**
     * Returns nullable {@link RObjectType} from ordinal Integer.
     * If null is returned it will not fail immediately unlike {@link RObjectType#fromOrdinal(int)}.
     * This is practical for eager argument resolution for
     * {@link #objectReferenceType(String, RObjectType, String)}.
     * Null may still be OK if OID is null as well - which means no reference.
     */
    protected @Nullable RObjectType repoObjectType(
            @Nullable Integer repoObjectTypeId) {
        return repoObjectTypeId != null
                ? RObjectType.fromOrdinal(repoObjectTypeId)
                : null;
    }

    /**
     * Trimming the value to the column size from column metadata (must be specified).
     */
    protected @Nullable String trim(
            @Nullable String value, @NotNull ColumnMetadata columnMetadata) {
        if (!columnMetadata.hasSize()) {
            throw new IllegalArgumentException(
                    "trimString with column metadata without specified size: " + columnMetadata);
        }
        return MiscUtil.trimString(value, columnMetadata.getSize());
    }
}
