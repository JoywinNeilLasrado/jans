package io.jans.configapi.plugin.shibboleth.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import io.jans.configapi.core.model.ApiError;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.core.rest.ProtectedApi;
import io.jans.configapi.plugin.shibboleth.form.TrustRelationshipForm;
import io.jans.configapi.plugin.shibboleth.model.ShibbolethIdpConfiguration;
import io.jans.configapi.plugin.shibboleth.model.Status;
import io.jans.configapi.plugin.shibboleth.model.TrustRelationship;
import io.jans.configapi.plugin.shibboleth.model.TrustedServiceProvider;
import io.jans.configapi.plugin.shibboleth.service.ShibbolethService;
import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.model.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import static io.jans.as.model.util.Util.escapeLog;

import java.io.IOException;
import java.util.*;
import java.util.stream.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.slf4j.Logger;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ShibbolethResource extends BaseResource {

    private static final String SAML_TRUST_RELATIONSHIP_FORM = "Trust Relationship From";
    private static final String SAML_TRUST_RELATIONSHIP = "Trust Relationship";
    private static final String SAML_TRUST_RELATIONSHIP_CHECK_STR = "Trust Relationship identified by '";
    private static final String NAME_CONFLICT = "NAME_CONFLICT";
    private static final String NAME_CONFLICT_MSG = "Trust Relationship with same name `%s` already exists!";
    private static final String DATA_NULL_CHK = "RESOURCE_IS_NULL";
    private static final String DATA_NULL_MSG = "`%s` should not be null!";

    private class TrustRelationshipPagedResult extends PagedResult<TrustRelationship> {
    };

    @Inject
    private Logger logger;

    @Inject
    private ShibbolethService shibbolethService;

    @Operation(summary = "Gets Shibboleth IDP configuration", description = "Gets Shibboleth IDP configuration", operationId = "get-shibboleth-config", tags = {
            "Shibboleth IDP - Config Management" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shibboleth IDP configuration", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ShibbolethIdpConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path("/shibboleth-config")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_ADMIN_ACCESS })
    public Response getConfiguration() {

        ShibbolethIdpConfiguration config = shibbolethService.getConfiguration();
        return Response.ok(config).build();
    }

    @Operation(summary = "Updates Shibboleth IDP configuration", description = "Updates Shibboleth IDP configuration", operationId = "put-shibboleth-config", tags = {
            "Shibboleth IDP - Config Management" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Shibboleth IDP configuration", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ShibbolethIdpConfiguration.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @PUT
    @Path("/shibboleth-config")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_ADMIN_ACCESS })
    public Response updateConfiguration(@Valid @NotNull ShibbolethIdpConfiguration configuration) {
        shibbolethService.updateConfiguration(configuration);
        return Response.ok(configuration).build();
    }

    @Operation(summary = "Gets trusted service providers", description = "Gets list of trusted SAML service providers", operationId = "get-shibboleth-trust", tags = {
            "Shibboleth IDP - Config Management" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationshipPagedResult.class), examples = @ExampleObject(name = "Response json example", value = "example/shibboleth/trust-relationship/get-shibboleth-trust"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path("/trust")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getTrustedServiceProviders(
            @Parameter(description = "Search size - max size of the results to return") @DefaultValue(ApiConstants.DEFAULT_LIST_SIZE) @QueryParam(value = ApiConstants.LIMIT) int limit,
            @Parameter(description = "Search pattern") @DefaultValue("") @QueryParam(value = ApiConstants.PATTERN) String pattern,
            @Parameter(description = "The 1-based index of the first query result") @DefaultValue(ApiConstants.DEFAULT_LIST_START_INDEX) @QueryParam(value = ApiConstants.START_INDEX) int startIndex,
            @Parameter(description = "Attribute whose value will be used to order the returned response") @DefaultValue(ApiConstants.INUM) @QueryParam(value = ApiConstants.SORT_BY) String sortBy,
            @Parameter(description = "Order in which the sortBy param is applied. Allowed values are \"ascending\" and \"descending\"") @DefaultValue(ApiConstants.ASCENDING) @QueryParam(value = ApiConstants.SORT_ORDER) String sortOrder,
            @Parameter(description = "Page number to be retrieved, the number of pages is the total number of records divided by the page size (rounded up)") @DefaultValue(ApiConstants.PAGE_INDEX) @QueryParam(value = ApiConstants.PAGE) int page,
            @Parameter(description = "Field and value pair for searching", examples = @ExampleObject(name = "Field value example", value = "applicationType=web,persistClientAuthorizations=true")) @DefaultValue("") @QueryParam(value = ApiConstants.FIELD_VALUE_PAIR) String fieldValuePair) {
        logger.debug("GET /shibboleth/trust");
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Shibboleth trust search param - limit:{}, pattern:{}, startIndex:{}, sortBy:{}, sortOrder:{}, page:{}, fieldValuePair:{}",
                    escapeLog(limit), escapeLog(pattern), escapeLog(startIndex), escapeLog(sortBy),
                    escapeLog(sortOrder), escapeLog(page), escapeLog(fieldValuePair));
        }
        SearchRequest searchReq = createSearchRequest(shibbolethService.getDnForTrustRelationship(null), pattern,
                sortBy, sortOrder, startIndex, limit, null, null, shibbolethService.getRecordMaxCount(), fieldValuePair,
                TrustRelationship.class);
        searchReq.setPage(page);

        return Response.ok(this.doSearch(searchReq)).build();
    }

    @Operation(summary = "Gets a trusted service provider", description = "Gets a trusted SAML service provider by entity ID", operationId = "get-shibboleth-trust-by-id", tags = {
            "Shibboleth IDP - Config Management" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trusted service provider", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustedServiceProvider.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path("/{entityId}")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getTrustedServiceProvider(
            @Parameter(description = "Entity ID of the service provider") @PathParam("entityId") String entityId) {
        logger.debug("GET /shibboleth/trust/{}", entityId);
        TrustedServiceProvider provider = shibbolethService.getTrustedServiceProvider(entityId);

        if (provider == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(provider).build();
    }

    @Operation(summary = "Adds trusted service provider", description = "Adds a new trusted SAML service provider", operationId = "post-shibboleth-trust", tags = {
            "Shibboleth IDP - Config Management" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @RequestBody(description = "Trust Relationship object", content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = TrustRelationshipForm.class), examples = @ExampleObject(name = "Request example", value = "example/shibboleth/trust-relationship/trust-relationship-post.json")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Newly created Trust Relationship", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustRelationship.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "BadRequestException"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "NotFoundException"))),
            @ApiResponse(responseCode = "500", description = "InternalServerError", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class, description = "InternalServerError"))), })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    @Path("/trust")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response addTrustedServiceProvider(@MultipartForm TrustRelationshipForm trustRelationshipForm,
            InputStream metadatafile) {
        logger.info("POST /shibboleth/trust");

        // validation
        checkResourceNotNull(trustRelationshipForm, SAML_TRUST_RELATIONSHIP_FORM);

        TrustRelationship trustRelationship = trustRelationshipForm.getTrustRelationship();
        // if (serviceProvider.getEntityId() == null) {
        // return Response.status(Response.Status.BAD_REQUEST).entity("Entity ID is
        // required").build();
        // }

        shibbolethService.addTrustRelationship(trustRelationship);
        return Response.status(Response.Status.CREATED).entity(trustRelationship).build();
    }

    private void validateTrustRelationship(TrustRelationship trustRelationship) {
        checkResourceNotNull(trustRelationship, SAML_TRUST_RELATIONSHIP);
        checkNotNull(trustRelationship.getDisplayName(), "Display Name");
        // check if TrustRelationship with same name already exists
        List<TrustRelationship> existingTrustRelationship = shibbolethService
                .getAllTrustRelationshipByName(trustRelationship.getDisplayName());
        logger.debug(" existingTrustRelationship:{} ", existingTrustRelationship);
        if (existingTrustRelationship != null && !existingTrustRelationship.isEmpty()) {
            throwBadRequestException(NAME_CONFLICT,
                    String.format(NAME_CONFLICT_MSG, trustRelationship.getDisplayName()));
        }
    }

    @Operation(summary = "Updates trusted service provider", description = "Updates an existing trusted SAML service provider", operationId = "put-shibboleth-trust", tags = {
            "Shibboleth IDP - Config Management" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated trusted service provider", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TrustedServiceProvider.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @PUT
    @Path("/trust/{entityId}")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }, groupScopes = {}, superScopes = {
            Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response updateTrustedServiceProvider(
            @Parameter(description = "Entity ID of the service provider") @PathParam("entityId") String entityId,
            @Valid @NotNull TrustedServiceProvider serviceProvider) {
        logger.info("PUT /shibboleth/trust/{}", entityId);

        TrustedServiceProvider existing = shibbolethService.getTrustedServiceProvider(entityId);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        serviceProvider.setEntityId(entityId);
        shibbolethService.updateTrustedServiceProvider(serviceProvider);
        return Response.ok(serviceProvider).build();
    }

    @Operation(summary = "Deletes trusted service provider", description = "Deletes a trusted SAML service provider", operationId = "delete-shibboleth-trust", tags = {
            "Shibboleth IDP - Config Management" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_DELETE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @DELETE
    @Path("/trust/{entityId}")
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_DELETE_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response deleteTrustedServiceProvider(
            @Parameter(description = "Entity ID of the service provider") @PathParam("entityId") String entityId) {
        logger.info("DELETE /shibboleth/trust/{}", entityId);

        TrustedServiceProvider existing = shibbolethService.getTrustedServiceProvider(entityId);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        shibbolethService.deleteTrustedServiceProvider(entityId);
        return Response.noContent().build();
    }

    @Operation(summary = "Gets IDP metadata", description = "Gets Shibboleth IDP SAML metadata", operationId = "get-shibboleth-metadata", tags = {
            "Shibboleth IDP - Config Management" }, security = {
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_WRITE_ACCESS }),
                    @SecurityRequirement(name = "oauth2", scopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS }) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "IDP SAML metadata", content = @Content(mediaType = MediaType.APPLICATION_XML)),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "501", description = "Not Implemented"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error") })
    @GET
    @Path("/metadata")
    @Produces(MediaType.APPLICATION_XML)
    @ProtectedApi(scopes = { Constants.SHIBBOLETH_TR_READ_ACCESS }, groupScopes = {
            Constants.SHIBBOLETH_TR_WRITE_ACCESS }, superScopes = { Constants.SHIBBOLETH_TR_ADMIN_ACCESS })
    public Response getIdpMetadata() {
        logger.debug("GET /shibboleth/metadata");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    /**/

    private TrustRelationshipPagedResult doSearch(SearchRequest searchReq) {

        if (logger.isInfoEnabled()) {
            logger.info("TrustRelationship search params - searchReq:{}", escapeLog(searchReq));
        }

        PagedResult<TrustRelationship> pagedResult = shibbolethService.getTrustRelationship(searchReq);
        if (logger.isTraceEnabled()) {
            logger.debug("PagedResult  - pagedResult:{}", pagedResult);
        }

        TrustRelationshipPagedResult pagedTrustRelationship = new TrustRelationshipPagedResult();
        if (pagedResult != null) {
            logger.debug("TrustRelationship fetched  - pagedResult.getEntries():{}", pagedResult.getEntries());
            List<TrustRelationship> trustRelationships = pagedResult.getEntries();

            pagedTrustRelationship.setStart(pagedResult.getStart());
            pagedTrustRelationship.setEntriesCount(pagedResult.getEntriesCount());
            pagedTrustRelationship.setTotalEntriesCount(pagedResult.getTotalEntriesCount());
            pagedTrustRelationship.setEntries(trustRelationships);
        }

        logger.info("TrustRelationship pagedTrustRelationship:{}", pagedTrustRelationship);
        return pagedTrustRelationship;

    }

}
