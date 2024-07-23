package ru.axiomatika.axilink.exsternalsystems.surelead;

import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ru.axiomatika.axilink.api.exceptions.ExternalSystemArgumentException;
import ru.axiomatika.axilink.api.exceptions.ExternalSystemException;
import ru.axiomatika.axilink.util.TestHelper;

import java.nio.charset.Charset;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "jdk.xml.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "java.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*"})
@PrepareForTest(SureleadInvoker.class)
public class SureleadInvokerTest {
    private SureleadInvoker invoker;

    @Before
    public void init() {
       invoker = PowerMockito.spy(new SureleadInvoker("url=https://google;token=123;"));
        try {
            PowerMockito.doReturn(false)
                    .when(invoker, "isCallParameterMode");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * Тест-кейс успешной работы сервиса
     * Принимается валидный input
     * Ожидается корректный ответ
     *
     * Файлы:
     * surelead/success_input.txt
     * surelead/success_response.json
     * surelead/success_output.xml
     */
    @Test
    public void testSuccessWork() throws Exception {
        final String successInput = TestHelper.getFile(SureleadInvoker.class, "surelead/success_input.txt");
        final String successResponse = TestHelper.getFile(SureleadInvoker.class, "surelead/success_response.json");
        final String successOutput = TestHelper.getFile(SureleadInvoker.class, "surelead/success_output.xml");

        StringEntity stringEntity = new StringEntity(successResponse, Charset.forName("UTF-8"));
        PowerMockito
                .doReturn(stringEntity)
                .when(invoker, "requestRawApi", Mockito.anyList(), Mockito.any());

        final String result = invoker.invoke(successInput);

        TestHelper.xmlCompare(result, successOutput);
    }

    /**
     * Тест-кейс ошибки входных данных
     * Принимается пустой input
     * Ожидается ошибка
     */
    @Test(expected = ExternalSystemArgumentException.class)
    public void testEmptyInput() throws Exception {

        PowerMockito
                .doReturn(null)
                .when(invoker, "requestRawApi", Mockito.anyList(), Mockito.any());

        invoker.invoke("");
    }

    /**
     * Тест-кейс ошибки входных данных: невалидный номер телефона (10 цифр)
     * Принимается невалидный input
     * Ожидается ошибка
     *
     * Файлы:
     * surelead/invalid_input.txt
     */
    @Test(expected = ExternalSystemArgumentException.class)
    public void testInvalidPhoneNumber() throws Exception {
        final String invalidInput = TestHelper.getFile(SureleadInvoker.class, "surelead/invalid_input.txt");

        PowerMockito
                .doReturn(null)
                .when(invoker, "requestRawApi", Mockito.anyList(), Mockito.any());

        invoker.invoke(invalidInput);
    }

    /**
     * Тест-кейс проверки параметров в <connectionString>: отсутствует параметр token
     * Принимается валидный input
     * Ожидается ошибка
     *
     * Файлы:
     * surelead/success_input.txt
     */
    @Test(expected = ExternalSystemArgumentException.class)
    public void testConnectionString() throws Exception{
        invoker = PowerMockito.spy(new SureleadInvoker("url=https://google;token=;"));
        try {
            PowerMockito.doReturn(false)
                    .when(invoker, "isCallParameterMode");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            exception.printStackTrace();
        }

        final String successInput = TestHelper.getFile(SureleadInvoker.class, "surelead/success_input.txt");

        PowerMockito
                .doReturn(null)
                .when(invoker, "requestRawApi", Mockito.anyList(), Mockito.any());

        invoker.invoke(successInput);
    }

    /**
     * Тест-кейс проверки наличия параметра phone_number
     * Принимается невалидный input, в котором не заполнен phone_number
     * Ожидается ошибка
     *
     * Файлы:
     * surelead/input_without_phone_number.txt
     */
    @Test(expected = ExternalSystemArgumentException.class)
    public void testInputWithoutPhoneNumber() throws Exception{
        final String incorrectInput = TestHelper.getFile(SureleadInvoker.class, "surelead/input_without_phone_number.txt");

        PowerMockito
                .doReturn(null)
                .when(invoker, "requestRawApi", Mockito.anyList(), Mockito.any());

        invoker.invoke(incorrectInput);
    }

    /**
     * Тест-кейс ошибки входных данных
     * Принимается input формата json
     * Ожидается ошибка
     *
     * Файлы:
     * surelead/success_response.json
     */
    @Test(expected = ExternalSystemArgumentException.class)
    public void testInputNotXML() throws Exception{
        final String incorrectInput = TestHelper.getFile(SureleadInvoker.class, "surelead/success_response.json");

        PowerMockito
                .doReturn(null)
                .when(invoker, "requestRawApi", Mockito.anyList(), Mockito.any());

        invoker.invoke(incorrectInput);
    }

    /**
     * Тест-кейс ошибки преобразования ответа от сервиса к формату json
     * Принимается корректный input
     * Ожидается, что сервис вернет некорректный response не json формат
     * Ожидается ошибка
     *
     * Файлы:
     * surelead/success_input.txt
     */
    @Test(expected = ExternalSystemException.class)
    public void testResponse() throws Exception{
        final String successInput = TestHelper.getFile(SureleadInvoker.class, "surelead/success_input.txt");

        StringEntity stringEntity = new StringEntity(successInput, Charset.forName("UTF-8"));
        PowerMockito
                .doReturn(stringEntity)
                .when(invoker, "requestRawApi", Mockito.anyList(), Mockito.any());

        invoker.invoke(successInput);
    }
}