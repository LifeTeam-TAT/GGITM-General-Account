package org.ace.accounting.web.transaction;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.ace.accounting.common.utils.BusinessUtil;
import org.ace.accounting.common.utils.DateUtils;
import org.ace.accounting.common.validation.MessageId;
import org.ace.accounting.dto.VoucherDTO;
import org.ace.accounting.process.interfaces.IUserProcessService;
import org.ace.accounting.system.chartaccount.CurrencyChartOfAccount;
import org.ace.accounting.system.currency.Currency;
import org.ace.accounting.system.currency.service.interfaces.ICurrencyService;
import org.ace.accounting.system.rateinfo.RateType;
import org.ace.accounting.system.tlf.service.interfaces.ITLFService;
import org.ace.accounting.system.trantype.TranCode;
import org.ace.accounting.system.trantype.service.interfaces.ITranTypeService;
import org.ace.java.component.SystemException;
import org.ace.java.web.common.BaseBean;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;

@ManagedBean(name = "CreditVoucherActionBean")
@ViewScoped
public class CreditVoucherActionBean extends BaseBean {

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

	private List<Currency> currencyList;
	private VoucherDTO voucherDto;
	private boolean admin;
	private Date todayDate;
	private Date minDate;

	@PostConstruct
	public void init() {
		createNewCreditVoucher();
		rebindData();
		minDate = BusinessUtil.getBackDate();
	}

	public void rebindData() {
		currencyList = currencyService.findAllCurrency();
	}

	public void createNewCreditVoucher() {
		voucherDto = new VoucherDTO();
		Currency homeCur = currencyService.findHomeCurrency();
		voucherDto.setCurrency(homeCur);
		voucherDto.setHomeExchangeRate(BigDecimal.ONE);
		todayDate = new Date();
		admin = userProcessService.getLoginUser().isAdmin();
		voucherDto.setCreatedUserId(userProcessService.getLoginUser().getId());
		voucherDto.setSettlementDate(new Date());
	}

	public void addVoucher() {
		try {
			if (voucherDto.getHomeExchangeRate().doubleValue() > 0) {
				String voucherNo = tlfService.addVoucher(voucherDto, TranCode.CSCREDIT);
				addInfoMessage(null, MessageId.INSERT_SUCCESS, voucherNo);
				createNewCreditVoucher();
			} else {
				addErrorMessage(null, MessageId.NO_EXCHANGE_RATE,
						DateUtils.formatDateToString(null != voucherDto.getSettlementDate() ? voucherDto.getSettlementDate() : new Date()));
			}
		} catch (SystemException e) {
			handleSysException(e);
		}
	}

	public void changeRate() {
		// voucherDto.setCcoa(null);
		if (voucherDto.getCurrency() != null) {
			double exchangeRate = 0.0;
			if (null != voucherDto.getSettlementDate()) {
				exchangeRate = tlfService.getExchangeRate(voucherDto.getCurrency(), RateType.CS, voucherDto.getSettlementDate());
			} else {
				exchangeRate = tlfService.getExchangeRate(voucherDto.getCurrency(), RateType.CS, new Date());
			}
			voucherDto.setHomeExchangeRate(BigDecimal.valueOf(exchangeRate));
		}
		PrimeFaces.current().resetInputs("voucherForm:exchangeRate");
	}

	public void selectCCOAAccountCode() {
		selectCCOAAccountCode(voucherDto.getCurrency());
	}

	public void returnCCOAAccountCode(SelectEvent event) {
		CurrencyChartOfAccount ccoa = (CurrencyChartOfAccount) event.getObject();
		voucherDto.setCcoa(ccoa);
	}

	public List<Currency> getCurrencyList() {
		return currencyList;
	}

	public VoucherDTO getVoucherDto() {
		return voucherDto;
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

}
