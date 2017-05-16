package com.cognizant.ipm.adapter.runtime;
import java.util.ListResourceBundle;

public class ConnectorResourceBundle extends ListResourceBundle {
	protected Object[][] getContents() {
		return contents;
	}

	static final Object[][] contents = {
			{ "DESCRIBE_GLOBAL_FAILED",
					"Failed to get objects list from describeGlobal call." },
			{ "SELECTED_OBJECTS_LIST_EMPTY", "Selected objects list is empty." },
			{ "INVALID_ID",
					"Invalid id. Id does not belong to the selected object." },
			{ "OPERATION_NOT_SUCCESSFUL",
					"Error Message in response. Operation not successful." },
			{ "UNABLE_TO_FIND_SERVICE", "Unable to find service in target WSDL" },
			{ "UNABLE_TO_FIND_PORT", "Unable to find a port in service" },
			{ "NO_URL_FOUND_FOR_TARGET_WSDL", "No url found for target WSDL" },
			{ "QUERYMORE_INVOCATION_FAILED",
					"Error while invoking queryMore operation." },
			{ "WRONG_CONNECTION_TYPE", "Incorrect connection type." },
			{ "INVALID_SESSION", "Invalid Session." },
			{ "MISSING_QUERY_STRING",
					"Missing Query Statement in jca file. SOQL statements cannot be empty or null." },
			{
					"MISSING_SEARCH_STRING",
					"Missing Search Statement in jca file. SOSL statements cannot be empty or null." },
			{ "BIND_VARIABLES_VALUE_MISSING",
					"Bind Parameters' values are missing." },
			{ "BIND_VARIABLES_VALUE_EMPTY", "Bind Parameter's value is empty." },
			{ "UNABLE_TO_FIND_OPERATION_NAME",
					"Unable to find operation Name in integration WSDL." },
			{ "UNABLE_TO_GET_USERNAME",
					"Unable to find username in credential store." },
			{ "UNABLE_TO_GET_PASSWORD", "Unable to get password." },
			{ "UNKNOWN_EXCEPTION_REQUEST",
					"Unknown exception while transforming request message." },
			{ "UNKNOWN_EXCEPTION_RESPONSE",
					"Unknown exception while transforming response message." },
			{ "PARSER_CONFIGURATION_ERROR_LOGIN",
					"Error while creating login Request Message." },
			{ "PARSER_CONFIGURATION_ERROR_DESCRIBE_GLOBAL",
					"Error while creating describeGlobal Request Message." },
			{ "LOGIN_FAILED", "Login Failed. Exception:\n {0}" },
			{ "UNABLE_TO_FIND_ENDPOINT_URL_PROPERTY",
					"Unable to find endpoint URL property." },
			{ "UNABLE_TO_ACTIVATE_FLOW",
					"Unable to activate the flow. Exception:{0}" },
			{ "HANDSHAKE_FAILED", "Failed to handshake." },
			{ "ERRORS_DETECTED_IN_RESPONSE", "ERRORS_DETECTED_IN_RESPONSE" },
			{ "EMPTY_SOAP_RESPONSE_ERROR",
					"Unable to get response from zuora server." },
			{ "MISSING_SESSIONID_IN_OUTBOUND_MESSAGE_ERROR",
					"Unable to find sessionId in Outbound Message." },
			{ "SESSIONID_MISMATCH_IN_OUTBOUND_MESSAGE_ERROR",
					"Unexpected session Id Received in Outbound Message." },
			{ "ORGID_MISMATCH_IN_OUTBOUND_MESSAGE_ERROR",
					"Unexpected Organization Id received in Outbound Message." } };
}