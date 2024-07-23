package ru.axiomatika.axilink.externalsystems.fedresurs;

import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import ru.axiomatika.axilink.api.exceptions.AxiLinkException;
import ru.axiomatika.axilink.api.exceptions.ExternalSystemCryptoException;
import ru.axiomatika.axilink.api.exceptions.ExternalSystemException;
import ru.axiomatika.axilink.api.exceptions.ExternalSystemIOException;
import ru.axiomatika.axilink.api.model.InvokerResponse;
import ru.axiomatika.axilink.api.model.ResponseData;
import ru.axiomatika.axilink.api.model.dataobjects.ExceptionBuilder;
import ru.axiomatika.axilink.api.model.enums.AxiErrorDataCodeEnum;
import ru.axiomatika.axilink.api.model.enums.SSLContextType;
import ru.axiomatika.axilink.api.model.enums.TrustManagerFactoryType;
import ru.axiomatika.axilink.commons.externalsystems.HTTPSGetExternalSystem;
import ru.axiomatika.axilink.commons.util.JsonXMLConverter;
import ru.axiomatika.axilink.commons.util.StringHelper;
import ru.axiomatika.axilink.commons.util.XMLHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Инвокер позволяет осуществить поиск должника по ФИО и подсчитать
 * количество совпадений по фио и дате рождения.
 */
public class FedresursInvoker extends HTTPSGetExternalSystem {


    private final JsonXMLConverter jsonXMLConverter = new JsonXMLConverter();

    public FedresursInvoker(String connectionString) {
        super(connectionString, TrustManagerFactoryType.PKIX, SSLContextType.TLSv1_2);
    }

    public FedresursInvoker(String connectionString, String trustManagerFactoryType, String sslContextType) {
        super(connectionString, trustManagerFactoryType, sslContextType);
    }

    @Override
    public void setInvokerRequiredParams() {
        requiredConnectionParams = new ArrayList<>();
        requiredConnectionParams.add(Param.URL);
        requiredConnectionParams.add("card_url");
    }

    @Override
    public void init() throws Exception {
        super.init();
    }

    @Override
    public InvokerResponse invokeWithObjectResponse(String input) throws ExternalSystemException, AxiLinkException, ExternalSystemIOException, ExternalSystemCryptoException {
        if (StringHelper.isNullOrEmpty(input)) {
            log.error("Входные данные пусты: ");
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.REQUEST_PARAMETER_VALIDATION_ERROR, ExternalSystemException.class)
                    .setRawError("Входные данные пусты")
                    .build();
        }

        JSONObject requestObject;
        try {
            requestObject = jsonXMLConverter.convertXMLToJson(input).getJSONObject("Request");
        } catch (IOException | ParserConfigurationException | SAXException | JSONException e) {
            log.error("Ошибка построения данных для запроса: {}", input, e);
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_DATA_CONVERT_ERROR, ExternalSystemException.class)
                    .setException(e)
                    .setRawError("Ошибка построения данных для запроса" + e.getMessage())
                    .build();
        }

        if (!requestObject.has("birthdate") || !requestObject.has("first_name") || !requestObject.has("last_name")) {
            log.error("Отсутствуют одно или несколько полей для запроса: {}", requestObject);
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.REQUEST_PARAMETER_VALIDATION_ERROR, ExternalSystemException.class)
                    .setRawError("Отсутствуют одно или несколько полей для запроса")
                    .build();
        }


        if (StringHelper.isNullOrEmpty(requestObject.getString("birthdate")) ||
                StringHelper.isNullOrEmpty(requestObject.getString("first_name")) ||
                StringHelper.isNullOrEmpty(requestObject.getString("last_name"))) {

            log.error("Отсутствуют один или несколько полей для сравнения: {}", requestObject);
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.REQUEST_PARAMETER_VALIDATION_ERROR, ExternalSystemException.class)
                    .setRawError("Отсутствуют один или несколько полей для сравнения")
                    .build();
        }

        String searchString = "";

        searchString += requestObject.getString("first_name");
        if (requestObject.has("last_name"))
            searchString += (" " + requestObject.getString( "last_name"));
        if (requestObject.has("birthdate"))
            searchString += (" " + requestObject.getString("birthdate"));

        String isActiveLegalCaseDefault = "null",
            offsetDefault = "0",
            limitDefault = "15",
            soughtDebtorBirthdateString = requestObject.getString("birthdate"),
            searchUrl = parameters().getString(Param.URL),
            cardUrl = parameters().getString("card_url");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
        LocalDate soughtDebtorBirthdate;
        try {
            soughtDebtorBirthdate = LocalDate.parse(soughtDebtorBirthdateString, formatter);
        } catch (DateTimeParseException e) {
            log.error("Неверный формат даты: {}", soughtDebtorBirthdateString);
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_DATA_CONVERT_ERROR, ExternalSystemException.class)
                    .setException(e)
                    .setDescription("Неверный формат даты")
                    .build();
        }

        HttpGet request = new HttpGet();

        request.setHeaders(new Header[]{
                new BasicHeader("searchString", searchString),
                new BasicHeader("isActiveLegalCase", isActiveLegalCaseDefault),
                new BasicHeader("limit", limitDefault),
                new BasicHeader("offset", offsetDefault),
                new BasicHeader("Referer", searchUrl),
        });

        HttpClientContext context = HttpClientContext.create();
        CookieStore cookieStore = new BasicCookieStore();
        cookieStore.addCookie(new BasicClientCookie("name", "debtorsearch"));
        context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

        ResponseData seachResponse;

        try {
            request.setURI(URI.create(parameters().getString(Param.URL)));
            seachResponse = executeRequest(request, context);
        } catch (ExternalSystemException | AxiLinkException | ExternalSystemCryptoException |
                 ExternalSystemIOException | IllegalArgumentException e) {
            log.error("Ошибка отправки запроса к сервису: {}", e.getMessage());
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_REQUEST_NETWORK_ERROR, ExternalSystemException.class)
                    .setException(e)
                    .setDescription("Ошибка отправки запроса к сервису")
                    .build();
        }

        String responseBody = seachResponse.getBody();
        int statusCode = seachResponse.getStatusCode();

        if (StringHelper.isNullOrEmpty(responseBody)) {
            String rawError = "Получен пустой ответ от сервиса";
            log.error(rawError);
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR, ExternalSystemException.class)
                    .setRawError(String.format("Сервер вернул код %d", statusCode))
                    .addSourceError(Integer.toString(statusCode), "")
                    .build();
        }

        JSONObject responseJson;
        try {
            responseJson = new JSONObject(responseBody);
        } catch (JSONException e) {
            log.error("Ошибка разбора json ответа, response: {}", responseBody, e);
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_DATA_CONVERT_ERROR, ExternalSystemException.class)
                    .setException(e)
                    .setDescription("Ошибка разбора json ответа")
                    .build();
        }

        if (statusCode != 200 && statusCode != 404) {
            if (responseJson.has("error") && responseJson.has("message")) {
                log.error("Ошибка запроса к внешнему сервису, statusCode: {}, responsePost: {}", statusCode, responseBody);
                throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR, ExternalSystemException.class)
                        .addSourceError(responseJson.getJSONObject("error").toString(), responseJson.getString("message"))
                        .build();
            } else {
                log.error("Ошибка запроса к внешнему сервису, statusCode: {}, responsePost: {}", statusCode, responseBody);
                throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR, ExternalSystemException.class)
                        .addSourceError(String.valueOf(statusCode), "")
                        .build();
            }
        }

        if (statusCode == 404) {
            if (!responseJson.has("message")) {
                log.error("Ошибка запроса к внешнему серису, statusCode: {}, responseJson: {}", statusCode, responseJson);
                String rawError = "Ошибка запроса к внешнему сервису";
                throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR, ExternalSystemException.class)
                        .setRawError(rawError)
                        .build();
            } else if (!responseJson.get("message").equals("Not found")) {
                log.error("Ошибка запроса к внешнему серису, statusCode: {}, responseJson: {}", statusCode, responseJson);
                String rawError = "Ошибка запроса к внешнему сервису";
                throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR, ExternalSystemException.class)
                        .setRawError(rawError)
                        .addSourceError(responseJson.getString("message"), responseJson.getJSONArray("error").toString())
                        .build();
            }
        }

        JSONArray pageData;
        int result = 0;

        try {
            pageData = responseJson.getJSONArray("pageData");
        } catch (JSONException e) {
            log.error("Отсутствует объект pageData в json, response: {}", responseBody, e);
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_DATA_CONVERT_ERROR, ExternalSystemException.class)
                    .setException(e)
                    .setDescription("Отсутствует объект pageData в json")
                    .build();
        }

        Iterator<Object> iterator = pageData.iterator();
        HttpGet cardRequest = new HttpGet();
        JSONObject cardResponseJson;
        boolean isResponsesSuccess = false;
        String id;

        while (iterator.hasNext()) {
            try {
                id = ((JSONObject) iterator.next()).getString("guid");
            } catch (JSONException e) {
                log.error("Отсутствует объект guid в json, response: {}", responseBody, e);
                continue;
            }

            String cardResponseBody;
            int cardStatusCode;

            try {
                cardRequest.setURI(URI.create(cardUrl + "/" + id));
                cardRequest.addHeader("Referer", cardUrl);
                ResponseData cardResponse = executeRequest(cardRequest);
                cardStatusCode = cardResponse.getStatusCode();
                cardResponseBody = cardResponse.getBody();
                cardResponseJson = new JSONObject(cardResponseBody);
            } catch (ExternalSystemException | AxiLinkException | ExternalSystemCryptoException |
                     ExternalSystemIOException | IllegalArgumentException | JSONException e) {
                log.error("Ошибка отправки запроса на получение карточки, response: {}", e.getMessage());
                continue;
            }

            if (cardStatusCode != 200) {
                log.error("Ошибка запроса к внешнему серису, statusCode: {}, responseJson: {}", cardStatusCode, cardResponseJson);
                continue;
            }

            LocalDate currentDebtorBirthdate;
            try {
                DateTimeFormatter currentDebtorDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                String currentDebtorBirthdateString = cardResponseJson.getJSONObject("info").getString("birthdateBankruptcy");
                currentDebtorBirthdate = LocalDateTime.parse(currentDebtorBirthdateString, currentDebtorDateFormatter).toLocalDate();
            } catch (DateTimeParseException | JSONException e) {
                log.error("Не удалось преобразовать дату рождения: {}", e.getMessage());
                continue;
            }

            if (soughtDebtorBirthdate.isEqual(currentDebtorBirthdate)) result++;
            isResponsesSuccess = true;
        }

        if (!isResponsesSuccess) {
            log.error("Не удалось получить информацию ни об одной карточке");
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR, ExternalSystemException.class)
                    .setDescription("Не удалось получить информацию ни об одной карточке")
                    .build();
        }

        String convertedXml;
        try {
            Map<String, String> params = new HashMap<>();
            params.put("result", String.valueOf(result));
            Document xml = XMLHelper.generateXmlFromParams(params, "Result");
            Map<String, String> outputParams = new HashMap<>();
            outputParams.put(OutputKeys.INDENT, "yes");
            System.out.println();
            convertedXml = XMLHelper.convertToString(xml, "UTF-8", outputParams);
        } catch (AxiLinkException | ParserConfigurationException | TransformerException |
                 UnsupportedEncodingException e) {
            log.error("Произошла ошибка при формировании ответа: {}", e.getMessage());
            throw new ExceptionBuilder<>(AxiErrorDataCodeEnum.INVOKER_DATA_CONVERT_ERROR, ExternalSystemException.class)
                    .setException(e)
                    .setDescription("Произошла ошибка при формировании ответа")
                    .build();
        }

        return new InvokerResponse(convertedXml, result == 0);
    }
}