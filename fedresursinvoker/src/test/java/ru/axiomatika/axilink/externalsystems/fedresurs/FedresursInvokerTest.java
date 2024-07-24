package ru.axiomatika.axilink.externalsystems.fedresurs;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ru.axiomatika.axilink.api.exceptions.ExternalSystemException;
import ru.axiomatika.axilink.api.model.InvokerResponse;
import ru.axiomatika.axilink.util.CommonTestV2;

import java.net.URI;

@PowerMockIgnore({"javax.management.*", "jdk.xml.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "java.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*"})
@PrepareForTest(FedresursInvoker.class)
@RunWith(PowerMockRunner.class)
public class FedresursInvokerTest extends CommonTestV2 {
    public FedresursInvokerTest() throws Exception {
        super("fedresurs");
    }

    private FedresursInvoker invoker;
    private String searchUrl = "search_url", cardUrl = "card_url";

    private Answer<CloseableHttpResponse> makeDefaultAnswer(
            String url, CloseableHttpResponse successResponse, CloseableHttpResponse invalidResponse
    ) {
        return invocation -> {
            Object[] args = invocation.getArguments();
            HttpGet httpGet = (HttpGet) args[0];
            if (httpGet.getURI().getAuthority().equals(url)) return successResponse;
            else return invalidResponse;
        };
    }

    @Before
    public void init() throws Exception {
        invoker = PowerMockito.spy(new FedresursInvoker("url=https://" + searchUrl + ";card_url=https://" + cardUrl));
        PowerMockito
                .doReturn(false)
                .when(invoker, "isCallParameterMode");
    }

    /**
     * Тест кейс успешной работы инвокера
     * <p>
     * Файлы:
     * success_input.xml
     * success_search_response.json
     * success_card_response.json
     * success_output.xml
     */
    @Test
    public void successWorkTest() throws Exception {
        String successInput = getTestFileAsString("success_input.xml");
        String successSearchResponse = getTestFileAsString("success_search_response.json");
        String successCardResponse = getTestFileAsString("success_card_response.json");
        String successOutput = getTestFileAsString("success_output.xml");

        CloseableHttpClient client = mockClient(invoker);
        CloseableHttpResponse searchResponse = mockResponsePowerMock(successSearchResponse, 200);
        CloseableHttpResponse cardResponse = mockResponsePowerMock(successCardResponse, 200);
        CloseableHttpResponse invalidResponse = mockResponsePowerMock("", 400);

        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpClientContext.class)))
                .thenAnswer(makeDefaultAnswer(searchUrl, searchResponse, invalidResponse));

        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class)))
                .thenAnswer(makeDefaultAnswer(cardUrl, cardResponse, invalidResponse));

        InvokerResponse output = invoker.invokeWithObjectResponse(successInput);
    }

    /**
     * Тест кейс пустого ответа от сервера
     * <p>
     * файлы:
     * success_input.xml
     *
     * @throws Exception
     */
    @Test(expected = ExternalSystemException.class)
    public void emptyResponseTest() throws Exception {
        String successInput = getTestFileAsString("success_input.xml");

        CloseableHttpClient client = mockClient(invoker);
        CloseableHttpResponse searchResponse = mockResponsePowerMock("", 500);
        CloseableHttpResponse invalidResponse = mockResponsePowerMock("", 400);

        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpClientContext.class)))
                .thenAnswer(makeDefaultAnswer(searchUrl, searchResponse, invalidResponse));

        invoker.invokeWithObjectResponse(successInput);
    }

    /**
     * Тест кейс пустых входных параметров
     *
     * @throws Exception
     */
    @Test(expected = ExternalSystemException.class)
    public void errorEmptyInput() throws Exception {
        invoker.invokeWithObjectResponse("");
    }

    /**
     * Тест кейс при котором не указано только отчество
     * <p>
     * Файлы:
     * success_input.xml
     * success_search_response.json
     * success_card_response.json
     * success_output.xml
     */
    @Test
    public void successInputWithoutPatronymic() throws Exception {

        String successInput = getTestFileAsString("success_input.xml");
        String successSearchResponse = getTestFileAsString("success_search_response.json");
        String successCardResponse = getTestFileAsString("success_card_response.json");
        String successOutput = getTestFileAsString("success_output.xml");

        CloseableHttpClient client = mockClient(invoker);
        CloseableHttpResponse searchResponse = mockResponsePowerMock(successSearchResponse, 200);
        CloseableHttpResponse cardResponse = mockResponsePowerMock(successCardResponse, 200);
        CloseableHttpResponse invalidResponse = mockResponsePowerMock("", 400);

        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpClientContext.class)))
                .thenAnswer(makeDefaultAnswer(searchUrl, searchResponse, invalidResponse));

        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class)))
                .thenAnswer(makeDefaultAnswer(cardUrl, cardResponse, invalidResponse));

        InvokerResponse output = invoker.invokeWithObjectResponse(successInput);
    }

    /**
     * Тест кейс при котором не указаны дата рождения и отчество
     * <p>
     * файлы:
     * invalid_input.xml
     *
     * @throws Exception
     */
    @Test(expected = ExternalSystemException.class)
    public void errorInvalidInput() throws Exception {

        String invalidInput = getTestFileAsString("invalid_input.xml");
        invoker.invokeWithObjectResponse(invalidInput);
    }

    /**
     * Тест кейс ошибки конвертации ответа от сервиса в json
     * <p>
     * файлы:
     * success_input.xml
     *
     * @throws Exception
     */
    @Test(expected = ExternalSystemException.class)
    public void corruptGetResponseTest() throws Exception {
        String successInput = getTestFileAsString("success_input.xml");

        CloseableHttpClient client = mockClient(invoker);
        CloseableHttpResponse searchResponse = mockResponsePowerMock("invalid", 200);
        CloseableHttpResponse invalidResponse = mockResponsePowerMock("", 400);

        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpClientContext.class)))
                .thenAnswer(makeDefaultAnswer(searchUrl, searchResponse, invalidResponse));

        invoker.invokeWithObjectResponse(successInput);
    }

    /**
     * Тест кейс успешной работы при получении информации хотя бы об одной карточке
     * <p>
     * файлы:
     * success_input.xml
     * success_search_array_response.json
     * success_card_response.json
     */
    @Test
    public void fewWrongCardResponsesTest() throws Exception {
        String successInput = getTestFileAsString("success_input.xml");
        String successSearchArrayResponse = getTestFileAsString("success_search_array_response.json");
        String successCardResponse = getTestFileAsString("success_card_response.json");
        CloseableHttpClient client = mockClient(invoker);
        CloseableHttpResponse searchResponse = mockResponsePowerMock(successSearchArrayResponse, 200);
        CloseableHttpResponse cardResponse = mockResponsePowerMock(successCardResponse, 200);
        CloseableHttpResponse invalidResponse = mockResponsePowerMock("", 400);
        String cardPath = "/477da63f-8cc3-42b2-a8b1-c98301f345b8";

        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpClientContext.class)))
                .thenAnswer(makeDefaultAnswer(searchUrl, searchResponse, invalidResponse));

        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class))).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            HttpGet httpGet = (HttpGet) args[0];
            URI cardUri = httpGet.getURI();
            if (cardUri.getAuthority().equals(cardUrl) && cardUri.getPath().equals(cardPath)) return cardResponse;
            else return invalidResponse;
        });

        invoker.invokeWithObjectResponse(successInput);
    }

    /**
     * Тест кейс, при котором не нашлось совпадений
     * <p>
     * файлы:
     * success_input_with_another_birthdate.xml
     * success_search_array_response.json
     * success_card_response.json
     */
    @Test
    public void emptyResultTest() throws Exception {
        String successInput = getTestFileAsString("success_input_with_another_birthdate.xml");
        String successSearchArrayResponse = getTestFileAsString("success_search_array_response.json");
        String successCardResponse = getTestFileAsString("success_card_response.json");

        CloseableHttpClient client = mockClient(invoker);
        CloseableHttpResponse searchResponse = mockResponsePowerMock(successSearchArrayResponse, 200);
        CloseableHttpResponse cardResponse = mockResponsePowerMock(successCardResponse, 200);
        CloseableHttpResponse invalidResponse = mockResponsePowerMock("", 400);

        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpClientContext.class)))
                .thenAnswer(makeDefaultAnswer(searchUrl, searchResponse, invalidResponse));

        Mockito.when(client.execute(Mockito.any(HttpUriRequest.class)))
                .thenAnswer(makeDefaultAnswer(cardUrl, cardResponse, invalidResponse));

        InvokerResponse result = invoker.invokeWithObjectResponse(successInput);
        Assertions.assertTrue(result.dataNotFoundFlag);
    }
}
