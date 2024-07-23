package ru.axiomatika.axilink.exsternalsystems.blacklist;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ru.axiomatika.axilink.api.exceptions.ExternalSystemException;
import ru.axiomatika.axilink.util.CommonTestV2;
import ru.axiomatika.axilink.util.TestHelper;

@PowerMockIgnore({"javax.management.*", "jdk.xml.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "java.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*"})
@PrepareForTest(UserInfoBlacklistInvoker.class)
@RunWith(PowerMockRunner.class)
public class UserInfoBlacklistInvokerTest extends CommonTestV2 {


    public UserInfoBlacklistInvokerTest() throws Exception {
        super("blacklist");
    }

    private UserInfoBlacklistInvoker invoker;


    @Before
    public void init() throws Exception {
        invoker = PowerMockito.spy(new UserInfoBlacklistInvoker("url=https://url;partner_key="));
        PowerMockito
                .doReturn(false)
                .when(invoker, "isCallParameterMode");
    }

    /**
     * Тест кейс успешной работы инвокера
     * <p>
     * Файлы:
     * success_input.xml
     * success_response.json
     * success_output.xml
     */
    @Test
    public void successWorkTest() throws Exception {
        String successInput = getTestFileAsString("success_input.xml");
        String successResponse = getTestFileAsString("success_response.json");
        String successOutput = getTestFileAsString("success_output.xml");
        CloseableHttpClient client = mockClient(invoker);
        CloseableHttpResponse response = mockResponsePowerMock(successResponse, 200);
        Mockito
                .when(client.execute(Mockito.any()))
                .thenReturn(response);

        String output = invoker.invoke(successInput);
    }

    /**
     * Тест кейс пустого ответа от сервиса
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
        CloseableHttpResponse response = mockResponsePowerMock("", 500);
        Mockito
                .when(client.execute(Mockito.any()))
                .thenReturn(response);
        invoker.invoke(successInput);
    }

    /**
     * Тест кейс пустых входных параметров
     *
     * @throws Exception
     */
    @Test(expected = ExternalSystemException.class)
    public void errorEmptyInput() throws Exception {
        invoker.invoke("");
    }

    /**
     * Тест кейс ответа с кодом 404 (данные по клиенту не найдены либо такой клиент есть, но без fdp15_flag и fraud_flag)
     * без поля message
     * файлы:
     * success_input.xml
     * unsuccess_response_no_client.json
     */
    @Test
    public void unsuccessResponseNoClient() throws Exception {
        String successInput = getTestFileAsString("success_input.xml");
        String errorResponse = getTestFileAsString("unsuccess_response_no_client.json");

        CloseableHttpClient client = mockClient(invoker);
        CloseableHttpResponse response = mockResponsePowerMock(errorResponse, 404);
        Mockito
                .when(client.execute(Mockito.any()))
                .thenReturn(response);

        invoker.invoke(successInput);
    }

    /**
     * Тест кейс ответа с кодом 400 (ошибка входных данных)
     * без поля message
     * файлы:
     * success_input.xml
     * unsuccess_response_input_data_error.json
     */
    @Test(expected = ExternalSystemException.class)
    public void unsuccessResponseInputDataError() throws Exception {
        String successInput = getTestFileAsString("success_input.xml");
        String errorResponse = getTestFileAsString("unsuccess_response_input_data_error.json");

        CloseableHttpClient client = mockClient(invoker);
        CloseableHttpResponse response = mockResponsePowerMock(errorResponse, 400);
        Mockito
                .when(client.execute(Mockito.any()))
                .thenReturn(response);

        invoker.invoke(successInput);

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
    public void corruptPostResponseTest() throws Exception {
        String successInput = getTestFileAsString("success_input.xml");

        CloseableHttpClient client = mockClient(invoker);
        CloseableHttpResponse response = mockResponsePowerMock("dcfsdcasdcua", 200);
        Mockito
                .when(client.execute(Mockito.any()))
                .thenReturn(response);

        invoker.invoke(successInput);
    }
}
