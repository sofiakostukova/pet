package ru.axiomatika.axilink.exsternalsystems.crm;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ru.axiomatika.axilink.api.exceptions.ExternalSystemArgumentException;
import ru.axiomatika.axilink.api.exceptions.ExternalSystemException;
import ru.axiomatika.axilink.commons.util.JsonXMLConverter;
import ru.axiomatika.axilink.util.CommonTests;
import ru.axiomatika.axilink.util.TestHelper;

import java.io.IOException;

@PrepareForTest(CRMInvoker.class)
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.xml.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "java.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*"})
public class CRMInvokerTest extends CommonTests {

    public CRMInvokerTest() throws Exception {
        super(TestHelper.getFile(CRMInvoker.class, "AventusCRM/success_input.xml"),
                TestHelper.getFile(CRMInvoker.class, "AventusCRM/success_response.json"),
                TestHelper.getFile(CRMInvoker.class, "AventusCRM/success_output.xml"));
    }

    @Override
    public void init() {
        invoker = PowerMockito.spy(new CRMInvoker("url=test;path=test;apiKey=test;id=221"));
        try {
            PowerMockito
                    .doReturn(false)
                    .when(invoker, "isCallParameterMode");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSuccessWork() throws Exception {
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mockResponsePowerMock(successResponse, 200);

        PowerMockito
                .doReturn(mockClient)
                .when(invoker, "getHHTPClient");

        PowerMockito
                .doReturn(mockResponse)
                .when(mockClient).execute(Mockito.any(HttpGet.class), Mockito.any(HttpClientContext.class));

        String result = invoker.invoke(successInput);
        TestHelper.xmlCompare(successOutput, result);
    }

    @Test(expected = ExternalSystemException.class)
    public void testConvertXmlToJson() throws Exception {
        JsonXMLConverter mockConverter = Mockito.mock(JsonXMLConverter.class);
        invoker = PowerMockito.spy(new CRMInvoker("url=test;path=test;apiKey=test;id=221"));
        PowerMockito
                .doReturn(false)
                .when(invoker, "isCallParameterMode");

        PowerMockito
                .doThrow(new IOException("ошибка"))
                .when(mockConverter).convertXMLToJson(Mockito.anyString());

        invoker.invoke("");
    }

    @Test(expected = ExternalSystemException.class)
    public void testNetworkError() throws Exception {
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);

        PowerMockito
                .doReturn(mockClient)
                .when(invoker, "getHHTPClient");

        PowerMockito
                .doThrow(new IOException("ошибка"))
                .when(mockClient).execute(Mockito.any(HttpGet.class), Mockito.any(HttpClientContext.class));

        invoker.invoke("");
    }

    @Test(expected = ExternalSystemException.class)
    public void testStatusCode() throws Exception {
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mockResponsePowerMock("test", 400);

        PowerMockito
                .doReturn(mockClient)
                .when(invoker, "getHHTPClient");

        PowerMockito
                .doReturn(mockResponse)
                .when(mockClient).execute(Mockito.any(HttpGet.class), Mockito.any(HttpClientContext.class));

        invoker.invoke("");
    }

    @Test(expected = ExternalSystemException.class)
    public void testEmptyResponse() throws Exception {
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mockResponsePowerMock("", 200);

        PowerMockito
                .doReturn(mockClient)
                .when(invoker, "getHHTPClient");

        PowerMockito
                .doReturn(mockResponse)
                .when(mockClient).execute(Mockito.any(HttpGet.class), Mockito.any(HttpClientContext.class));

        invoker.invoke("");
    }

//    @Test(expected = ExternalSystemException.class)
    public void testOutputConvertation() throws Exception {
        CloseableHttpClient mockClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mockResponsePowerMock(successResponse, 200);

        PowerMockito
                .doReturn(mockClient)
                .when(invoker, "getHHTPClient");

        PowerMockito
                .doReturn(mockResponse)
                .when(mockClient).execute(Mockito.any(HttpGet.class), Mockito.any(HttpClientContext.class));

        invoker.invoke("");
    }
}
