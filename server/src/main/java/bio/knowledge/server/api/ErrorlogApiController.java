package bio.knowledge.server.api;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import bio.knowledge.server.impl.ControllerImpl;
import bio.knowledge.server.model.ServerLogEntry;
import io.swagger.annotations.ApiParam;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-12-19T18:00:36.924-08:00")

@Controller
public class ErrorlogApiController implements ErrorlogApi {

	@Autowired ControllerImpl ctrl;

    public ResponseEntity<List<ServerLogEntry>> getErrors( @NotNull @ApiParam(value = "client-defined session identifier ", required = true) @RequestParam(value = "sessionId", required = true) String sessionId) {
         return ctrl.getErrors(sessionId);
    }

}
