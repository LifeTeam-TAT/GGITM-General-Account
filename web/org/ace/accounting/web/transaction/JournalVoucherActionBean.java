package org.ace.accounting.web.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.ace.accounting.common.utils.BusinessUtil;
import org.ace.accounting.common.utils.DateUtils;
import org.ace.accounting.common.validation.MessageId;
import org.ace.accounting.dto.EditVoucherDto;
import org.ace.accounting.dto.JVdto;
import org.ace.accounting.process.interfaces.IUserProcessService;
import org.ace.accounting.system.chartaccount.CurrencyChartOfAccount;
import org.ace.accounting.system.currency.Currency;
import org.ace.accounting.system.currency.service.interfaces.ICurrencyService;
import org.ace.accounting.system.rateinfo.service.interfaces.IRateInfoService;
import org.ace.accounting.system.tlf.service.interfaces.ITLFService;
import org.ace.accounting.system.trantype.TranCode;
import org.ace.java.component.SystemException;
import org.ace.java.web.common.BaseBean;
import org.ace.java.web.common.ParamId;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;

@ManagedBean(name = "JournalVoucherActionBean")
@ViewScoped
public class JournalVoucherActionBean extends BaseBean {

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

	@ManagedProperty(value = "#{RateInfoService}")
	private IRateInfoService rateInfoService;

	public void setRateInfoService(IRateInfoService rateInfoService) {
		this.rateInfoService = rateInfoService;
	}

	@ManagedProperty(value = "#{UserProcessService}")
	private IUserProcessService userProcessService;

	public void setUserProcessService(IUserProcessService userProcessService) {
		this.userProcessService = userProcessService;
	}

	private boolean createNew;
	private JVdto dto;
	private List<JVdto> dtoList;
	private Currency cur;
	private double exchangeRate;
	private int dtoIndex;
	private boolean admin;
	private Date todayDate;
	private Date minDate;
	
	private BigDecimal totalDebitAmount = BigDecimal.ZERO;
	private BigDecimal totalDebitHomeAmount = BigDecimal.ZERO;
	private BigDecimal totalCreditAmount = BigDecimal.ZERO;
	private BigDecimal totalCreditHomeAmount = BigDecimal.ZERO;
	
	private BigDecimal differentAmount = BigDecimal.ZERO;
	private BigDecimal differentHomeAmount = BigDecimal.ZERO;

	@PostConstruct
	public void init() {
		rebindData();
		createNewDto();
		minDate = BusinessUtil.getBackDate();
	}

	@PreDestroy
	public void destroy() {
		removeParam(ParamId.COA_DATA);
	}

	private void rebindData() {
		dtoList = new ArrayList<JVdto>();
	}

	public void prepareUpdateDto(JVdto dto) {
		dtoIndex = dtoList.indexOf(dto);
		this.dto = dto;
		createNew = false;
	}

	public void createNewDto() {
		createNew = true;
		dto = new JVdto();
		if (dtoList.size() > 0) {
			dto.setCur(cur);
			dto.setExchangeRate(BigDecimal.valueOf(exchangeRate));
			dto.setSettlementDate(dtoList.get(0).getSettlementDate());
		} else {
			dto.setCur(currencyService.findHomeCurrency());
			cur = dto.getCur();
			dto.setExchangeRate(BigDecimal.ONE);
		}
		todayDate = new Date();

		admin = userProcessService.getLoginUser().isAdmin();
		if (null == dto.getSettlementDate()) {
			dto.setSettlementDate(new Date());
		}

	}

	public void changeRate() {
		if (cur.getIsHomeCur()) {
			dto.setExchangeRate(BigDecimal.ONE);
		} else {
			exchangeRate = rateInfoService.findCurrentRateInfo(cur, dto.getSettlementDate());
			dto.setExchangeRate(BigDecimal.valueOf(exchangeRate));
		}
		dto.setCur(cur);
		dto.setCcoa(null);
		PrimeFaces.current().resetInputs("journalVoucherEntryForm:exchangeRate");
	}

	public void saveDtos() {
		if (validateDtoList()) {
			try {
				String voucherNo = tlfService.addNewTlfByDto(dtoList);
				addInfoMessage(null, MessageId.INSERT_SUCCESS, voucherNo);
				rebindData();
				createNewDto();
			} catch (SystemException ex) {
				handleSysException(ex);
			}
		}
	}

	public void addNewDto() {
		try {
			exchangeRate = dto.getExchangeRate().doubleValue();
			dto.setHomeAmount(dto.getAmount().multiply(dto.getExchangeRate()));
			dtoList.add(dto);
			calculatTotalAmount();
			calculateDifferentAmount();
			createNewDto();
		} catch (SystemException ex) {
			handleSysException(ex);
		}
	}
	
	private void calculatTotalAmount() {
		this.totalDebitAmount = BigDecimal.ZERO;
		this.totalDebitHomeAmount = BigDecimal.ZERO;
		
		this.totalCreditAmount = BigDecimal.ZERO;
		this.totalCreditHomeAmount = BigDecimal.ZERO;
		
		for (JVdto dto : dtoList) {
			if (dto.getTranCode().equals(TranCode.CSCREDIT)
					|| dto.getTranCode().equals(TranCode.TRCREDIT)) {
				this.totalCreditHomeAmount = totalCreditHomeAmount.add(dto.getHomeAmount()).setScale(2,RoundingMode.HALF_UP);
				this.totalCreditAmount = totalCreditAmount.add(dto.getAmount()).setScale(2,RoundingMode.HALF_UP);
			} else {
				this.totalDebitHomeAmount = totalDebitHomeAmount.add(dto.getHomeAmount()).setScale(2,RoundingMode.HALF_UP);
				this.totalDebitAmount = totalDebitAmount.add(dto.getAmount()).setScale(2,RoundingMode.HALF_UP);
			}
		}
	}
	
	private void calculateDifferentAmount() {
		this.differentAmount = BigDecimal.ZERO;
		this.differentHomeAmount = BigDecimal.ZERO;
		
		this.differentAmount = totalDebitAmount.subtract(totalCreditAmount).setScale(2,RoundingMode.HALF_UP);
		this.differentHomeAmount = totalDebitHomeAmount.subtract(totalCreditHomeAmount).setScale(2,RoundingMode.HALF_UP);
	}

	public void updateDto() {
		try {
			exchangeRate = dto.getExchangeRate().doubleValue();
			dto.setHomeAmount(dto.getAmount().multiply(dto.getExchangeRate()));
			dtoList.set(dtoIndex, dto);
			calculatTotalAmount();
			calculateDifferentAmount();
			createNewDto();
		} catch (SystemException ex) {
			handleSysException(ex);
		}
	}

	public String deleteDto(JVdto dto) {
		try {
			dtoList.remove(dto);
			createNewDto();
		} catch (SystemException ex) {
			handleSysException(ex);
		}
		return null;
	}

	private boolean validateDtoList() {
		BigDecimal totalDebit = BigDecimal.ZERO;
		BigDecimal totalCredit = BigDecimal.ZERO;
		if (cur != null) {
			for (JVdto dto : dtoList) {
				if (dto.getTranCode().equals(TranCode.TRCREDIT)) {
					totalCredit = totalCredit.add(dto.getAmount());
				} else if (dto.getTranCode().equals(TranCode.TRDEBIT)) {
					totalDebit = totalDebit.add(dto.getAmount());
				}
			}
			totalCredit.doubleValue();
			totalCredit.setScale(4);
			totalDebit.setScale(4);
			if (totalCredit.compareTo(totalDebit) == 0 
					&& dtoList.stream().allMatch(dto -> DateUtils.resetStartDate(dtoList.get(0).getSettlementDate()).equals(DateUtils.resetStartDate(dto.getSettlementDate())))) {
				return true;
			}
		}
		addErrorMessage(null, MessageId.AMOUNT_DATE_INBALANCE);
		return false;
	}

	public void setCreateNew(boolean createNew) {
		this.createNew = createNew;
	}

	public boolean isCreateNew() {
		return createNew;
	}

	public List<Currency> getCurs() {
		return currencyService.findAllCurrency();
	}

	public void returnCcoa(SelectEvent event) {
		CurrencyChartOfAccount ccoa = (CurrencyChartOfAccount) event.getObject();
		dto.setCcoa(ccoa);
	}

	public JVdto getDto() {
		return dto;
	}

	public void setDto(JVdto dto) {
		this.dto = dto;
	}

	public List<JVdto> getDtoList() {
		return dtoList;
	}

	public void setDtoList(List<JVdto> dtoList) {
		this.dtoList = dtoList;
	}

	public List<TranCode> getCodes() {
		List<TranCode> list = new ArrayList<TranCode>();
		list.add(TranCode.TRCREDIT);
		list.add(TranCode.TRDEBIT);
		return list;
	}

	public Currency getCur() {
		return cur;
	}

	public void setCur(Currency cur) {
		this.cur = cur;
	}

	public List<JVdto> filteredList;

	public List<JVdto> getFilteredList() {
		return filteredList;
	}

	public void setFilteredList(List<JVdto> filteredList) {
		this.filteredList = filteredList;
	}

	public void selectCCOAAccountCode() {
		selectCCOAAccountCode(dto.getCur());
	}

	public boolean isCurDisabled() {
		return dtoList.size() != 0 ? true : false;
	}

	public boolean isAdmin() {
		return admin;
	}

	public Date getTodayDate() {
		return todayDate;
	}

	public Date getMinDate() {
		return minDate;
	}

	public BigDecimal getTotalDebitAmount() {
		return totalDebitAmount;
	}

	public void setTotalDebitAmount(BigDecimal totalDebitAmount) {
		this.totalDebitAmount = totalDebitAmount;
	}

	public BigDecimal getTotalDebitHomeAmount() {
		return totalDebitHomeAmount;
	}

	public void setTotalDebitHomeAmount(BigDecimal totalDebitHomeAmount) {
		this.totalDebitHomeAmount = totalDebitHomeAmount;
	}

	public BigDecimal getTotalCreditAmount() {
		return totalCreditAmount;
	}

	public void setTotalCreditAmount(BigDecimal totalCreditAmount) {
		this.totalCreditAmount = totalCreditAmount;
	}

	public BigDecimal getTotalCreditHomeAmount() {
		return totalCreditHomeAmount;
	}

	public void setTotalCreditHomeAmount(BigDecimal totalCreditHomeAmount) {
		this.totalCreditHomeAmount = totalCreditHomeAmount;
	}

	public BigDecimal getDifferentAmount() {
		return differentAmount;
	}

	public void setDifferentAmount(BigDecimal differentAmount) {
		this.differentAmount = differentAmount;
	}

	public BigDecimal getDifferentHomeAmount() {
		return differentHomeAmount;
	}

	public void setDifferentHomeAmount(BigDecimal differentHomeAmount) {
		this.differentHomeAmount = differentHomeAmount;
	}
	
	

}
