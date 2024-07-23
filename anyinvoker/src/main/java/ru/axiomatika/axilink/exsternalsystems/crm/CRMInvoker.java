package ru.axiomatika.axilink.exsternalsystems.crm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import ru.axiomatika.axilink.api.exceptions.*;
import ru.axiomatika.axilink.api.model.Call;
import ru.axiomatika.axilink.api.model.enums.AxiErrorDataCodeEnum;
import ru.axiomatika.axilink.api.model.enums.SSLContextType;
import ru.axiomatika.axilink.api.model.enums.TrustManagerFactoryType;
import ru.axiomatika.axilink.commons.externalsystems.HTTPSGetExternalSystem;
import ru.axiomatika.axilink.commons.util.JsonXMLConverter;
import ru.axiomatika.axilink.commons.util.StringHelper;
import ru.axiomatika.axilink.commons.util.XMLHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * url - url сервера
 * path - путь для запроса
 * apiKey - ключ авторизации
 * id - id таблицы для запроса
 */
public class CRMInvoker extends HTTPSGetExternalSystem {
    private static final List<String> requiredConnectionParams = new ArrayList<>();
    private boolean init = false;
    private JsonXMLConverter converter = new JsonXMLConverter();

    static {
        requiredConnectionParams.add("url");
        requiredConnectionParams.add("path");
        requiredConnectionParams.add("apiKey");
        requiredConnectionParams.add("id");
    }

    public CRMInvoker(String connectionString, JsonXMLConverter jsonXMLConverter) {
        super(connectionString, TrustManagerFactoryType.PKIX, SSLContextType.TLSv1_2);
        converter = jsonXMLConverter;
    }

    public CRMInvoker(String connectionString) {
        super(connectionString, TrustManagerFactoryType.PKIX, SSLContextType.TLSv1_2);
    }

    public CRMInvoker(Call call) {
        super(call, TrustManagerFactoryType.PKIX, SSLContextType.TLSv1_2);
    }

    @Override
    public String invoke(String input) throws ExternalSystemException, ExternalSystemIOException, ExternalSystemArgumentException, AxiLinkException, ExternalSystemCryptoException {
        log.info("Вызов сервиса");

        JSONObject requestObject;
        try {
            requestObject = converter.convertXMLToJson(input);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            String rawError = "Ошибка при конвертации входных данных: " + e.getMessage();
            log.error(rawError);
            throw buildException(ExternalSystemException.class, e, rawError,
                    AxiErrorDataCodeEnum.INVOKER_DATA_CONVERT_ERROR, null);
        }
        XMLHelper.processRequest(requestObject);
        requestObject = requestObject.getJSONObject("Request");

        URI url;
        try {
            url = new URI(parameters().getString("url") + parameters().getString("path"));
        } catch (URISyntaxException e) {
            String rawError = "Ошибка при инициализации uri для запроса: " + e.getMessage();
            log.error(rawError);
            throw buildException(ExternalSystemException.class, e, rawError,
                    AxiErrorDataCodeEnum.INVOKER_REQUEST_BUILD_ERROR, null);
        }

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("filter", requestObject.getJSONArray("filter").toString()));
        params.add(new BasicNameValuePair("apiKey", parameters().getString("apiKey")));
        params.add(new BasicNameValuePair("id", parameters().getString("id")));

        CloseableHttpResponse httpResponse;
        log.info("Отправка запроса");
        try {
            httpResponse = requestRawHttpApi(params, url);
        } catch (IOException e) {
            String rawError = "Ошибка при отправки запроса во внешний сервис: " + e.getMessage();
            log.error(rawError);
            throw buildException(ExternalSystemException.class, e, rawError,
                    AxiErrorDataCodeEnum.INVOKER_REQUEST_NETWORK_ERROR, null);
        }

        String response;
        try {
            response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            String rawError = "Ошибка при обработке сырого ответа от сервиса: " + e.getMessage();
            log.error(rawError);
            throw buildException(ExternalSystemException.class, e, rawError,
                    AxiErrorDataCodeEnum.INVOKER_RESPONSE_VALIDATION_ERROR, null);
        }

        int statusCode = httpResponse.getStatusLine().getStatusCode();
        log.debug("Получено statusCode: {}, response: {}", statusCode, response);
        if (statusCode >= 500) {
            log.error("Серверная ошибка statusCode: {}, response: {}", statusCode, response);
            String rawError = String.format("Серверная ошибка statusCode: %s", statusCode);
            throw buildException(ExternalSystemException.class, null, rawError,
                    AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR, null);
        } else if (statusCode >= 400) {
            log.error("Ошибка запроса во внешний сервис statusCode: {}, response: {}", statusCode, response);
            String rawError = String.format("Серверная ошибка statusCode: %s", statusCode);
            throw buildException(ExternalSystemException.class, null, rawError,
                    AxiErrorDataCodeEnum.INVOKER_REQUEST_BUILD_ERROR, null);
        }

        if (StringHelper.isNullOrEmpty(response)) {
            String rawError = "Ошибка, получен пустой ответ от внешнего сервиса";
            log.error(rawError);
            throw buildException(ExternalSystemException.class, null, rawError,
                    AxiErrorDataCodeEnum.INVOKER_RESPONSE_VALIDATION_ERROR, null);
        }

        JSONObject converted = convertJson(response);

        try {
            response = XMLHelper.convertToString(XMLHelper.convertJsonToXml(converted, "CRM_AVENTUS"));
        } catch (TransformerException | UnsupportedEncodingException | ParserConfigurationException e) {
            log.error("Ошибка преобразования ответа от внешнего сервиса: {}, response: {}", e.getMessage(), response);
            String rawError = String.format("Ошибка преобразования ответа от внешнего сервиса: %s", e.getMessage());
            throw buildException(ExternalSystemException.class, e, rawError,
                    AxiErrorDataCodeEnum.INVOKER_DATA_CONVERT_ERROR, null);
        }

        return response;
    }

    private JSONObject convertJson(String responseData) throws ExternalSystemException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode data = null;
        try {
            data = mapper.readTree(responseData);
        } catch (IOException e) {
            log.error("Невозможно разобрать ответ с сервера: json: {}, сообщение: {}", responseData, e.getMessage());
            throw buildException(ExternalSystemException.class, e, "Невозможно разобрать ответ с сервера", AxiErrorDataCodeEnum.INVOKER_RESPONSE_VALIDATION_ERROR, responseData);
        }

        if (!data.has("data")) {
            log.error("В ответе нет поля data: {}", responseData);
            throw buildException(ExternalSystemException.class, null, "", AxiErrorDataCodeEnum.INVOKER_RESPONSE_VALIDATION_ERROR, null);
        }

        if (!data.get("data").has("names")) {
            log.error("В сущности data нет поля names: {}", responseData);
            throw buildException(ExternalSystemException.class, null, "", AxiErrorDataCodeEnum.INVOKER_RESPONSE_VALIDATION_ERROR, null);
        }

        if (!data.get("data").has("data")) {
            log.error("В сущности data нет поля data: {}", responseData);
            throw buildException(ExternalSystemException.class, null, "", AxiErrorDataCodeEnum.INVOKER_RESPONSE_VALIDATION_ERROR, null);
        }

        if (!data.has("status")) {
            log.error("В ответе нет поля status: {}", responseData);
            throw buildException(ExternalSystemException.class, null, "", AxiErrorDataCodeEnum.INVOKER_RESPONSE_VALIDATION_ERROR, null);
        }

        Iterator<JsonNode> names = data.get("data").get("names").iterator();
        JSONObject converted = new JSONObject();
        JSONObject convertedData = new JSONObject();
        JSONObject convertedNames = new JSONObject();

        converted.put("status", data.get("status").intValue());
        converted.put("data", convertedData);
        convertedData.put("names", convertedNames);

        JsonNode dataArray = data.get("data").get("data");
        if (dataArray.size() > 0) {
            dataArray = dataArray.get(0);
            for (int i = 0; i < dataArray.size(); i++) {
                convertedNames.put(names.next().asText(), dataArray.get(i).asText());
            }
        } else {
            log.error("Ответ пришел без данных. Только с названиями полей.");
            throw buildException(ExternalSystemException.class, null, "Ответ пришел без данных. Только с названиями полей.", AxiErrorDataCodeEnum.INVOKER_RESPONSE_VALIDATION_ERROR, "Ответ сервера: " + responseData);
        }
        return converted;
    }

}
