package bio.knowledge.server.api;

import bio.knowledge.server.model.ServerLogEntry;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-02-22T17:06:24.263-08:00")

@Controller
public class ErrorlogApiController implements ErrorlogApi {



    public ResponseEntity<List<ServerLogEntry>> getErrors( @NotNull @ApiParam(value = "client-defined session identifier ", required = true) @RequestParam(value = "sessionId", required = true) String sessionId) {
        // do some magic!
        return new ResponseEntity<List<ServerLogEntry>>(HttpStatus.OK);
    }

}
