package bio.knowledge.server.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import bio.knowledge.server.model.ServerKnowledgeMap;
import io.swagger.annotations.ApiParam;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-03-12T16:26:47.889-07:00")

@Controller
public class KmapApiController implements KmapApi {



    public ResponseEntity<List<ServerKnowledgeMap>> getKnowledgeMap( @ApiParam(value = "set of aggregator indices of beacons constraining knowledge maps returned  ") @RequestParam(value = "beacons", required = false) List<String> beacons) {
        // do some magic!
        return new ResponseEntity<List<ServerKnowledgeMap>>(HttpStatus.OK);
    }

}
