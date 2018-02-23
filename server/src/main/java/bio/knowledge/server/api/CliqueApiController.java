package bio.knowledge.server.api;

import bio.knowledge.server.impl.ControllerImpl;
import bio.knowledge.server.model.ServerCliqueIdentifier;

import io.swagger.annotations.*;

import org.springframework.beans.factory.annotation.Autowired;
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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-02-22T15:14:24.514-08:00")

@Controller
public class CliqueApiController implements CliqueApi {

	@Autowired ControllerImpl ctrl;

    public ResponseEntity<ServerCliqueIdentifier> getClique(@ApiParam(value = "a [CURIE-encoded](https://www.w3.org/TR/curie/) identifier of interest to be resolved to a concept clique",required=true ) @PathVariable("identifier") String identifier,
         @ApiParam(value = "client-defined session identifier ") @RequestParam(value = "sessionId", required = false) String sessionId) {
         return ctrl.getClique(identifier, sessionId);
    }

}
