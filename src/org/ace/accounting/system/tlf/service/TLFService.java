package org.ace.accounting.system.tlf.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.NoResultException;

import org.ace.accounting.common.BasicEntity;
import org.ace.accounting.common.SystemConstants;
import org.ace.accounting.common.VoucherType;
import org.ace.accounting.common.utils.DateUtils;
import org.ace.accounting.dto.EditVoucherDto;
import org.ace.accounting.dto.GainAndLossDTO;
import org.ace.accounting.dto.JVdto;
import org.ace.accounting.dto.RateDTO;
import org.ace.accounting.dto.VoucherDTO;
import org.ace.accounting.process.interfaces.IUserProcessService;
import org.ace.accounting.report.balancesheet.BalanceSheetDTO;
import org.ace.accounting.system.branch.Branch;
import org.ace.accounting.system.coasetup.COASetup;
import org.ace.accounting.system.coasetup.persistence.interfaces.ICOASetupDAO;
import org.ace.accounting.system.currency.Currency;
import org.ace.accounting.system.rateinfo.RateType;
import org.ace.accounting.system.rateinfo.persistence.interfaces.IRateInfoDAO;
import org.ace.accounting.system.tlf.TLF;
import org.ace.accounting.system.tlf.persistence.interfaces.ITLFDAO;
import org.ace.accounting.system.tlf.service.interfaces.ITLFService;
import org.ace.accounting.system.tlfhist.persistence.interfaces.ITLFHISTDAO;
import org.ace.accounting.system.trantype.TranCode;
import org.ace.accounting.system.trantype.TranType;
import org.ace.accounting.system.trantype.persistence.interfaces.ITranTypeDAO;
import org.ace.java.component.SystemException;
import org.ace.java.component.idgen.service.interfaces.ICustomIDGenerator;
import org.ace.java.component.persistence.exception.DAOException;
import org.ace.java.component.service.BaseService;
import org.ace.java.component.service.interfaces.IDataRepService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "TLFService")
public class TLFService extends BaseService implements ITLFService {

	@Resource(name = "TLFDAO")
	private ITLFDAO tlfDao;

	@Resource(name = "DataRepService")
	private IDataRepService<TLF> dataRepService;

	@Resource(name = "RateInfoDAO")
	private IRateInfoDAO rateInfoDAO;

	@Resource(name = "COASetupDAO")
	private ICOASetupDAO coaSetupDAO;

	@Resource(name = "TLFHISTDAO")
	private ITLFHISTDAO tlfHistoryDAO;

	@Resource(name = "TranTypeDAO")
	private ITranTypeDAO tranTypeDAO;

	@Resource(name = "CustomIDGenerator")
	private ICustomIDGenerator customIDGenerator;

	@Resource(name = "UserProcessService")
	private IUserProcessService userProcessService;

	@Transactional(propagation = Propagation.REQUIRED)
	public List<TLF> findAllTLF() {
		List<TLF> result = null;
		try {
			result = tlfDao.findAll();
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to find all of TLF)", e);
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<RateDTO> findRateTLF(RateDTO ratedto) {
		List<RateDTO> result = null;
		try {
			result = tlfDao.findRateList(ratedto);
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to find all of TLF)", e);
		}
		return result;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public List<GainAndLossDTO> findGainAndLosttList(GainAndLossDTO dto) {
		List<GainAndLossDTO> result = new ArrayList<>();
		try {
			result = tlfDao.findGainAndLossList(dto);
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to find all of Gain & Loss List)", e);
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public String getVoucherNo() {
		String voucherNo = null;
		voucherNo = customIDGenerator.getNextId(SystemConstants.VOUCHER_NO, null);
		return voucherNo;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<String> findVoucherNoByBranchIdAndVoucherType(String branchId, VoucherType voucherType) {
		List<String> result = null;
		try {
			result = tlfDao.findVoucherNoByBranchIdAndVoucherType(branchId, voucherType);
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to Find VoucherNoList by " + branchId, e);
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public String getVoucherNoForVocherTypes(TranCode tranCode) {
		String voucherNo = null;
		String voucherCodeprefix = "";
		if (TranCode.CSCREDIT.equals(tranCode)) {
			voucherCodeprefix = "P";
		} else if (TranCode.CSDEBIT.equals(tranCode)) {
			voucherCodeprefix = "R";
		} else {
			voucherCodeprefix = "J";
		}
		voucherNo = customIDGenerator.getNextId(SystemConstants.VOUCHER_NO, null, voucherCodeprefix);
		return voucherNo;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<TLF> findVoucherListByReverseZero(String voucherNo) {
		List<TLF> result = null;
		try {
			result = tlfDao.findVoucherListByReverseZero(voucherNo);
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to Find VoucherListByReverseZero", e);
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public double getExchangeRate(Currency currency, RateType rateType, Date date) throws SystemException {
		double exchangeRate = 0.0;
		try {
			// RateInfo result = null;
			if (currency.getIsHomeCur()) {
				exchangeRate = 1;
			} else {
				exchangeRate = rateInfoDAO.findExchangeRateBy(currency, rateType, DateUtils.formatDate(date));
				// exchangeRate = result == null ? 0 :
				// result.getExchangeRate().doubleValue();
			}
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to Find ExchangeRate", e);
		}
		return exchangeRate;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public COASetup findCOABy(String acName, Currency currency, Branch branch) {
		COASetup result = coaSetupDAO.findCOAByACNameAndCur(acName, currency, branch);
		return result;

	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addVoucher(List<TLF> tlfList) throws SystemException {
		// If Class_Authorizor != Globalizer.Class_Authorizor
		// show error message ==>
		try {
			for (TLF tlf : tlfList) {
				dataRepService.insert(tlf);
			}
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to add a Voucher", e);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public String addVoucher(VoucherDTO voucherDto, TranCode tranCode) throws SystemException {
		try {
			Date settlementDate = null;
			Date createdDate = null;
			if (null != voucherDto.getSettlementDate()) {
				settlementDate = voucherDto.getSettlementDate();
				createdDate = voucherDto.getSettlementDate();
			} else {
				settlementDate = new Date();
			}

			TranType tranType = null;
			boolean reverse = false;
			boolean paid = true;
			String referenceType = null;
			List<TLF> tlfList = new ArrayList<TLF>();
			TLF tlf = null;
			Branch branch = null;
			COASetup coaSetup = null;
			String voucherNo = getVoucherNoForVocherTypes(tranCode);
			branch = userProcessService.getLoginUser().getBranch();
			// .round(new MathContext(6))
			voucherDto.setHomeAmount(voucherDto.getAmount().multiply(voucherDto.getHomeExchangeRate()));
			voucherDto.setLocalAmount(voucherDto.getAmount());

			// credit account
			tranType = tranTypeDAO.findByTransCode(tranCode);
			tlf = new TLF(voucherDto, voucherNo, tranType, branch, reverse, paid, referenceType, settlementDate, createdDate, voucherDto.getCreatedUserId());
			tlfList.add(tlf);

			// cash account
			coaSetup = coaSetupDAO.findCOAByACNameAndCur("CASH", voucherDto.getCurrency(), branch);
			tranCode = (tranCode.equals(TranCode.CSCREDIT)) ? TranCode.CSDEBIT : TranCode.CSCREDIT;
			tranType = tranTypeDAO.findByTransCode(tranCode);
			voucherDto.setCcoa(coaSetup.getCcoa());
			tlf = new TLF(voucherDto, voucherNo, tranType, branch, reverse, paid, referenceType, settlementDate, createdDate, voucherDto.getCreatedUserId());
			tlfList.add(tlf);

			addVoucher(tlfList);
			return voucherNo;
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to add voucher", e);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public TranType findCashAccountByVoucherNo(String voucherNo) {
		TranType result = null;
		try {
			result = tlfDao.findCashAccountByVoucherNo(voucherNo);
		} catch (NoResultException e) {
			return null;
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to Find Cash Account", e);
		}
		return result;

	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateVoucher(List<TLF> oldVoucherList, List<EditVoucherDto> voucherList, VoucherType voucherType) {
		try {
			prepareSaveVoucher(voucherList, oldVoucherList.get(0).getCurrency(), voucherList.get(0).getSettlementDate());
			reverseVoucher(oldVoucherList, true, voucherType);
			List<TLF> tlfList = new ArrayList<TLF>();
			for (EditVoucherDto dto : voucherList) {
				TLF tlf = new TLF(dto);
				tlfList.add(tlf);
			}
			addVoucher(tlfList);
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to add voucher", e);
		}
	}

	public void prepareSaveVoucher(List<EditVoucherDto> voucherList, Currency currency, Date settlementDate) {
		for (EditVoucherDto dto : voucherList) {
			BigDecimal homeExchangeRate = BigDecimal.valueOf(getExchangeRate(currency, RateType.CS, settlementDate));
			BigDecimal localAmount = dto.getLocalAmount();
			BigDecimal homeAmount = localAmount.multiply(homeExchangeRate);
			dto.setRate(homeExchangeRate);
			dto.setHomeAmount(homeAmount);
			dto.setSettlementDate(settlementDate);
			dto.setReverse(false);
			dto.setPaid(true);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void reverseVoucher(List<TLF> oldList, boolean reverse, VoucherType voucherType) {
		try {
			if (voucherType.equals(VoucherType.JOURNAL)) {
				for (TLF tlf : oldList) {
					tlfDao.updateReverseByID(tlf.getId(), reverse);
					TLF reverseVoucher = new TLF(tlf.getCcoa(), tlf.geteNo(), tlf.getHomeAmount(), tlf.getLocalAmount(), tlf.getNarration(), tlf.getTranType(), tlf.getCurrency(),
							tlf.getRate(), tlf.getBranch(), reverse, tlf.getSettlementDate(), tlf.getChequeNo());
					if (tlf.getTranType().getTranCode().equals(TranCode.TRDEBIT)) {
						reverseVoucher.setTranType(tranTypeDAO.findByTransCode(TranCode.TRCREDIT));
					} else {
						reverseVoucher.setTranType(tranTypeDAO.findByTransCode(TranCode.TRDEBIT));
					}
					dataRepService.insert(reverseVoucher);
				}
			} else {
				for (TLF tlf : oldList) {
					tlfDao.updateReverseByID(tlf.getId(), reverse);
					TLF reverseVoucher = new TLF(tlf.getCcoa(), tlf.geteNo(), tlf.getHomeAmount(), tlf.getLocalAmount(), tlf.getNarration(), tlf.getTranType(), tlf.getCurrency(),
							tlf.getRate(), tlf.getBranch(), reverse, tlf.getSettlementDate(), tlf.getChequeNo());
					dataRepService.insert(reverseVoucher);
				}
			}

		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to reverse voucher", e);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public String addNewTlfByDto(List<JVdto> dtoList) {
		String voucherNo;
		Branch branch = null;
		String voucherCodePrefix = "J";
		try {
			voucherNo = customIDGenerator.getNextId(SystemConstants.VOUCHER_NO, null, voucherCodePrefix);
			// voucherNo =
			// customIDGenerator.getNextId(SystemConstants.VOUCHER_NO, null);
			for (JVdto dto : dtoList) {
				branch = userProcessService.getLoginUser().getBranch();
				TLF tlf = new TLF(dto);
				tlf.setTranType(tranTypeDAO.findByTransCode(dto.getTranCode()));
				tlf.seteNo(voucherNo);
				tlf.setBranch(branch);
				tlf.setPaid(true);
				BasicEntity basicEntity = new BasicEntity();
				basicEntity.setCreatedDate(dto.getSettlementDate());
				basicEntity.setCreatedUserId(userProcessService.getLoginUser().getId());
				tlf.setBasicEntity(basicEntity);
				dataRepService.insert(tlf);
			}
		} catch (DAOException e) {
			throw new SystemException(e.getErrorCode(), "Failed to add TLF by jvdto)", e);
		}
		return voucherNo;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<BalanceSheetDTO> generateBalanceSheet(String branchId, String currencyId, boolean isHomeCurrency) throws SystemException {
		try {
			return tlfDao.generateBalanceSheet(branchId, currencyId, isHomeCurrency);
		} catch (DAOException de) {
			throw new SystemException(de.getErrorCode(), "Failed to add Balance Sheet Data)", de);
		}

	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<BalanceSheetDTO> generateBalanceSheetByDate(String branchId, String currencyId, Date fromDate, Date toDate, boolean isHomeCurrency) throws SystemException {
		try {
			return tlfDao.generateBalanceSheetByDate(branchId, currencyId, fromDate, toDate, isHomeCurrency);
		} catch (DAOException de) {
			throw new SystemException(de.getErrorCode(), "Failed to add Balance Sheet Data)", de);
		}

	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<BalanceSheetDTO> generateBalanceSheetByGroup() throws SystemException {
		try {
			return tlfDao.generateBalanceSheetByGroup();
		} catch (DAOException de) {
			throw new SystemException(de.getErrorCode(), "Failed to add Balance Sheet Data)", de);
		}

	}

}