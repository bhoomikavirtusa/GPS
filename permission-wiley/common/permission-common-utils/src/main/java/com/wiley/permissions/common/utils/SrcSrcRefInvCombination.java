package com.wiley.permissions.common.utils;

public class SrcSrcRefInvCombination {


	private String srcRefNo;
	private String srcName;
	private String invNo;
	public SrcSrcRefInvCombination(String srcName, String srcRefNo, String invNo) {
		this.srcRefNo = srcRefNo;
		this.srcName = srcName;
		this.invNo = invNo;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((invNo == null) ? 0 : invNo.hashCode());
		result = prime * result + ((srcName == null) ? 0 : srcName.hashCode());
		result = prime * result + ((srcRefNo == null) ? 0 : srcRefNo.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SrcSrcRefInvCombination other = (SrcSrcRefInvCombination) obj;
		if (invNo == null) {
			if (other.invNo != null)
				return false;
		} else if (!invNo.equals(other.invNo))
			return false;
		if (srcName == null) {
			if (other.srcName != null)
				return false;
		} else if (!srcName.equals(other.srcName))
			return false;
		if (srcRefNo == null) {
			if (other.srcRefNo != null)
				return false;
		} else if (!srcRefNo.equals(other.srcRefNo))
			return false;
		return true;
	}

	public String getSrcRefNo() {
		return srcRefNo;
	}
	public String getSrcName() {
		return srcName;
	}
	public String getInvNo() {
		return invNo;
	}
	public void setSrcRefNo(String srcRefNo) {
		this.srcRefNo = srcRefNo;
	}
	public void setSrcName(String srcName) {
		this.srcName = srcName;
	}
	public void setInvNo(String invNo) {
		this.invNo = invNo;
	}


}
