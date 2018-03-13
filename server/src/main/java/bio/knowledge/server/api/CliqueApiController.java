package bio.knowledge.server.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import bio.knowledge.server.model.ServerCliqueIdentifier;
import io.swagger.annotations.ApiParam;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-03-12T16:26:47.889-07:00")

@Controller
public class CliqueApiController implements CliqueApi {



    public ResponseEntity<ServerCliqueIdentifier> getClique(@ApiParam(value = "a [CURIE-encoded](https://www.w3.org/TR/curie/) identifier of interest to be resolved to a concept clique",required=true ) @PathVariable("identifier") String identifier,
         @ApiParam(value = "client-defined session identifier ") @RequestParam(value = "sessionId", required = false) String sessionId) {
        // do some magic!
        return new ResponseEntity<ServerCliqueIdentifier>(HttpStatus.OK);
    }

}
