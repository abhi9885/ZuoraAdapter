package com.cognizant.ipm.adapter.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;

import oracle.tip.tools.ide.adapters.cloud.api.connection.CloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.api.service.AdapterPluginServiceException;
import oracle.tip.tools.ide.adapters.cloud.api.service.SOAPHelperService;
import oracle.tip.tools.ide.adapters.cloud.api.service.WSDLHelperService;

import org.w3c.dom.NodeList;

public class CloudSessionValidator {
	public static void validateSession(AdapterPluginContext context,
			CloudConnection cloudConnection)
			throws CloudApplicationAdapterException {
		Properties connectionProperties = cloudConnection
				.getConnectionProperties();

		Object sessionId = context.getContextObject("sessionId");

		Object serverURL = context.getContextObject("serverUrl");

		if ((sessionId == null) || (serverURL == null)) {
			getcloudLoginResponse(cloudConnection, context);
		} else {
			String wsdlChecksum = CloudUtil.getChecksum(
					connectionProperties.getProperty("targetWSDLURL"),
					cloudConnection.getConnectionProperties());

			String userName = cloudConnection.getAuthenticationScheme()
					.getAuthenticationProperties().getProperty("username");

			String password = cloudConnection.getAuthenticationScheme()
					.getAuthenticationProperties().getProperty("password");

			if (userName == null) {
				userName = cloudConnection.getConnectionProperties()
						.getProperty("username");
			}

			if (password == null) {
				password = cloudConnection.getConnectionProperties()
						.getProperty("password");
			}

			String UPFKey = userName + password;

			String prevUPFKey = context.getContextObject("upfKey").toString();

			long nextLoginTime = Long.parseLong(context.getContextObject(
					"sessionSecondsValid").toString());

			if ((!UPFKey.equals(prevUPFKey))
					|| (System.currentTimeMillis() > nextLoginTime)) {
				getcloudLoginResponse(cloudConnection, context);
			}
		}
	}

	private static void populateContextWithNewSession(
			SOAPMessage loginResponseMessage, AdapterPluginContext context,
			Properties connectionProperties, CloudConnection cloudConnection)
			throws CloudApplicationAdapterException {
		NodeList passwordExpiredNode = null;
		NodeList serverUrlNode = null;
		NodeList sessionIdNode = null;
		NodeList organizationIdNode = null;
		NodeList sessionSecondsValidNode = null;

		long sessionEstablishedTime = System.currentTimeMillis();
		String sessionId = "";
		String serverURL = "";
		String organizationId = "";
		String sessionExpiredValue = "";

		try {
			passwordExpiredNode = loginResponseMessage.getSOAPBody()
					.getElementsByTagName("passwordExpired");

			if (passwordExpiredNode != null) {
				sessionExpiredValue = passwordExpiredNode.item(0)
						.getChildNodes().item(0).getNodeValue();

				if (CloudUtil.returnBooleanValue(sessionExpiredValue)) {
					throw new CloudApplicationAdapterException(
							"PASSWORD_EXPIRED");
				}
			}

			serverUrlNode = loginResponseMessage.getSOAPBody()
					.getElementsByTagName("serverUrl");

			sessionIdNode = loginResponseMessage.getSOAPBody()
					.getElementsByTagName("sessionId");

			sessionSecondsValidNode = loginResponseMessage.getSOAPBody()
					.getElementsByTagName("sessionSecondsValid");

			organizationIdNode = loginResponseMessage.getSOAPBody()
					.getElementsByTagName("organizationId");

			if (sessionIdNode != null) {
				sessionId = sessionIdNode.item(0).getChildNodes().item(0)
						.getNodeValue();

				context.setContextObject("sessionId", sessionId);

				serverURL = serverUrlNode.item(0).getChildNodes().item(0)
						.getNodeValue();

				context.setContextObject("serverUrl", serverURL);

				String sessionSecondsValid = sessionSecondsValidNode.item(0)
						.getChildNodes().item(0).getNodeValue();

				long nextLoginTime = sessionEstablishedTime
						+ Integer.parseInt(sessionSecondsValid) * 1000;

				context.setContextObject("sessionSecondsValid",
						Long.valueOf(nextLoginTime));

				organizationId = organizationIdNode.item(0).getChildNodes()
						.item(0).getNodeValue();

				context.setContextObject("organizationId", organizationId);

				String wsdlChecksum = CloudUtil.getChecksum(
						connectionProperties.getProperty("targetWSDLURL"),
						cloudConnection.getConnectionProperties());

				String userName = cloudConnection.getAuthenticationScheme()
						.getAuthenticationProperties().getProperty("username");

				String password = cloudConnection.getAuthenticationScheme()
						.getAuthenticationProperties().getProperty("password");

				if (userName == null) {
					userName = cloudConnection.getConnectionProperties()
							.getProperty("username");
				}

				if (password == null) {
					password = cloudConnection.getConnectionProperties()
							.getProperty("password");
				}

				String UPFKey = userName + password;

				context.setContextObject("upfKey", UPFKey);
			}
		} catch (SOAPException e) {
			throw new CloudApplicationAdapterException(e);
		}
	}

	public static SOAPMessage getcloudLoginResponse(
			CloudConnection cloudConnection, AdapterPluginContext context)
			throws CloudApplicationAdapterException {
		SOAPMessage loginRequestMessage = null;
		SOAPMessage loginResponseMessage = null;
		String loginFaultStatusCode = null;
		Properties connectionProperties = cloudConnection
				.getConnectionProperties();

		SOAPHelperService soapHelper = (SOAPHelperService) context
				.getServiceRegistry().getService(SOAPHelperService.class);

		WSDLHelperService wsdlHelper = (WSDLHelperService) context
				.getServiceRegistry().getService(WSDLHelperService.class);

		String wsdlURL = connectionProperties.getProperty("targetWSDLURL");

		URL endpointURL = null;

		try {
			endpointURL = wsdlHelper.getEndpointAddressFromWSDL(
					new URL(wsdlURL), connectionProperties);

			if (endpointURL.toString().contains("/c/")) {

				context.setContextObject("WSDL", "enterprise");
			} else {
				context.setContextObject("WSDL", "partner");
			}
		} catch (AdapterPluginServiceException apse) {
			throw new CloudApplicationAdapterException(apse);
		} catch (MalformedURLException murle) {
			throw new CloudApplicationAdapterException(murle);
		}

		boolean isEnterprise = context.getContextObject("WSDL").toString()
				.equals("enterprise");

		loginRequestMessage = CloudUtil.createLoginSoapMessage(cloudConnection,
				isEnterprise);

		try {
			loginResponseMessage = soapHelper.sendSOAP(loginRequestMessage,
					connectionProperties, endpointURL);

			SOAPFault soapFault = null;
			soapFault = loginResponseMessage.getSOAPBody().getFault();
			if (soapFault != null) {
				if (soapFault.getFaultCode().contains(":")) {
					String statusCode = soapFault.getFaultCode().substring(
							soapFault.getFaultCode().indexOf(":") + 1,
							soapFault.getFaultCode().length());
					loginFaultStatusCode = statusCode;
				} else {
					loginFaultStatusCode = soapFault.getFaultCode();
				}
				if (loginFaultStatusCode != null) {
					throw new CloudApplicationAdapterException(
							loginFaultStatusCode, null);
				}
			} else {
				populateContextWithNewSession(loginResponseMessage, context,
						connectionProperties, cloudConnection);
			}

		} catch (AdapterPluginServiceException apse) {
			if (apse.getMessage().toLowerCase()
					.contains("unable to find valid certification path")) {
				throw new CloudApplicationAdapterException("CERT_NOT_FOUND",
						apse);
			}

			if (loginResponseMessage == null) {
				throw new CloudApplicationAdapterException("HOST_NOT_FOUND",
						apse);
			}

			throw new CloudApplicationAdapterException(apse);
		} catch (SOAPException soape) {
			throw new CloudApplicationAdapterException(soape);
		}
		return loginResponseMessage;
	}
}