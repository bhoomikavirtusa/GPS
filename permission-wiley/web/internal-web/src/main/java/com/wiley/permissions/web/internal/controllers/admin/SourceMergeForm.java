package com.wiley.permissions.web.internal.controllers.admin;



public class SourceMergeForm {

	private String sourceFrom;
	private String sourceTo;
	private int fromSourceId = -1;
	private int toSourceId = -1;

	public int getFromSourceId()
	{
		return fromSourceId;
	}

	public void setFromSourceId(int fromSourceId)
	{
		this.fromSourceId = fromSourceId;
	}


	public int getToSourceId()
	{
		return toSourceId;
	}

	public void setToSourceId(int toSourceId)
	{
		this.toSourceId = toSourceId;
	}

	public String getSourceFrom()
	{
		return sourceFrom;
	}

	public void setSourceFrom(String sourceFrom)
	{
		this.sourceFrom = sourceFrom;
	}

	public String getSourceTo()
	{
		return sourceTo;
	}

	public void setSourceTo(String sourceTo)
	{
		this.sourceTo = sourceTo;
	}

}
