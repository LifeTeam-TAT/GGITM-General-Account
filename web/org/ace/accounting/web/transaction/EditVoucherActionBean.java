package org.ace.accounting.web.transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.ace.accounting.common.VoucherType;
import org.ace.accounting.common.utils.BusinessUtil;
import org.ace.accounting.common.validation.MessageId;
import org.ace.accounting.dto.EditVoucherDto;
import org.ace.accounting.process.interfaces.IUserProcessService;
import org.ace.accounting.system.branch.Branch;
import org.ace.accounting.system.chartaccount.CurrencyChartOfAccount;
import org.ace.accounting.system.coasetup.service.interfaces.ICOASetupService;
import org.ace.accounting.system.currency.Currency;
import org.ace.accounting.system.currency.service.interfaces.ICurrencyService;
import org.ace.accounting.system.tlf.TLF;
import org.ace.accounting.system.tlf.service.interfaces.ITLFService;
import org.ace.accounting.system.trantype.TranCode;
import org.ace.accounting.system.trantype.TranType;
import org.ace.accounting.system.trantype.service.interfaces.ITranTypeService;
import org.ace.java.component.SystemException;
import org.ace.java.web.common.BaseBean;
import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.event.SelectEvent;

@ManagedBean(name = "ManageEditVoucherActionBean")
@ViewScoped
public class EditVoucherActionBean extends BaseBean {
	// Note ==> find all the cash account and
	// in journal compare if any account are match with cashaccount coasetup
	// ccoaid
	// that account is cash
	// if all the cash account's must have one cheque no
	@ManagedProperty(value = "#{CurrencyService}")
	private ICurrencyService currencyService;

	public void setCurrencyService(ICurrencyService currencyService) {
		this.currencyService = currencyService;
	}

	@ManagedProperty(value = "#{TLFService}")
	private ITLFService tlfService;

	public void setTlfService(ITLFService tlfService) {
		this.tlfService = tlfService;
	}

	@ManagedProperty(value = "#{UserProcessService}")
	private IUserProcessService userProcessService;

	public void setUserProcessService(IUserProcessService userProcessService) {
		this.userProcessService = userProcessService;
	}

	@ManagedProperty(value = "#{TranTypeService}")
	private ITranTypeService tranTypeService;

	public void setTranTypeService(ITranTypeService tranTypeService) {
		this.tranTypeService = tranTypeService;
	}

	@ManagedProperty(value = "#{COASetupService}")
	private ICOASetupService coaSetupService;

	public void setCoaSetupService(ICOASetupService coaSetupService) {
		this.coaSetupService = coaSetupService;
	}
	

	public final String formID = "voucherEditForm";
	private VoucherType voucherType;
	private VoucherType editedVoucherType;
	private Date settlementDate;
	private String voucherNo;
	private List<Currency> currencyList;
	private Currency currency;
	private List<String> voucherNoList;
	private List<EditVoucherDto> voucherList;
	private List<TLF> oldVoucherList;
	private EditVoucherDto editVoucherDto;
	boolean createNew;
	boolean editedData;
	boolean deleteAll;
	boolean deleteCurrent;
	boolean cashVoucher;
	boolean selectVoucherNo;
	private Branch branch;
	private TranType cashAccount;
	// private List<COASetup> cashAccountList;
	private boolean cashAccountDisable;

	private Date todayDate;
	private Date minDate;
	private boolean admin;

	
	@PostConstruct
	public void init() {
		
		// cashAccountList = coaSetupService.findAllCashAccount();
		createNewEditVoucher();
		voucherType = VoucherType.CASH;
		rebindData();
		todayDate = new Date();
		admin = userProcessService.getLoginUser().isAdmin();
		minDate = BusinessUtil.getBackDate();
	}
	

	public void rebindData() {
		currencyList = currencyService.findAllCurrency();
		branch = userProcessService.getLoginUser().getBranch();
		voucherNoList = tlfService.findVoucherNoByBranchIdAndVoucherType(branch.getId(), voucherType);
	}

	public void createNewEditVoucher() {
		selectVoucherNo = false;
		currency = null;
		voucherNo = null;
		branch = new Branch();
		currencyList = new ArrayList<Currency>();
		// voucherNoList = new ArrayList<String>();
		voucherList = new ArrayList<EditVoucherDto>();
		oldVoucherList = new ArrayList<TLF>();
		cashVoucher = true;
		editedVoucherType = VoucherType.DEBIT;
		settlementDate = new Date();
		editVoucherDto = new EditVoucherDto();
		createNew = false;
		editedData = false;
		deleteAll = false;
		deleteCurrent = false;
		cashAccountDisable = false;
	}

	public void createNewVoucher() {
		createNew = true;
		editedData = false;
		editVoucherDto = new EditVoucherDto();
	}

	public void addNewVoucher() {
		TranType tranType = new TranType();
		branch = userProcessService.getLoginUser().getBranch();
		if (validateEditedVoucher()) {
			createNew = false;
			if (voucherType.equals(VoucherType.JOURNAL)) {
				if (editedVoucherType.equals(VoucherType.DEBIT)) {
					tranType = tranTypeService.findByTransCode(TranCode.TRDEBIT);
				} else {
					tranType = tranTypeService.findByTransCode(TranCode.TRCREDIT);
				}
			} else {
				if (editedVoucherType.equals(VoucherType.DEBIT)) {
					tranType = tranTypeService.findByTransCode(TranCode.CSDEBIT);
				} else {
					tranType = tranTypeService.findByTransCode(TranCode.CSCREDIT);

				}
			}
			editVoucherDto.setTranType(tranType);
			editVoucherDto.seteNo(voucherNo);
			editVoucherDto.setCurrency(currency);
			editVoucherDto.setReverse(false);
			editVoucherDto.setBranch(branch);
			voucherList.add(editVoucherDto);
			createNew = false;
			editedData = false;
			editVoucherDto = new EditVoucherDto();
		}
	}

	public void updateVoucher() {
		TranType tranType = null;
		if (validateEditedVoucher()) {
			createNew = false;
			if (voucherType.equals(VoucherType.JOURNAL)) {
				if (editedVoucherType.equals(VoucherType.DEBIT)) {
					tranType = tranTypeService.findByTransCode(TranCode.TRDEBIT);
				} else {
					tranType = tranTypeService.findByTransCode(TranCode.TRCREDIT);
				}
			} else {
				if (editedVoucherType.equals(VoucherType.DEBIT)) {
					tranType = tranTypeService.findByTransCode(TranCode.CSDEBIT);
				} else {
					tranType = tranTypeService.findByTransCode(TranCode.CSCREDIT);
				}
			}
			editVoucherDto.setTranType(tranType);
			editVoucherDto.setCurrency(currency);
			cancelNewVoucher();
		}

	}

	public void cancelNewVoucher() {
		createNew = false;
		editedData = false;
		editVoucherDto = new EditVoucherDto();
		cashAccountDisable = false;
	}

	public void saveVoucher() {
		try {
			if (validateVoucher()) {

				voucherList.forEach(voucher -> {
					voucher.setSettlementDate(settlementDate);
					voucher.setRate(oldVoucherList.get(0).getRate());
				});
				tlfService.updateVoucher(oldVoucherList, voucherList, voucherType);
				addInfoMessage(null, MessageId.INSERT_SUCCESS, voucherNo);
				createNewEditVoucher();
				rebindData();
			}
		} catch (SystemException e) {
			handleSysException(e);
		}

	}

	public void changeVoucherType() {
		resetDataTable();
		createNewEditVoucher();
		Branch branch = userProcessService.getLoginUser().getBranch();
		voucherNoList = new ArrayList<String>();
		voucherNoList = tlfService.findVoucherNoByBranchIdAndVoucherType(branch.getId(), voucherType);
		cashVoucher = voucherType.equals(VoucherType.CASH);
		// PrimeFaces.current().executeScript("PF('voucherNoTable').clearFilters();");

	}

	public void resetDataTable() {
		DataTable dataTable = (DataTable) FacesContext.getCurrentInstance().getViewRoot().findComponent("voucherNoDialogForm:voucherNoTable");
		dataTable.reset();
		PrimeFaces.current().ajax().update("voucherNoDialogForm:voucherNoTable");

		/*
		 * if(!dataTable.getFilters().isEmpty()) { dataTable.reset();
		 * 
		 * PrimeFaces.current().ajax().update(
		 * "voucherNoDialogForm:voucherNoTable"); }
		 */
	}

	public void prepareEditVoucher(EditVoucherDto dto) {
		createNew = false;
		editedData = true;
		this.editVoucherDto = dto;
		editedVoucherType = (dto.getTranType().getTranCode().equals(TranCode.CSDEBIT) || dto.getTranType().getTranCode().equals(TranCode.TRDEBIT)) ? VoucherType.DEBIT
				: VoucherType.CREDIT;
		cashAccountDisable = dto.getTranType().equals(cashAccount);
		// COASetup journalCashAccount = cashAccountList.stream().filter(p->
		// p.getCcoa().equals(dto.getCcoa())).findFirst().get();
	}

	public void deleteCurrentVoucher() {
		voucherList.remove(editVoucherDto);
		cancelNewVoucher();
	}

	public void deleteAllVoucher() {
		try {
			tlfService.reverseVoucher(oldVoucherList, true, voucherType);
			addInfoMessage(null, MessageId.DELETE_SUCCESS, "All The Voucher");
			createNewEditVoucher();
			rebindData();
		} catch (SystemException e) {
			handleSysException(e);
		}

	}

	public boolean validateVoucher() {
		boolean valid = true;
		if (!isDebitAmountEqualCreditAmount()) {
			valid = false;
			addInfoMessage(null, MessageId.DEBIT_CREDIT_BALANCE, voucherType);
		}
		if (voucherList.isEmpty() || voucherList == null) {
			valid = false;
			addInfoMessage(null, MessageId.NO_DATA_TOSAVE);
		}
		if (voucherType.equals(VoucherType.JOURNAL)) {

		}

		return valid;
	}

	public boolean validateEditedVoucher() {
		boolean valid = true;
		if (editVoucherDto.getCcoa() == null) {
			addErrorMessage(formID + ":accountCode", MessageId.REQUIRED, "Account Code");
			valid = false;
		}
		if (editVoucherDto.getLocalAmount() == null || editVoucherDto.getLocalAmount().doubleValue() <= 0) {
			addErrorMessage(formID + ":amount", MessageId.REQUIRED, "Amount");
			valid = false;
		}

		if (editVoucherDto.getNarration() == null || editVoucherDto.getNarration().isEmpty()) {
			addErrorMessage(formID + ":narration", MessageId.REQUIRED, "Narration");
			valid = false;
		}
		return valid;
	}

	public boolean isDebitAmountEqualCreditAmount() {
		BigDecimal debitAmount = BigDecimal.ZERO;
		BigDecimal creditAmount = BigDecimal.ZERO;
		for (EditVoucherDto dto : voucherList) {
			if (dto.getTranType().getTranCode().equals(TranCode.CSCREDIT) || dto.getTranType().getTranCode().equals(TranCode.TRCREDIT)) {
				creditAmount = creditAmount.add(dto.getLocalAmount());
			} else {
				debitAmount =debitAmount.add(dto.getLocalAmount());
			}
		}
		return debitAmount.compareTo(creditAmount) == 0;
	}

	public void selectVoucherNo(String voucherNo) {
		oldVoucherList = new ArrayList<TLF>();
		voucherList = new ArrayList<EditVoucherDto>();

		this.voucherNo = voucherNo;
		oldVoucherList = tlfService.findVoucherListByReverseZero(this.voucherNo);
		if (!oldVoucherList.isEmpty()) {

			this.currency = oldVoucherList.get(0).getCurrency();
			this.settlementDate = oldVoucherList.get(0).getSettlementDate();
			if (voucherType.equals(VoucherType.CASH)) {
				cashAccount = tlfService.findCashAccountByVoucherNo(this.voucherNo);
			}
			for (TLF tlf : oldVoucherList) {
				EditVoucherDto dto = new EditVoucherDto(tlf.getCcoa(), tlf.geteNo(), tlf.getHomeAmount(), tlf.getLocalAmount(), tlf.getNarration(), tlf.getTranType(),
						tlf.getCurrency(), tlf.getRate(), tlf.getBranch(), tlf.isReverse(), tlf.getSettlementDate(), tlf.getChequeNo());
				voucherList.add(dto);
			}
			createNew = voucherType.equals(VoucherType.JOURNAL);
			editVoucherDto = new EditVoucherDto();
			selectVoucherNo = true;
		} else {
			addErrorMessage(null, MessageId.NO_DATA_TOEDIT);
			editVoucherDto = new EditVoucherDto();
			voucherNo = null;
			selectVoucherNo = false;
		}
	}

	public boolean isEditedData() {
		return editedData;
	}

	public void setEditedData(boolean editedData) {
		this.editedData = editedData;
	}

	public boolean isDeleteAll() {
		return deleteAll;
	}

	public void setDeleteAll(boolean deleteAll) {
		this.deleteAll = deleteAll;
	}

	public boolean isDeleteCurrent() {
		return deleteCurrent;
	}

	public void setDeleteCurrent(boolean deleteCurrent) {
		this.deleteCurrent = deleteCurrent;
	}

	public EditVoucherDto getEditVoucherDto() {
		return editVoucherDto;
	}

	public void setEditVoucherDto(EditVoucherDto editVoucherDto) {
		this.editVoucherDto = editVoucherDto;
	}

	public boolean isCreateNew() {
		return createNew;
	}

	public void setCreateNew(boolean createNew) {
		this.createNew = createNew;
	}

	public List<EditVoucherDto> getVoucherList() {
		return voucherList;
	}

	public List<String> getVoucherNoList() {
		return voucherNoList;
	}

	public VoucherType[] getVoucherTypeList() {
		VoucherType[] vtype = { VoucherType.CASH, VoucherType.JOURNAL };
		return vtype;
	}

	public VoucherType[] getEditedVoucherTypeList() {
		VoucherType[] vtype = { VoucherType.DEBIT, VoucherType.CREDIT };
		return vtype;
	}

	public VoucherType getEditedVoucherType() {
		return editedVoucherType;
	}

	public void setEditedVoucherType(VoucherType editedVoucherType) {
		this.editedVoucherType = editedVoucherType;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public List<Currency> getCurrencyList() {
		return currencyList;
	}

	public String getVoucherNo() {
		return voucherNo;
	}

	public void setVoucherNo(String voucherNo) {
		this.voucherNo = voucherNo;
	}

	public VoucherType getVoucherType() {
		return voucherType;
	}

	public void setVoucherType(VoucherType voucherType) {
		this.voucherType = voucherType;
	}

	public Date getSettlementDate() {
		return settlementDate;
	}

	public void setSettlementDate(Date settlementDate) {
		this.settlementDate = settlementDate;
	}

	private List<TLF> filterVnoList;

	public List<TLF> getFilterVnoList() {
		return filterVnoList;
	}

	public void setFilterVnoList(List<TLF> filterVnoList) {
		this.filterVnoList = filterVnoList;
	}

	public void selectCCOAAccountCode() {
		selectCCOAAccountCode(this.currency);
	}

	public void returnCCOAAccountCode(SelectEvent event) {
		CurrencyChartOfAccount ccoa = (CurrencyChartOfAccount) event.getObject();
		editVoucherDto.setCcoa(ccoa);
	}

	public void handleCloseDeleteDialog() {
		deleteAll = false;
		deleteCurrent = false;
	}

	public boolean isCashVoucher() {
		return cashVoucher;
	}

	public boolean isCashAccountDisable() {
		return cashAccountDisable;
	}

	public boolean isSelectVoucherNo() {
		return selectVoucherNo;
	}

	public Date getTodayDate() {
		return todayDate;
	}

	public Date getMinDate() {
		return minDate;
	}

	public boolean isAdmin() {
		return admin;
	}

}
