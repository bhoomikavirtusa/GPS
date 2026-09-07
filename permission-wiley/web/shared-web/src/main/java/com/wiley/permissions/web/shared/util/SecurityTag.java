package com.wiley.permissions.web.shared.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

import com.wiley.permissions.security.web.UserPrincipal;

/* lnagy - NOT USED ANYMORE - use security:authorize instead */
public class SecurityTag extends BodyTagSupport {

	private static final long serialVersionUID = 1L;

	private String code;

	@Override
	public int doStartTag() throws JspException {
		UserPrincipal up = (UserPrincipal) ((HttpServletRequest)pageContext.getRequest()).getUserPrincipal();

	    if (up.hasPrivilege(code)) {
	        return Tag.EVAL_BODY_INCLUDE;
	    } else {
	        return Tag.SKIP_BODY;
	    }
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
