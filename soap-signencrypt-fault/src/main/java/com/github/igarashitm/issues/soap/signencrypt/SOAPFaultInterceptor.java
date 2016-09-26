package com.github.igarashitm.issues.soap.signencrypt;

import java.util.List;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.messaging.saaj.soap.ver1_1.Message1_1Impl;
import com.sun.xml.messaging.saaj.soap.ver1_2.Message1_2Impl;

/**
 * Interceptor as workaround for https://issues.apache.org/jira/browse/CXF-6319
 * and https://issues.jboss.org/browse/ENTESB-5166 - The signature or decryption was invalid.
 *
 * Sets attribute namespace in SOAPFaults.
 *
 * @author Simon Vogel
 *
 */
public class SOAPFaultInterceptor extends AbstractSoapInterceptor {

    private final Logger _logger = LoggerFactory.getLogger(SOAPFaultInterceptor.class);

    private static final String NAMESPACE_URL = "http://www.w3.org/2000/xmlns/";

    public SOAPFaultInterceptor() {
        super(Phase.SETUP);
    }

    public void handleMessage(SoapMessage message) throws Fault {
        _logger.debug("Message SOAP Version: {} | Message SOAP Version Namespace: {}", message.getVersion().getVersion(), message.getVersion().getNamespace());

        @SuppressWarnings("rawtypes")
        List content = message.getContent(java.util.List.class);
        if (content == null || content.size() == 0)
            return;
        Object messageContent = content.get(0);
        if (messageContent instanceof Message1_1Impl) {
			Message1_1Impl msg = (Message1_1Impl) messageContent;
			try {
				SOAPBody soapBody = msg.getSOAPBody();
				setFaultAttributeNS(soapBody, Message1_1Impl.URI_NS_SOAP_1_1_ENVELOPE);
			} catch (SOAPException e) {
				_logger.warn("Error setting attribute ns.", e);
			}
		} else if (messageContent instanceof Message1_2Impl) {
			Message1_2Impl msg = (Message1_2Impl) messageContent;
			try {
				SOAPBody soapBody = msg.getSOAPBody();
				setFaultAttributeNS(soapBody, Message1_1Impl.URI_NS_SOAP_1_2_ENVELOPE);
			} catch (SOAPException e) {
				_logger.warn("Error setting attribute ns.", e);
			}
		} else {
			_logger.warn("Could not get and cast message to fix SOAPFault attribute ns.");
		}
    }

    private void setFaultAttributeNS(SOAPBody soapBody, String uriSoapEnvelopeNS) {
		SOAPFault fault = soapBody.getFault();
		if (fault == null) {
			_logger.debug("No SOAPFault in body.");
			return;
		}
		String prefix = fault.getPrefix();
		if (prefix != null && !prefix.isEmpty()) {
			_logger.debug("Setting attributeNS in SOAPFault.");
			fault.setAttributeNS(NAMESPACE_URL, "xmlns:" + prefix, uriSoapEnvelopeNS);
		}
	}
}
