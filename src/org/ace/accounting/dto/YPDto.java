package org.ace.accounting.dto;

public class YPDto {
	private String id;
	private String acCode;
	private String acName;

	public YPDto() {
	}
	
	public YPDto(String id, String acCode, String acName) {
		this.id = id;
		this.acCode = acCode;
		this.acName = acName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAcCode() {
		return acCode;
	}

	public void setAcCode(String acCode) {
		this.acCode = acCode;
	}

	public String getAcName() {
		return acName;
	}

	public void setAcName(String acName) {
		this.acName = acName;
	}

}
