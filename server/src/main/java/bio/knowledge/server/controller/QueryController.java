package bio.knowledge.server.controller;

import bio.knowledge.server.api.QueryApiDelegate;
import bio.knowledge.server.blackboard.Blackboard;
import bio.knowledge.server.blackboard.BlackboardException;
import bio.knowledge.server.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QueryController implements QueryApiDelegate {

    private static Logger _logger = LoggerFactory.getLogger(QueryController.class);

    @Autowired private LogService kbaLog;
    @Autowired private Blackboard blackboard;

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @Autowired
    public QueryController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @Override
    public Optional<ObjectMapper> getObjectMapper() {
         return Optional.of(this.objectMapper);
    }

    @Override
    public Optional<HttpServletRequest> getRequest() {
        return Optional.of(this.request);
    }

    @Override
    public ResponseEntity<Message> query(Map<String, Object> body) {
        if(getObjectMapper().isPresent() && getAcceptHeader().isPresent()) {
            if (getAcceptHeader().get().contains("application/json")) {
                try {

                    Message result_message = blackboard.query(body);

                    return new ResponseEntity<>(result_message, HttpStatus.OK);

                } catch (BlackboardException bbe) {

                    kbaLog.logError("global", bbe);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
               }
            }
        } else {
            log.warn("ObjectMapper or HttpServletRequest not configured so no result can be generated");
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    }
}
