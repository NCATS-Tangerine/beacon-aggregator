package bio.knowledge.server.api;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import bio.knowledge.server.model.ServerLogEntry;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-12-19T18:00:36.924-08:00")

@Api(value = "errorlog", description = "the errorlog API")
public interface ErrorlogApi {

    @ApiOperation(value = "", notes = "Get a log of the most recent errors in this session ", response = ServerLogEntry.class, responseContainer = "List", tags={ "aggregator", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful response with most recent errors ", response = ServerLogEntry.class) })
    @RequestMapping(value = "/errorlog",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<List<ServerLogEntry>> getErrors( @NotNull @ApiParam(value = "client-defined session identifier ", required = true) @RequestParam(value = "sessionId", required = true) String sessionId);

}
