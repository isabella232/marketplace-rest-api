/* Copyright (c) 2019 Eclipse Foundation and others.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-v20.html,
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipsefoundation.marketplace.resource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipsefoundation.marketplace.dao.MongoDao;
import org.eclipsefoundation.marketplace.dto.ErrorReport;
import org.eclipsefoundation.marketplace.dto.filter.DtoFilter;
import org.eclipsefoundation.marketplace.helper.ResponseHelper;
import org.eclipsefoundation.marketplace.helper.StreamHelper;
import org.eclipsefoundation.marketplace.model.MongoQuery;
import org.eclipsefoundation.marketplace.model.RequestWrapper;
import org.eclipsefoundation.marketplace.namespace.UrlParameterNames;
import org.eclipsefoundation.marketplace.service.CachingService;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource for retrieving {@linkplain ErrorReport} from the MongoDB instance.
 * 
 * @author Martin Lowe
 */
@RequestScoped
@Path("/error_reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ErrorReportResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorReportResource.class);

	@Inject
	MongoDao dao;
	@Inject
	CachingService<List<ErrorReport>> cachingService;
	@Inject
	RequestWrapper params;
	@Inject
	DtoFilter<ErrorReport> dtoFilter;
	@Inject
	ResponseHelper responseBuider;

	/**
	 * Endpoint for /error/ to retrieve all ErrorReports from the database along with
	 * the given query string parameters.
	 * 
	 * @param ErrorReportId int version of the ErrorReport ID
	 * @return response for the browser
	 */
	@GET
	@PermitAll
	public Response select() {
		MongoQuery<ErrorReport> q = new MongoQuery<>(params, dtoFilter);
		// retrieve the possible cached object
		Optional<List<ErrorReport>> cachedResults = cachingService.get("all", params,
				null, () -> StreamHelper.awaitCompletionStage(dao.get(q)));
		if (!cachedResults.isPresent()) {
			LOGGER.error("Error while retrieving cached ErrorReports");
			return Response.serverError().build();
		}

		// return the results as a response
		return responseBuider.build("all", params, cachedResults.get());
	}

	/**
	 * Endpoint for /error/ to post a new ErrorReport to the persistence layer.
	 * 
	 * @param errorReport the ErrorReport object to insert into the database.
	 * @return response for the browser
	 */
	@POST
	@PermitAll
	public Response putErrorReport(ErrorReport errorReport) {
		// attach ID if present for update
		if (errorReport.getId() != null) {
			params.addParam(UrlParameterNames.ID.getParameterName(), errorReport.getId());
		}
		MongoQuery<ErrorReport> q = new MongoQuery<>(params, dtoFilter);

		// add the object, and await the result
		StreamHelper.awaitCompletionStage(dao.add(q, Arrays.asList(errorReport)));

		// return the results as a response
		return Response.ok().build();
	}

	/**
	 * Endpoint for /error/\<errorReportId\> to retrieve a specific ErrorReport from the
	 * database.
	 * 
	 * @param errorReportId the ErrorReport ID
	 * @return response for the browser
	 */
	@GET
	@PermitAll
	@Path("/{errorReportId}")
	public Response select(@PathParam("errorReportId") String errorReportId) {
		params.addParam(UrlParameterNames.ID.getParameterName(), errorReportId);

		MongoQuery<ErrorReport> q = new MongoQuery<>(params, dtoFilter);
		// retrieve a cached version of the value for the current ErrorReport
		Optional<List<ErrorReport>> cachedResults = cachingService.get(errorReportId, params,
				null, () -> StreamHelper.awaitCompletionStage(dao.get(q)));
		if (!cachedResults.isPresent()) {
			LOGGER.error("Error while retrieving cached ErrorReport for ID {}", errorReportId);
			return Response.serverError().build();
		}

		// return the results as a response
		return responseBuider.build(errorReportId, params, cachedResults.get());
	}
}
