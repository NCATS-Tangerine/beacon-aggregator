package bio.knowledge.server.api;

import bio.knowledge.server.controller.ControllerImpl;
import bio.knowledge.server.model.ServerConceptCategory;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-08-28T14:42:53.737-07:00")

@Controller
public class CategoriesApiController implements CategoriesApi {

	@Autowired ControllerImpl ctrl;

    public ResponseEntity<List<ServerConceptCategory>> getConceptCategories( @ApiParam(value = "set of aggregator indices of beacons to constrain categories returned ") @RequestParam(value = "beacons", required = false) List<Integer> beacons) {
    	return ctrl.getConceptCategories(beacons);
    }

}
