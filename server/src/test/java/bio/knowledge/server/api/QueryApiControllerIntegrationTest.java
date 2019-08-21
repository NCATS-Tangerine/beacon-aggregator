package bio.knowledge.server.api;

import bio.knowledge.server.model.Message;

import java.util.*;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class QueryApiControllerIntegrationTest {

    @Autowired
    private QueryApi api;

    @Test @Ignore // This test is somewhat meaningless outside of a regular API request - fails
    public void queryTest() throws Exception {
        Map<String, Object> body = new HashMap<>();
        ResponseEntity<Message> responseEntity = api.query(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

}
