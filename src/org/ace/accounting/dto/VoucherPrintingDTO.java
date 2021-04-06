package org.ace.accounting.dto;

import java.io.Serializable;
import java.util.List;

public class VoucherPrintingDTO implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String voucherNo;
	private List<VPdto> vpDtoList;
	
	public String getVoucherNo() {
		return voucherNo;
	}
	public void setVoucherNo(String voucherNo) {
		this.voucherNo = voucherNo;
	}
	public List<VPdto> getVpDtoList() {
		return vpDtoList;
	}
	public void setVpDtoList(List<VPdto> vpDtoList) {
		this.vpDtoList = vpDtoList;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((voucherNo == null) ? 0 : voucherNo.hashCode());
		result = prime * result + ((vpDtoList == null) ? 0 : vpDtoList.hashCode());
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
		VoucherPrintingDTO other = (VoucherPrintingDTO) obj;
		if (voucherNo == null) {
			if (other.voucherNo != null)
				return false;
		} else if (!voucherNo.equals(other.voucherNo))
			return false;
		if (vpDtoList == null) {
			if (other.vpDtoList != null)
				return false;
		} else if (!vpDtoList.equals(other.vpDtoList))
			return false;
		return true;
	}

}
