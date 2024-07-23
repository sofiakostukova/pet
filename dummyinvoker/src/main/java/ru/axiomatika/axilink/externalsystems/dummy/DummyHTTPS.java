package ru.axiomatika.axilink.externalsystems.dummy;

import org.apache.http.client.methods.HttpPost;
import ru.axiomatika.axilink.api.exceptions.*;
import ru.axiomatika.axilink.api.model.Call;
import ru.axiomatika.axilink.api.model.ContextRequest;
import ru.axiomatika.axilink.api.model.InvokerResponse;
import ru.axiomatika.axilink.api.model.enums.SSLContextType;
import ru.axiomatika.axilink.api.model.enums.TrustManagerFactoryType;
import ru.axiomatika.axilink.commons.externalsystems.HTTPSExternalSystem;
import ru.axiomatika.axilink.commons.util.XMLHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

/**
 * Created by ignatov on 29.10.2021.
 */
public class DummyHTTPS extends HTTPSExternalSystem {

    public DummyHTTPS(String connectionString) {
        super(connectionString, TrustManagerFactoryType.PKIX, SSLContextType.TLSv1_2);
    }

    public DummyHTTPS(Call call) {
        super(call, TrustManagerFactoryType.PKIX, SSLContextType.TLSv1_2);
    }

    @Override
    public String invoke(String input) throws ExternalSystemException, ExternalSystemIOException, ExternalSystemArgumentException, AxiLinkException, ExternalSystemCryptoException {
        return invokeWithObjectResponse(input, null).xml;
    }

    @Override
    public InvokerResponse invokeWithObjectResponse(String input) throws ExternalSystemException, ExternalSystemIOException, ExternalSystemArgumentException, AxiLinkException, ExternalSystemCryptoException {
        return super.invokeWithObjectResponse(input, null);
    }

    @Override
    public InvokerResponse invokeWithObjectResponse(String input, ContextRequest context) throws ExternalSystemException, ExternalSystemIOException, ExternalSystemArgumentException, AxiLinkException, ExternalSystemCryptoException {

        HttpPost httpPost = new HttpPost(parameters().getString(Param.URL));
        executeRequest(httpPost);


        String result;
        try {
            result = XMLHelper.convertToString(XMLHelper.generateEmptyXmlDocument("Dummy"));
        } catch (TransformerException | UnsupportedEncodingException | ParserConfigurationException e) {
            throw new AxiLinkException("(Dummy)Ошибка при создании пустого XML документа", e);
        }

        Optional<Boolean> emptyResponse = parameters().getBooleanOptional("emptyResponse");
        return new InvokerResponse(result, emptyResponse.orElse(false));
    }
}
