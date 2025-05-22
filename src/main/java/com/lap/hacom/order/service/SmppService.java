package com.lap.hacom.order.service;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.*;
import com.lap.hacom.order.config.SmppConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class SmppService {

    private static final Logger logger = LoggerFactory.getLogger(SmppService.class);

    private final SmppConfig smppConfig;
    private final Counter smsCounter;

    private DefaultSmppClient smppClient;
    private SmppSession smppSession;

    @Autowired
    public SmppService(SmppConfig smppConfig, MeterRegistry meterRegistry) {
        this.smppConfig = smppConfig;
        this.smsCounter = Counter.builder("hacom.sms.sent.total")
                .description("Total number of SMS messages sent")
                .register(meterRegistry);
    }

    @PostConstruct
    public void initialize() {
        if (!smppConfig.isEnabled()) {
            logger.info("SMPP service is disabled in configuration");
            return;
        }

        try {
            logger.info("Initializing SMPP client with host: {}:{}", smppConfig.getHost(), smppConfig.getPort());

            // Create SMPP client
            smppClient = new DefaultSmppClient();

            // Configure session
            SmppSessionConfiguration config = new SmppSessionConfiguration();
            config.setWindowSize(1);
            config.setName("HacomSMPP");
            config.setType(SmppBindType.TRANSCEIVER);
            config.setHost(smppConfig.getHost());
            config.setPort(smppConfig.getPort());
            config.setSystemId(smppConfig.getSystemId());
            config.setPassword(smppConfig.getPassword());
            config.setSystemType(smppConfig.getSystemType());
            config.setInterfaceVersion(smppConfig.getInterfaceVersion());
            config.setAddressRange(new Address((byte) 0x00, (byte) 0x00, ""));

            // Create session
            smppSession = smppClient.bind(config, new DefaultSmppSessionHandler());

            logger.info("SMPP session established successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize SMPP client: {}", e.getMessage(), e);
        }
    }

    public boolean sendSms(String phoneNumber, String message) {
        if (!smppConfig.isEnabled() || smppSession == null || !smppSession.isBound()) {
            logger.warn("SMPP service is not available or session not bound");
            return false;
        }

        try {
            logger.info("Sending SMS to {} with message: {}", phoneNumber, message);

            SubmitSm submitSm = new SubmitSm();
            submitSm.setSourceAddress(new Address(smppConfig.getAddressTon(),
                    smppConfig.getAddressNpi(),
                    smppConfig.getSourceAddress()));
            submitSm.setDestAddress(new Address(smppConfig.getAddressTon(),
                    smppConfig.getAddressNpi(),
                    phoneNumber));
            submitSm.setShortMessage(message.getBytes());

            // Send the message
            smppSession.submit(submitSm, 10000);

            // Increment counter for metrics
            smsCounter.increment();

            logger.info("SMS sent successfully to {}", phoneNumber);
            return true;

        } catch (RecoverablePduException | SmppTimeoutException | SmppChannelException | UnrecoverablePduException | InterruptedException e) {
            logger.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return false;
        }
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down SMPP service");

        if (smppSession != null && smppSession.isBound()) {
            try {
                smppSession.unbind(5000);
                logger.info("SMPP session unbound successfully");
            } catch (Exception e) {
                logger.error("Error unbinding SMPP session: {}", e.getMessage(), e);
            }
        }

        if (smppClient != null) {
            smppClient.destroy();
            logger.info("SMPP client destroyed");
        }
    }
}