package com.wiley.permissions.validator.rules.base;

/**
 * The Class Operation.
 */
public class Operation {

	private String name = null;

	public Operation() {
		super();

	}

	public Operation(String name) {
		super();
		this.name = name;
	}

	/*public Operation(OperationTypeEnum operationTypeEnum) {
		this.name = operationTypeEnum.name();
	}*/

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString(){
		return name;
	}
}
