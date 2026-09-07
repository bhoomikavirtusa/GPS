package com.wiley.permissions.domain.persistence.permissions;

import com.wiley.permissions.common.utils.ValidateException;

/**
 * Later this may actually be mapped to a database table.
 * For now it's not. Just controls the values for WTENTR.Permission_Type.
 *
 * @author smarkoff
 */
public class PermissionType {
	public static final PermissionType
	    ONE_TIME_USE = new PermissionType("One Time Use - Permission Form"),
	    REUSE_FORM = new PermissionType("Permission Form"),
	    REUSE_PO = new PermissionType("Purchase Order"),
	 	PREFERRED_VENDOR = new PermissionType("Preferred Vendor");

    public static final PermissionType [] VALID_VALUES = {
    	ONE_TIME_USE, REUSE_FORM, REUSE_PO, PREFERRED_VENDOR
    };

    /**
     * Throws a ValidateException if the value is not valid.
     *
     * @throws ValidateException
     */
    public static void validate(String code) throws ValidateException {
        if (code == null) {
        	throw new ValidateException("Permission Type may not be null.");
        }

        for (PermissionType valid: VALID_VALUES) {
        	if (code.equals(valid.getCode()))  return;
        }

        throw new ValidateException(
            "Permission Type of [" + code + "] is not valid.");
    }


    private final String code;
    private final String description;

    private PermissionType(String codeAndDescription) {
    	this.code = codeAndDescription;
    	this.description = codeAndDescription;
    }

    private PermissionType(String code, String description) {
    	this.code = code;
    	this.description = description;
    }

    public String getCode() {
    	return code;
    }

    public String getDescription() {
    	return description;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PermissionType other = (PermissionType) obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;
		return true;
	}
}
