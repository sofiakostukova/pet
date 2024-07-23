package ru.axiomatika.axilink.exsternalsystems.surelead;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import ru.axiomatika.axilink.api.exceptions.*;
import ru.axiomatika.axilink.api.model.Call;
import ru.axiomatika.axilink.api.model.enums.SSLContextType;
import ru.axiomatika.axilink.api.model.enums.TrustManagerFactoryType;
import ru.axiomatika.axilink.commons.externalsystems.HTTPSGetExternalSystem;
import ru.axiomatika.axilink.commons.util.StringHelper;
import ru.axiomatika.axilink.commons.util.XMLHelper;
import org.w3c.dom.Document;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Настройки инвокера:
 *  url адрес отправки запросов url=https://surelead.tech/check/{phone_number},
 *  где phone_number - номер телефона в международном формате
 *
 * Пример:
 *  url=https://surelead.tech/check/79101234567
 */
public class SureleadInvoker extends HTTPSGetExternalSystem {
    static final Pattern patternPhoneNumber = Pattern.compile("^79[0-9]{9}");

    public SureleadInvoker(String connectionString) {
        super(connectionString, TrustManagerFactoryType.PKIX, SSLContextType.TLSv1_2);
    }

    public SureleadInvoker(Call call) {
        super(call, TrustManagerFactoryType.PKIX, SSLContextType.TLSv1_2);
    }

    @Override
    protected HttpEntity getHttpEntity(List<NameValuePair> requestParams) throws ExternalSystemException {
        return null;
    }

    @Override
    protected void setHeaders(HttpGet httpGet) {
        try {
            httpGet.addHeader("X-Auth-Token", this.getConnectionParams().get("token"));
        } catch (AxiLinkException e) {
            throw new RuntimeException("Ошибка формирования http заголовка X-Auth-Token: " + e.getMessage(), e);
        }
    }

    @Override
    public String invoke(String input) throws ExternalSystemException, ExternalSystemIOException, ExternalSystemArgumentException, AxiLinkException, ExternalSystemCryptoException {
        log.info("Вызов сервиса Surelead");

        Map<String, String> inputParams = StringHelper.splitParamsStringToMap(input);
        String phoneNumber = inputParams.remove("phone_number");

        if (StringHelper.isNullOrEmpty(phoneNumber)) {
            log.error("Не заполнено обязательное поле phone_number для поиска на Surelead");
            throw new ExternalSystemArgumentException("Не заполнено обязательное поле phone_number для поиска на Surelead");
        }

        Matcher matcherPhoneNumber = patternPhoneNumber.matcher(phoneNumber);
        if(!matcherPhoneNumber.matches()) {
            throw new ExternalSystemArgumentException("Номер телефона не в международном формате");
        }

        if (StringHelper.isNullOrEmpty(this.getConnectionParams().get("token"))) {
            log.error("Отсутствует токен для авторизации в сервисе Surelead");
            throw new ExternalSystemArgumentException("Отсутствует токен для авторизации в сервисе Surelead");
        }

        // Формируем адрес запроса
        List<NameValuePair> params = new ArrayList<>();
        for (String key : inputParams.keySet()) {
            params.add(new BasicNameValuePair(key, inputParams.get(key)));
        }

        // пытаемся ортправить запрос
        String response = null;
        try {
            response = requestApi(params, new URI(this.getConnectionParams().get("url") + phoneNumber));
        } catch (IOException | URISyntaxException e) {
            log.error("Ошибка отправки запроса к сервису: {}", e.getMessage());
            throw new ExternalSystemIOException("Ошибка отправки запроса к сервису: " + e.getMessage(), e);
        }
        log.debug("Сырой ответ от внешней системы = '{}'", response);

        // преобразуем ответ от сервиса в json
        JSONObject responseJson = null;
        try {
            responseJson = new JSONObject(response);
        } catch (JSONException e) {
            log.error("Ошибка преобразования ответа от сервиса в json: {}, сырой ответ от сервиса '{}'", e.getMessage(), response);
            throw new ExternalSystemException("Ошибка преобразования ответа от сервиса в json: {}: " + e.getMessage(), e);
        }

        // преобразуем ответ от сервиса из json в xml
        Document responseXML = null;
        try {
            responseXML = XMLHelper.convertJsonToXml(responseJson, "SURELEAD");
        } catch (Exception e) {
            log.error("Ошибка преобразования запроса из xml в json: {}", e.getMessage());
            throw new ExternalSystemException("Ошибка преобразования запроса из xml в json: " + e.getMessage(), e);
        }

        // преобразуем ответ от сервиса из xml в строку
        String responseString = null;
        try {
            responseString = XMLHelper.convertToString(responseXML);
        } catch (TransformerException | UnsupportedEncodingException e) {
            log.error("Ошибка в преобразование ответа от сервиса: '{}', сырой ответ от сервиса '{}'", e.getMessage(), response);
            throw new ExternalSystemException("Ошибка в преобразование ответа от сервиса " + e.getMessage(), e);
        }

        log.info("Завершение работы сервиса");
        return responseString;
    }
}
