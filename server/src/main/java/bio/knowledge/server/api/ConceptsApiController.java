package bio.knowledge.server.api;

import bio.knowledge.server.model.ServerConcept;
import bio.knowledge.server.model.ServerConceptWithDetails;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-12-05T23:10:08.342-08:00")

@Controller
public class ConceptsApiController implements ConceptsApi {



    public ResponseEntity<List<ServerConceptWithDetails>> getConceptDetails(@ApiParam(value = "a [CURIE-encoded](https://www.w3.org/TR/curie/) identifier, as returned  by any other endpoint of the beacon aggregator API, of an exactly matching  concept clique of interest.",required=true ) @PathVariable("cliqueId") String cliqueId,
         @ApiParam(value = "set of aggregator indices of beacons to be used as knowledge sources for the query ") @RequestParam(value = "beacons", required = false) List<String> beacons,
         @ApiParam(value = "client-defined session identifier ") @RequestParam(value = "sessionId", required = false) String sessionId) {
        // do some magic!
        return new ResponseEntity<List<ServerConceptWithDetails>>(HttpStatus.OK);
    }

    public ResponseEntity<List<ServerConcept>> getConcepts( @NotNull @ApiParam(value = "a (urlencoded) space delimited set of keywords or substrings against which to match concept names and synonyms, e.g. diabetes.", required = true) @RequestParam(value = "keywords", required = true) String keywords,
         @ApiParam(value = "a (url-encoded) space-delimited set of semantic groups (specified as codes CHEM, GENE, ANAT, etc.) to which to constrain concepts matched by the main keyword search (see [semantic groups](https://metamap.nlm.nih.gov/Docs/SemGroups_2013.txt) for the full list of codes) ") @RequestParam(value = "semanticGroups", required = false) String semanticGroups,
         @ApiParam(value = "(1-based) number of the page to be returned in a paged set of query results ") @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
         @ApiParam(value = "number of concepts per page to be returned in a paged set of query results ") @RequestParam(value = "pageSize", required = false) Integer pageSize,
         @ApiParam(value = "set of aggregator indices of beacons to be used as knowledge sources for the query ") @RequestParam(value = "beacons", required = false) List<String> beacons,
         @ApiParam(value = "client-defined session identifier ") @RequestParam(value = "sessionId", required = false) String sessionId) {
        // do some magic!
        return new ResponseEntity<List<ServerConcept>>(HttpStatus.OK);
    }

}
