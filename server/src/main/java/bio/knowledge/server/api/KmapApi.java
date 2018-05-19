package bio.knowledge.server.api;

import bio.knowledge.server.model.ServerKnowledgeMap;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-05-19T15:02:51.082-07:00")

@Api(value = "kmap", description = "the kmap API")
public interface KmapApi {

    @ApiOperation(value = "", notes = "Get a high level knowledge map of the all the beacons specified by triplets of subject concept category, relationship predicate and concept object category ", response = ServerKnowledgeMap.class, responseContainer = "List", tags={ "metadata", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful response with categories and frequency returned ", response = ServerKnowledgeMap.class) })
    @RequestMapping(value = "/kmap",
        produces = { "application/json" }, 
        method = RequestMethod.GET)
    ResponseEntity<List<ServerKnowledgeMap>> getKnowledgeMap( @ApiParam(value = "set of aggregator indices of beacons constraining knowledge maps returned  ") @RequestParam(value = "beacons", required = false) List<Integer> beacons);

}
