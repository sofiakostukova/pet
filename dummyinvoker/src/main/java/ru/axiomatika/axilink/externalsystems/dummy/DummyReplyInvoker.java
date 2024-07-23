package ru.axiomatika.axilink.externalsystems.dummy;

import ru.axiomatika.axilink.api.exceptions.*;
import ru.axiomatika.axilink.api.model.Call;
import ru.axiomatika.axilink.commons.externalsystems.AbstractExternalSystem;
import ru.axiomatika.axilink.commons.util.NumberHelper;
import ru.axiomatika.axilink.commons.util.StringHelper;

/**
 * Данный инвокер предназначен для пробпроса входных данных на выход с возможностью заджержки
 * <p>
 * Настройки:
 * delay - флаг применения задержки (по умолчанию false)
 * delayTime - время задержки (при отсутствии задержка определяется случайно от 1 до 35 секунд)
 * <p>
 * Настрйки инвокера могут быть пустыми, тогда задержки в ответе не будет
 * <p>
 * Created by astakhov on 28.02.2020.
 */
public class DummyReplyInvoker extends AbstractExternalSystem {

    public DummyReplyInvoker(String connectionString) {
        super(connectionString);
    }

    public DummyReplyInvoker(Call call) {
        super(call);
    }

    @Override
    public String invoke(String input) throws ExternalSystemException, ExternalSystemIOException, ExternalSystemArgumentException, AxiLinkException, ExternalSystemCryptoException {
        if (StringHelper.isNullOrEmpty(getConnectionString()) == false
                && StringHelper.parseBoolean(getConnectionParams().get("delay"), false)) {
            try {
                int sleepTime;
                if (getConnectionParams().containsKey("delayTime")) {
                    sleepTime = Integer.parseInt(getConnectionParams().get("delayTime"));
                } else {
                    sleepTime = NumberHelper.randInt(1000, 35000);
                }
                log.info("(Dummy)Задержка на {} милисекунд", sleepTime);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new ExternalSystemException("(Dummy)Ошибка при приостоновлении работы заявки", e);
            }
        }

        return input;
    }

}
