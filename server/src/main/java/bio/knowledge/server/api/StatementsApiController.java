package bio.knowledge.server.api;

import bio.knowledge.server.model.ServerStatement;

import io.swagger.annotations.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import javax.validation.constraints.*;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-12-19T10:39:33.168-08:00")

@Controller
public class StatementsApiController implements StatementsApi {



    public ResponseEntity<List<ServerStatement>> getStatements( @NotNull @ApiParam(value = "a [CURIE-encoded](https://www.w3.org/TR/curie/) identifier of the  exactly matching 'source' clique, as defined by other endpoints of the beacon aggregator API.  ", required = true) @RequestParam(value = "source", required = true) String source,
         @ApiParam(value = "a (url-encoded, space-delimited) string of predicate relation identifiers with which to constrain the statement relations retrieved  for the given query seed concept. The predicate ids sent should  be as published by the beacon-aggregator by the /predicates API endpoint. ") @RequestParam(value = "relations", required = false) String relations,
         @ApiParam(value = "a [CURIE-encoded](https://www.w3.org/TR/curie/) identifier of the  exactly matching 'target' clique, as defined by other endpoints of the beacon aggregator API.  ") @RequestParam(value = "target", required = false) String target,
         @ApiParam(value = "a (url-encoded, space-delimited) string of keywords or substrings against which to match the 'target' concept or 'predicate' names of the set of concept-relations matched by the 'source' concepts. ") @RequestParam(value = "keywords", required = false) String keywords,
         @ApiParam(value = "a (url-encoded, space-delimited) string of concept semantic types (specified as CURIEs of Translator ontology data type terms) to which to constrain 'target' concepts associated with the given 'source' concept. ") @RequestParam(value = "types", required = false) String types,
         @ApiParam(value = "(1-based) number of the page to be returned in a paged set of query results ") @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
         @ApiParam(value = "number of concepts per page to be returned in a paged set of query results ") @RequestParam(value = "pageSize", required = false) Integer pageSize,
         @ApiParam(value = "set of aggregator indices of beacons to be used as knowledge sources for the query ") @RequestParam(value = "beacons", required = false) List<String> beacons,
         @ApiParam(value = "client-defined session identifier ") @RequestParam(value = "sessionId", required = false) String sessionId) {
        // do some magic!
        return new ResponseEntity<List<ServerStatement>>(HttpStatus.OK);
    }

}
