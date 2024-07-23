package ru.axiomatika.axilink.externalsystems.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.axiomatika.axilink.api.exceptions.*;
import ru.axiomatika.axilink.api.model.Call;
import ru.axiomatika.axilink.commons.externalsystems.AbstractExternalSystem;
import ru.axiomatika.axilink.commons.util.NumberHelper;
import ru.axiomatika.axilink.commons.util.StringHelper;
import ru.axiomatika.axilink.commons.util.XMLHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Random;

/**
 * Created by tikhoninav on 08.09.2016.
 */
public class DummyInvoker extends AbstractExternalSystem {

    public DummyInvoker(String connectionString) {
        super(connectionString);
    }

    public DummyInvoker(Call call) { super(call); }

    @Override
    public String invoke(String input) throws ExternalSystemException, ExternalSystemIOException, ExternalSystemArgumentException, AxiLinkException, ExternalSystemCryptoException {
        Map<String, String> inputParams = StringHelper.splitParamsStringToMap(input);

        String result = null;
        try {
            result = XMLHelper.convertToString(XMLHelper.generateEmptyXmlDocument("Dummy"));
        } catch (TransformerException | UnsupportedEncodingException|ParserConfigurationException e) {
            throw new AxiLinkException("(Dummy)Ошибка при создании пустого XML документа", e);
        }

        if (StringHelper.parseBoolean(inputParams.get("delay"), false)) {
            try {
                int sleepTime = NumberHelper.randInt(1000, 35000);
                log.info("(Dummy)Задержка на {} секунд", sleepTime);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new ExternalSystemException("(Dummy)Ошибка при приостоновлении работы заявки", e);
            }
        }

        if (StringHelper.parseBoolean(inputParams.get("error"), false)) {
            throw new ExternalSystemException("(Dummy)Тестовое исключение");
        }

        if (StringHelper.parseBoolean(inputParams.get("error_random"), false))
        {
            if (NumberHelper.randInt(1000, 50000) % 10 == 0) {
                throw new ExternalSystemException("(Dummy)Тестовое исключение");
            }
        }

        return result;
    }

}
