/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.Collection;
import javax.annotation.PreDestroy;

import com.google.common.base.Strings;
import com.querydsl.core.Tuple;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.repo.sqale.qbean.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.QObject;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryExecutor;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Repository implementation based on SQL, JDBC and Querydsl without any ORM.
 * WORK IN PROGRESS:
 * It will be PostgreSQL only or at least PG optimized with generic SQL support for other unsupported DB.
 * Possible Oracle support is in play.
 */
public class SqaleRepositoryService implements RepositoryService {

    private static final Trace LOGGER = TraceManager.getTrace(SqaleRepositoryService.class);

    private static final String OP_NAME_PREFIX = SqaleRepositoryService.class.getSimpleName() + '.';

    private final SqlRepoContext sqlRepoContext;
    private final SqlQueryExecutor sqlQueryExecutor;
    private final PrismContext prismContext;

    public SqaleRepositoryService(
            SqlRepoContext sqlRepoContext,
            PrismContext prismContext) {
        this.sqlRepoContext = sqlRepoContext;
        this.sqlQueryExecutor = new SqlQueryExecutor(sqlRepoContext, prismContext);
        this.prismContext = prismContext;
    }

    @Override
    public @NotNull <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Getting object '{}' with oid '{}': {}",
                type.getSimpleName(), oid, parentResult.getOperation());
        InternalMonitor.recordRepositoryRead(type, oid);

        OperationResult operationResult = parentResult.subresult(GET_OBJECT)
                .addQualifier(type.getSimpleName())
                .setMinor()
                .addParam("type", type.getName())
                .addParam("oid", oid)
                .build();

        PrismObject<T> object;
        try {
            //noinspection unchecked
            object = (PrismObject<T>) readByOid(type, oid, options).asPrismObject();

            // TODO what's with all the conflict watchers?
            // "objectLocal" is here just to provide effectively final variable for the lambda below
//            PrismObject<T> objectLocal = executeAttempts(oid, OP_GET_OBJECT, type, "getting",
//                    subResult, () -> objectRetriever.getObjectAttempt(type, oid, options, operationResult));
//            object = objectLocal;
//            invokeConflictWatchers((w) -> w.afterGetObject(objectLocal));
        } catch (RuntimeException e) { // TODO what else to catch?
            handleGeneralException(e, operationResult);
            throw new SystemException(e);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
//            OperationLogger.logGetObject(type, oid, options, object, operationResult);
            // TODO some logging
        }

        return object;
    }

    public <S extends ObjectType, Q extends QObject<R>, R extends MObject> S readByOid(
            @NotNull Class<S> schemaType,
            String oid,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {

//        context.processOptions(options); TODO how to process option, is setting of select expressions enough?

        QueryModelMapping<S, Q, R> rootMapping = sqlRepoContext.getMappingBySchemaType(schemaType);
        final Q root = rootMapping.defaultAlias();

        Tuple result;
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            result = sqlRepoContext.newQuery(jdbcSession.connection())
                    .from(root)
                    .select(rootMapping.selectExpressions(root, options))
                    .where(root.oid.eq(oid))
                    .fetchOne();
        }

        return rootMapping.createTransformer(prismContext, sqlRepoContext)
                .toSchemaObject(result, root, options);
    }

    @Override
    public <T extends ObjectType> String getVersion(
            Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        return null;
        // TODO
    }

    // Add/modify/delete

    @Override
    @NotNull
    public <T extends ObjectType> String addObject(
            PrismObject<T> object, RepoAddOptions options, OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException {
        Validate.notNull(object, "Object must not be null.");
        PolyString name = object.getName();
        // TODO investigate how norm works, it should not be stored as null
        if (name == null || Strings.isNullOrEmpty(name.getOrig())) {
            throw new SchemaException("Attempt to add object without name.");
        }
        Validate.notNull(parentResult, "Operation result must not be null.");

        if (options == null) {
            options = new RepoAddOptions();
        }

        LOGGER.debug(
                "Adding object type '{}', overwrite={}, allowUnencryptedValues={}, name={} - {}",
                object.getCompileTimeClass().getSimpleName(), options.isOverwrite(),
                options.isAllowUnencryptedValues(), name.getOrig(), name.getNorm());
//        if (InternalsConfig.encryptionChecks && !RepoAddOptions.isAllowUnencryptedValues(options)) {
//            CryptoUtil.checkEncrypted(object);
//        }

        if (InternalsConfig.consistencyChecks) {
            object.checkConsistence(ConsistencyCheckScope.THOROUGH);
        } else {
            object.checkConsistence(ConsistencyCheckScope.MANDATORY_CHECKS_ONLY);
        }

        OperationResult operationResult = parentResult.subresult(ADD_OBJECT)
                .addQualifier(object.asObjectable().getClass().getSimpleName())
                .addParam("object", object)
                .addParam("options", options.toString())
                .build();

//        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
//        long opHandle = pm.registerOperationStart(OP_ADD_OBJECT, object.getCompileTimeClass());
//        int attempt = 1;
//        int restarts = 0;
//        boolean noFetchExtensionValueInsertionForbidden = false;
        try {
            // TODO use executeAttempts
            final String operation = "adding";

            return null;
            /*
            String proposedOid = object.getOid();
            while (true) {
                try {
                    String createdOid = objectUpdater.addObjectAttempt(object, options, noFetchExtensionValueInsertionForbidden, subResult);
                    invokeConflictWatchers((w) -> w.afterAddObject(createdOid, object));
                    return createdOid;
                } catch (RestartOperationRequestedException ex) {
                    // special case: we want to restart but we do not want to count these
                    LOGGER.trace("Restarting because of {}", ex.getMessage());
                    restarts++;
                    if (restarts > RESTART_LIMIT) {
                        throw new IllegalStateException("Too many operation restarts");
                    }
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(proposedOid, operation, attempt, ex, subResult);
//                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
                noFetchExtensionValueInsertionForbidden = true; // todo This is a temporary measure; needs better handling.
            }
        } finally {
//            pm.registerOperationFinish(opHandle, attempt);
//            OperationLogger.logAdd(object, options, subResult); TODO logging
        }
        */
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    @Override
    public @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            Class<T> type, String oid,
            Collection<? extends ItemDelta<?, ?>> modifications, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        return null;
        // TODO
    }

    @Override
    public @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            Class<T> type, String oid, Collection<? extends ItemDelta<?, ?>> modifications,
            RepoModifyOptions options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        return null;
        // TODO
    }

    @Override
    public @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            ModificationPrecondition<T> precondition,
            RepoModifyOptions options,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {

        return null;
        // TODO
    }

    @Override
    public @NotNull <T extends ObjectType> DeleteObjectResult deleteObject(
            Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException {
        return null;
        // TODO
    }

    // Counting/searching

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        return 0;
        // TODO
    }

    @Override
    public @NotNull <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        // Q{EQUAL: name,PPV(PolyString:DefaultNode),null paging
        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + "searchObjects")
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("query", query)
                .build();

        try {
            SearchResultList<T> result =
                    sqlQueryExecutor.list(type, query, options);
            //noinspection unchecked
            return result.map(
                    o -> (PrismObject<T>) o.asPrismObject());
        } catch (QueryException | RuntimeException e) {
            handleGeneralException(e, operationResult);
            throw new SystemException(e);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    /*
    TODO from ObjectRetriever, how to do this per-object Throwable catch + record result?
     should we smuggle the OperationResult all the way to the transformer call?
    @NotNull
    private <T extends ObjectType> List<PrismObject<T>> queryResultToPrismObjects(
            List<T> objects, Class<T> type,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws SchemaException {
        List<PrismObject<T>> rv = new ArrayList<>();
        if (objects == null) {
            return rv;
        }
        for (T object : objects) {
            String oid = object.getOid();
            Holder<PrismObject<T>> partialValueHolder = new Holder<>();
            PrismObject<T> prismObject;
            try {
                prismObject = createPrismObject(object, type, oid, options, partialValueHolder);
            } catch (Throwable t) {
                if (!partialValueHolder.isEmpty()) {
                    prismObject = partialValueHolder.getValue();
                } else {
                    prismObject = prismContext.createObject(type);
                    prismObject.setOid(oid);
                    prismObject.asObjectable().setName(PolyStringType.fromOrig("Unreadable object"));
                }
                result.recordFatalError("Couldn't retrieve " + type + " " + oid + ": " + t.getMessage(), t);
                prismObject.asObjectable().setFetchResult(result.createOperationResultType());
            }
            rv.add(prismObject);
        }
        return rv;
    }
    */

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
            Class<T> type, ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options, boolean strictlySequential,
            OperationResult parentResult) throws SchemaException {
        return null;
        // TODO
    }

    @Override
    public <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        return 0;
    }

    @Override
    public <T extends Containerable> SearchResultList<T> searchContainers(
            Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        return null;
        // TODO
    }

    @Override
    public boolean isAnySubordinate(String upperOrgOid, Collection<String> lowerObjectOids)
            throws SchemaException {
        return false;
        // TODO
    }

    @Override
    public <O extends ObjectType> boolean isDescendant(PrismObject<O> object, String orgOid)
            throws SchemaException {
        return false;
        // TODO
    }

    @Override
    public <O extends ObjectType> boolean isAncestor(PrismObject<O> object, String oid)
            throws SchemaException {
        return false;
        // TODO
    }

    @Override
    public <F extends FocusType> PrismObject<F> searchShadowOwner(String shadowOid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        return null;
        // TODO
    }

    @Override
    public long advanceSequence(String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        return 0;
        // TODO
    }

    @Override
    public void returnUnusedValuesToSequence(
            String oid, Collection<Long> unusedValues, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        // TODO
    }

    @Override
    public RepositoryDiag getRepositoryDiag() {
        return null;
        // TODO
    }

    @Override
    public void repositorySelfTest(OperationResult parentResult) {

        // TODO
    }

    @Override
    public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult testResult) {

        // TODO
    }

    @Override
    public RepositoryQueryDiagResponse executeQueryDiagnostics(
            RepositoryQueryDiagRequest request, OperationResult result) {
        return null;
        // TODO
    }

    @Override
    public <O extends ObjectType> boolean selectorMatches(
            ObjectSelectorType objectSelector, PrismObject<O> object,
            ObjectFilterExpressionEvaluator filterEvaluator, Trace logger, String logMessagePrefix)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return false;
        // TODO
    }

    @Override
    public void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch) {
        // TODO
    }

    @Override
    public FullTextSearchConfigurationType getFullTextSearchConfiguration() {
        return null;
        // TODO
    }

    @Override
    public void postInit(OperationResult result) throws SchemaException {
        // TODO
    }

    @Override
    public ConflictWatcher createAndRegisterConflictWatcher(@NotNull String oid) {
        return null;
        // TODO
    }

    @Override
    public void unregisterConflictWatcher(ConflictWatcher watcher) {
        // TODO
    }

    @Override
    public boolean hasConflict(ConflictWatcher watcher, OperationResult result) {
        return false;
        // TODO
    }

    @Override
    public <T extends ObjectType> void addDiagnosticInformation(Class<T> type, String oid,
            DiagnosticInformationType information, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        // TODO
    }

    @Override
    public PerformanceMonitor getPerformanceMonitor() {
        return null;
        // TODO
    }

    @PreDestroy
    public void destroy() {
        // TODO current monitoring is repo-sql-impl related, see SqlBaseService#destroy
    }

    /**
     * Handles exception outside of transaction - this does not handle transactional problems.
     */
    private void handleGeneralException(Throwable ex, OperationResult result) {
        LOGGER.error("General checked exception occurred.", ex);
        recordException(ex, result,
                sqlRepoContext.getJdbcRepositoryConfiguration().isFatalException(ex));

        throw ex instanceof SystemException
                ? (SystemException) ex
                : new SystemException(ex.getMessage(), ex);
    }

    private void recordException(@NotNull Throwable ex, OperationResult result, boolean fatal) {
        String message = ex != null && Strings.isNullOrEmpty(ex.getMessage())
                ? ex.getMessage() : "null";
        if (Strings.isNullOrEmpty(message) && ex != null) {
            message = ex.getMessage();
        }

        // non-fatal errors will NOT be put into OperationResult, not to confuse the user
        if (result != null && fatal) {
            result.recordFatalError(message, ex);
        }
    }
}
