package ru.axiomatika.axilink.externalsystems.dummy;

import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.axiomatika.axilink.api.exceptions.*;
import ru.axiomatika.axilink.api.model.Call;
import ru.axiomatika.axilink.api.model.ContextRequest;
import ru.axiomatika.axilink.api.model.InvokerResponse;
import ru.axiomatika.axilink.api.model.dataobjects.ExceptionBuilder;
import ru.axiomatika.axilink.api.model.enums.AxiErrorDataCodeEnum;
import ru.axiomatika.axilink.api.service.ConfigurationService;
import ru.axiomatika.axilink.commons.externalsystems.AbstractExternalSystem;
import ru.axiomatika.axilink.commons.util.NumberHelper;
import ru.axiomatika.axilink.commons.util.XMLHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by ignatov on 09.06.2021.
 */
public class DummyInvokerV2 extends AbstractExternalSystem {

    @Autowired
    private ConfigurationService configurationService;

    public DummyInvokerV2(String connectionString) {
        super(connectionString);
    }

    public DummyInvokerV2(Call call) {
        super(call);
    }

    @Override
    public String invoke(String input) throws ExternalSystemException, ExternalSystemIOException, ExternalSystemArgumentException, AxiLinkException, ExternalSystemCryptoException {
        return null;
    }

    @Override
    public InvokerResponse invokeWithObjectResponse(String input, ContextRequest context) throws ExternalSystemException, ExternalSystemIOException, ExternalSystemArgumentException, AxiLinkException, ExternalSystemCryptoException {
        if (parameters().isExist("delay") && parameters().getBoolean("delay")) {
            //получить retry_count из контекста исполнения или CallParameter
            int retryCount;
            if (context == null) {
                //получить retry_count из CallParameter
                if (!parameters().isExist("retryCount")) {
                    log.error("Отсутствует обязательный параметр retryCount");
                    throw new ExternalSystemIOException("Отсутствует обязательный параметр retryCount");
                }
                retryCount = parameters().getInteger("retryCount");
            } else {
                //получить retry_count из контекста исполнения...
                Document contextXML;
                try {
                    contextXML = XMLHelper.parseXml(context.getText());
                } catch (ParserConfigurationException | IOException | SAXException e) {
                    log.error("Ошибка разбора XML контекста");
                    throw new ExternalSystemIOException("Ошибка разбора XML контекста", e);
                }

                NodeList nodeList = contextXML.getElementsByTagName("retry_count");
                if (nodeList.getLength() == 0) {
                    log.error("Отсутствует обязательный параметр retry_count в контексте исполнения");
                    throw new ExternalSystemArgumentException("Отсутствует обязательный параметр retry_count в контексте исполнения");
                }
                retryCount = Integer.parseInt(nodeList.item(0).getTextContent());
            }

            if (retryCount > 0) {
                retryCount--;

                long sleepTime = parameters().isExist("sleepTime") ? parameters().getInteger("sleepTime") : NumberHelper.randInt(1000, 35000);
                log.info("(Dummy)Задержка на {} миллисекунд", sleepTime);

                //собрать xml для контекста исполнения
                Map<String, String> contextParameters = new HashMap<>();
                contextParameters.put("retry_count", Integer.toString(retryCount));
                String contextXMLString = buildContext(contextParameters);

                context = new ContextRequest(contextXMLString, sleepTime);
                return new InvokerResponse(null, false, context);
            }
        }

        if (parameters().isExist("error") && parameters().getBoolean("error")) {
            throw new ExceptionBuilder<>(ExternalSystemException.class)
                    .setCode(AxiErrorDataCodeEnum.COMMON_INVOKER_ERROR)
                    .setDescription("Тестовое исключение")
                    .setRawError("Тестовое исключение")
                    .build();
        }

        if (parameters().isExist("error_random") && parameters().getBoolean("error_random")) {
            if (NumberHelper.randInt(1000, 50000) % 10 == 0) {
                throw new ExternalSystemException("(Dummy)Тестовое исключение");
            }
        }

        String result;
        try {
            result = XMLHelper.convertToString(XMLHelper.generateEmptyXmlDocument("Dummy"));
        } catch (TransformerException | UnsupportedEncodingException | ParserConfigurationException e) {
            throw new AxiLinkException("(Dummy)Ошибка при создании пустого XML документа", e);
        }

        Optional<Boolean> emptyResponse = parameters().getBooleanOptional("emptyResponse");
        return new InvokerResponse(result, emptyResponse.orElse(false));
    }

    private String buildContext(Map<String, String> contextParameters) throws ExternalSystemException {
        //собрать xml для контекста исполнения
        Document contextXML;
        try {
            contextXML = XMLHelper.generateEmptyXmlDocument("context");
        } catch (ParserConfigurationException e) {
            log.error("Ошибка создания xml контекста исполнения");
            throw new ExternalSystemException("Ошибка создания xml контекста исполнения", e);
        }
        Element data = contextXML.getDocumentElement();

        //прикрепить параметры
        for (String key : contextParameters.keySet()) {
            Element elementParameter = data.getOwnerDocument().createElement(key);
            elementParameter.setTextContent(contextParameters.get(key));
            data.appendChild(elementParameter);
        }

        //преобразовать контекст исполнения в строку
        String contextXMLString;
        try {
            contextXMLString = XMLHelper.convertToString(contextXML);
        } catch (TransformerException | UnsupportedEncodingException e) {
            log.error("Ошибка преобразования xml контекста исполнения в строку");
            throw new ExternalSystemException("Ошибка преобразования xml контекста исполнения в строку", e);
        }

        return contextXMLString;
    }
}
