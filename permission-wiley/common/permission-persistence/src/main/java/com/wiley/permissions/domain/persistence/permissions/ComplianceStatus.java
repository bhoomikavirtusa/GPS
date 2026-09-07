package com.wiley.permissions.domain.persistence.permissions;

/**
 *
 * @author smarkoff
 */
public enum ComplianceStatus {
	// Don't use underscores in codes because affects Lucene search
	NOT_STARTED ("notStarted"),
	IN_PROCESS ("inProcess"),
	COMPLETE ("complete"),
	COMPLETE_NO_3RD_PARTY ("completeNo3rdParty"),
	PROBLEM ("problem");  // special computed status that can not be chosen by user and not stored in DB

	private String code;

	private ComplianceStatus(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

    public static ComplianceStatus normalizedCodeToValue(String s) {
    	if (s == null)  return null;
    	else if (ComplianceStatus.NOT_STARTED.getCode().equalsIgnoreCase(s))  return NOT_STARTED;
    	else if (ComplianceStatus.IN_PROCESS.getCode().equalsIgnoreCase(s))  return IN_PROCESS;
    	else if (ComplianceStatus.COMPLETE.getCode().equalsIgnoreCase(s))  return COMPLETE;
    	else if (ComplianceStatus.COMPLETE_NO_3RD_PARTY.getCode().equalsIgnoreCase(s))  return COMPLETE_NO_3RD_PARTY;
    	else if (ComplianceStatus.PROBLEM.getCode().equalsIgnoreCase(s))  return PROBLEM;
    	else {
    		throw new RuntimeException("unexpected code: " + s);
    	}
    }
}
