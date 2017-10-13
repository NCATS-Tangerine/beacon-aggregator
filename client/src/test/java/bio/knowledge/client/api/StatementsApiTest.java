/*
 * Translator Knowledge Beacon API
 * This is the Translator Knowledge Beacon web service application programming interface (API).  This OpenAPI (\"Swagger\") document may be used as the input specification into a tool like [Swagger-Codegen](https://github.com/swagger-api/swagger-codegen/blob/master/README.md) to generate client and server code stubs implementing the API, in any one of several supported computer languages and frameworks. In order to customize usage to your web site, you should change the 'host' directive below to your hostname. 
 *
 * OpenAPI spec version: 1.0.11
 * Contact: richard@starinformatics.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package bio.knowledge.client.api;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import bio.knowledge.client.ApiException;
import bio.knowledge.client.model.BeaconStatement;

/**
 * API tests for StatementsApi
 */
@Ignore
public class StatementsApiTest {

    private final StatementsApi api = new StatementsApi();

    
    /**
     * 
     *
     * Given a list of [CURIE-encoded](https://www.w3.org/TR/curie/) identifiers of exactly matching concepts, retrieves a paged list of concept-relations where either the subject or object concept matches at least one concept in the input list 
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void getStatementsTest() throws ApiException {
        List<String> c = null;
        Integer pageNumber = null;
        Integer pageSize = null;
        String keywords = null;
        String semgroups = null;
        String relations = null;
        List<BeaconStatement> response = api.getStatements(c, pageNumber, pageSize, keywords, semgroups, relations);
        // TODO: test validations
    }
    
}
