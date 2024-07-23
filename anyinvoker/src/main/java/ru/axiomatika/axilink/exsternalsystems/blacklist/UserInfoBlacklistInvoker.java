package ru.axiomatika.axilink.exsternalsystems.blacklist;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import ru.axiomatika.axilink.api.exceptions.*;
import ru.axiomatika.axilink.api.model.Call;
import ru.axiomatika.axilink.api.model.ResponseData;
import ru.axiomatika.axilink.api.model.dataobjects.ExceptionBuilder;
import ru.axiomatika.axilink.api.model.enums.AxiErrorDataCodeEnum;
import ru.axiomatika.axilink.api.model.enums.SSLContextType;
import ru.axiomatika.axilink.api.model.enums.TrustManagerFactoryType;
import ru.axiomatika.axilink.commons.externalsystems.HTTPSExternalSystem;
import ru.axiomatika.axilink.commons.util.JsonXMLConverter;
import ru.axiomatika.axilink.commons.util.StringHelper;
import ru.axiomatika.axilink.commons.util.XMLHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Метод проверки клиента по nationalId в черном списке
 *
 */
public class UserInfoBlacklistInvoker extends HTTPSExternalSystem {

    private final JsonXMLConverter jsonXMLConverter = new JsonXMLConverter();

    public UserInfoBlacklistInvoker(String connectionString) {
        super(connectionString, TrustManagerFactoryType.PKIX, SSLContextType.TLSv1_2);
    }

    public UserInfoBlacklistInvoker(Call call) {
        super(call, TrustManagerFactoryType.PKIX, SSLContextType.TLSv1_2);
    }

    @Override
    public void setInvokerRequiredParams() {
        requiredConnectionParams = new ArrayList<>();
        requiredConnectionParams.add(Param.URL);
        requiredConnectionParams.add("partner_key");
    }

    @Override
    public void init() throws Exception {
        super.init();
    }

    @Override
    public String invoke(String input) throws ExternalSystemException, ExternalSystemIOException, ExternalSystemArgumentException, AxiLinkException, ExternalSystemCryptoException {
        if (StringHelper.isNullOrEmpty(input)) {
            log.error("Входные данные пусты: ");
            throw new ExceptionBuilder<>(ExternalSystemException.class)
                    .setRawError("Входные данные пусты")
                    .setCode(AxiErrorDataCodeEnum.REQUEST_PARAMETER_VALIDATION_ERROR)
                    .build();
        }

        JSONObject requestObject = null;
        try {
            requestObject = jsonXMLConverter.convertXMLToJson(input).getJSONObject("Request");
        } catch (IOException | ParserConfigurationException | SAXException | JSONException e) {
            log.error("Ошибка построения данных для запроса: {}", input, e);
            throw new ExceptionBuilder<>(ExternalSystemException.class)
                    .setException(e)
                    .setRawError("Ошибка построения данных для запроса" + e.getMessage())
                    .setCode(AxiErrorDataCodeEnum.INVOKER_DATA_CONVERT_ERROR)
                    .build();
        }

        if (!requestObject.has("national_id")) {
            log.error("Отсутствуют необходмое поле national_id для запроса: {}", requestObject);
            throw new ExceptionBuilder<>(ExternalSystemException.class)
                    .setRawError("Отсутствуют необходмое поле national_id для запроса")
                    .setCode(AxiErrorDataCodeEnum.REQUEST_PARAMETER_VALIDATION_ERROR)
                    .build();
        }

        String url = parameters().getString(Param.URL);
        HttpPost request = new HttpPost(url);
        StringEntity stringEntity = new StringEntity(requestObject.toString(), ContentType.APPLICATION_JSON);
        request.setEntity(stringEntity);
        request.setHeader("PARTNER-API-KEY", parameters().getString("partner_key"));
        request.setHeader("Content-Type", "application/json");

        ResponseData response = executeRequest(request);
        String responsePost = response.getBody();
        int statusCode = response.getStatusCode();
        if (StringHelper.isNullOrEmpty(responsePost)) {
            String rawError = "Получен пустой ответ от сервиса";
            log.error(rawError);
            throw new ExceptionBuilder<>(ExternalSystemException.class)
                    .setRawError(String.format("Сервер вернул код %d", statusCode))
                    .addSourceError(Integer.toString(statusCode), "")
                    .setCode(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR)
                    .build();
        }

        JSONObject responseJson;
        try {
            responseJson = new JSONObject(responsePost);
        } catch (JSONException e) {
            log.error("Ошибка разбора json ответа, responseGet: {}", responsePost, e);
            throw new ExceptionBuilder<>(ExternalSystemException.class)
                    .setException(e)
                    .setCode(AxiErrorDataCodeEnum.INVOKER_DATA_CONVERT_ERROR)
                    .setDescription("Ошибка разбора json ответа")
                    .build();
        }

        if (statusCode != 200 && statusCode != 404) {
            if (responseJson.has("error") && responseJson.has("message")) {
                log.error("Ошибка запроса к внешнему сервису, statusCode: {}, responsePost: {}", statusCode, responsePost);
                throw new ExceptionBuilder<>(ExternalSystemException.class)
                        .setCode(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR)
                        .addSourceError(responseJson.getJSONObject("error").toString(), responseJson.getString("message"))
                        .build();
            } else {
                log.error("Ошибка запроса к внешнему сервису, statusCode: {}, responsePost: {}", statusCode, responsePost);
                throw new ExceptionBuilder<>(ExternalSystemException.class)
                        .setCode(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR)
                        .addSourceError(String.valueOf(statusCode), "")
                        .build();
            }
        }

        if (statusCode == 404) {
            if (!responseJson.has("message")) {
                log.error("Ошибка запроса к внешнему серису, statusCode: {}, responseJson: {}", statusCode, responseJson);
                String rawError = "Ошибка запроса к внешнему сервису";
                throw new ExceptionBuilder<>(ExternalSystemException.class)
                        .setRawError(rawError)
                        .setCode(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR)
                        .build();
            } else if (!responseJson.get("message").equals("Not found")) {
                log.error("Ошибка запроса к внешнему серису, statusCode: {}, responseJson: {}", statusCode, responseJson);
                String rawError = "Ошибка запроса к внешнему сервису";
                throw new ExceptionBuilder<>(ExternalSystemException.class)
                        .setRawError(rawError)
                        .setCode(AxiErrorDataCodeEnum.INVOKER_RESPONSE_ERROR)
                        .addSourceError(responseJson.getString("message"), responseJson.getJSONArray("error").toString())
                        .build();
            }
        }

        String result;
        try {
            result = XMLHelper.convertToString(XMLHelper.convertJsonToXml(responseJson, "Result"));
        } catch (TransformerException | UnsupportedEncodingException | ParserConfigurationException | JSONException e) {
            log.error("Произошла ошибка при формировании ответа: {}", response);
            throw new ExceptionBuilder<>(ExternalSystemException.class)
                    .setException(e)
                    .setCode(AxiErrorDataCodeEnum.INVOKER_DATA_CONVERT_ERROR)
                    .setRawError(response + e.getMessage())
                    .setDescription("Произошла ошибка при формировании ответа")
                    .build();
        }
        return result;
    }
}
