package org.ace.accounting.posting.persistence;

import java.util.Arrays;
import java.util.Date;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.ace.accounting.common.utils.BusinessUtil;
import org.ace.accounting.common.utils.DateUtils;
import org.ace.accounting.dto.YPDto;
import org.ace.accounting.posting.persistence.interfaces.IYearlyPostingDAO;
import org.ace.accounting.system.chartaccount.AccountType;
import org.ace.accounting.system.setup.persistence.interfaces.ISetupDAO;
import org.ace.java.component.persistence.BasicDAO;
import org.ace.java.component.persistence.exception.DAOException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository("YearlyPostingDAO")
public class YearlyPostingDAO extends BasicDAO implements IYearlyPostingDAO {

	@Resource(name = "SetupDAO")
	private ISetupDAO setupDAO;

	@Transactional(propagation = Propagation.REQUIRED)
	public void moveTlfToHistory(Date postingDate) throws DAOException {
		try {
			insertTLFHISTByPostingDate(postingDate);
			deleteTLFByPostingDate(postingDate);
		} catch (PersistenceException pe) {
			throw translate("Failed to move Tlf To History By PostingDate", pe);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void insertTLFHISTByPostingDate(Date postingDate) throws DAOException {
		StringBuffer sf = new StringBuffer("INSERT INTO TLF_HIST(ID, CUSTOMERID, ccoaid, HOMEAMOUNT, LOCALAMOUNT, CURRENCYID, ");
		sf.append(" CHEQUENO, BANKID, PURCHASEORDERID, LOANID, NARRATION, TRANTYPEID, DEPARTMENTID, ORGNTLFID, BRANCHID, RATE,");
		sf.append(" SETTLEMENTDATE, REVERSE, REFERENCENO, REFERENCETYPE, ENO, PAID, TLFNO, CLEARING, ISRENEWAL, ");
		sf.append(" VERSION,CREATEDDATE,CREATEDUSERID,UPDATEDDATE,UPDATEDUSERID)  (SELECT ID, CUSTOMERID, CCOAID, HOMEAMOUNT, LOCALAMOUNT,");
		sf.append(" CURRENCYID, CHEQUENO, BANKID, PURCHASEORDERID, LOANID, NARRATION, TRANTYPEID, DEPARTMENTID, ORGNTLFID, BRANCHID, RATE,");
		sf.append(" SETTLEMENTDATE, REVERSE, REFERENCENO, REFERENCETYPE, ENO, PAID, TLFNO, CLEARING, ISRENEWAL, ");
		sf.append(" VERSION,CREATEDDATE,CREATEDUSERID,UPDATEDDATE,UPDATEDUSERID FROM TLF t WHERE t.SETTLEMENTDATE <= ?1 )");
		try {
			Query q = em.createNativeQuery(sf.toString());
			q.setParameter(1, postingDate);
			q.executeUpdate();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to insertTLFHIST By PostingDate", pe);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteTLFByPostingDate(Date postingDate) throws DAOException {
		StringBuffer sf = new StringBuffer("DELETE FROM TLF T WHERE t.settlementDate <= :postingDate");
		try {
			Query q = em.createQuery(sf.toString());
			q.setParameter("postingDate", postingDate);
			q.executeUpdate();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to deleteTLF By PostingDate", pe);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void moveCcoaToHistory(String budgetYear, YPDto tAc, YPDto plAc) throws DAOException {
		try {
			insertCcoaHistory();
			updateCcoaBal();
			updateCcoaBalByTacAndPlAcAndACTYPE(tAc, plAc);
			updateCcoaAllZeroAndBudgetYear(budgetYear);
		} catch (PersistenceException pe) {
			throw translate("Failed to moveCcoaToHistory by Yearly Posting", pe);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void insertCcoaHistory() throws DAOException {
		StringBuffer sf = new StringBuffer("INSERT INTO PREV_CCOA(ID, ACNAME, ACCRUED, BF, BUDGET, CBAL, HOBAL, OBAL, SCCRBAL, VERSION, CREATEDDATE,");
		sf.append(" CREATEDUSERID,  UPDATEDDATE, UPDATEDUSERID, BF1, BF10, BF11, BF12, BF13, BF2, BF3 ,BF4 ,BF5 ,BF6 ,BF7 ,BF8 ,BF9 , ");
		sf.append(" BFSRC1 ,BFSRC10 , BFSRC11 , BFSRC12 , BFSRC13 , BFSRC2 , BFSRC3 , BFSRC4 , BFSRC5 , BFSRC6 , BFSRC7 ,  BFSRC8 , BFSRC9 , ");
		sf.append("LYMSRC1 , LYMSRC10 , LYMSRC11 , LYMSRC12 , LYMSRC13 , LYMSRC2 , LYMSRC3 , LYMSRC4 ,  LYMSRC5 , LYMSRC6 , LYMSRC7 , LYMSRC8 ,");
		sf.append(" LYMSRC9 , M1 , M10 , M11 , M12 , M13 , M2 , M3 , M4 , M5 , M6 , M7 ,  M8 , M9 , MREV1 , MREV10 , MREV11 , MREV12 , MREV13 , MREV2");
		sf.append(" , MREV3 , MREV4 , MREV5 , MREV6 , MREV7 , MREV8 , MREV9 ,  MSRC1 , MSRC10 , MSRC11 , MSRC12 , MSRC13 , MSRC2 , MSRC3 , MSRC4 , ");
		sf.append("MSRC5 , MSRC6 , MSRC7 , MSRC8 , MSRC9 , COAID , BRANCHID ,  CURRENCYID , DEPARTMENTID)   (SELECT  ID, ACNAME, ACCRUED, BF, BUDGET, ");
		sf.append("CBAL, HOBAL, OBAL, SCCRBAL, VERSION, CREATEDDATE, CREATEDUSERID,  UPDATEDDATE, UPDATEDUSERID, BF1, BF10, BF11, BF12, BF13, BF2, BF3 ,");
		sf.append("BF4 ,BF5 ,BF6 ,BF7 ,BF8 ,BF9 ,  BFSRC1 ,BFSRC10 , BFSRC11 , BFSRC12 , BFSRC13 , BFSRC2 , BFSRC3 , BFSRC4 , BFSRC5 , BFSRC6 , BFSRC7 ,");
		sf.append("  BFSRC8 , BFSRC9 , LYMSRC1 , LYMSRC10 , LYMSRC11 , LYMSRC12 , LYMSRC13 , LYMSRC2 , LYMSRC3 , LYMSRC4 ,  LYMSRC5 , LYMSRC6 , LYMSRC7 , ");
		sf.append("LYMSRC8 , LYMSRC9 , M1 , M10 , M11 , M12 , 0 , M2 , M3 , M4 , M5 , M6 , M7 ,  M8 , M9 , MREV1 , MREV10 , MREV11 , MREV12 , MREV13 , MREV2 , ");
		sf.append("MREV3 , MREV4 , MREV5 , MREV6 , MREV7 , MREV8 , MREV9 ,  MSRC1 , MSRC10 , MSRC11 , MSRC12 , 0 , MSRC2 , MSRC3 , MSRC4 , MSRC5 , MSRC6 , MSRC7 , ");
		sf.append("MSRC8 , MSRC9 , COAID , BRANCHID ,  CURRENCYID , DEPARTMENTID FROM CCOA  )");
		try {
			Query q = em.createNativeQuery(sf.toString());
			q.executeUpdate();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to insertCcoaHistory by Yearly Posting", pe);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateCcoaBal() throws DAOException {
		StringBuffer sf = new StringBuffer("UPDATE CurrencyChartOfAccount c SET c.monthlyRate.m1= c.monthlyRate.m12 , c.msrcRate.msrc1=c.msrcRate.msrc12, "
				+ " c.cBal= c.monthlyRate.m12, c.oBal = c.monthlyRate.m12, c.hOBal = c.monthlyRate.m12");
		try {
			Query q = em.createQuery(sf.toString());
			q.executeUpdate();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to updateCcoaBal by Yearly Posting", pe);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateCcoaBalByTacAndPlAcAndACTYPE(YPDto tAc, YPDto plAc) throws DAOException {
		StringBuffer sf = new StringBuffer("UPDATE CurrencyChartOfAccount c SET c.monthlyRate.m1= 0, c.msrcRate.msrc1=0 ,c.cBal=0, c.oBal=0,");
		sf.append(" c.hOBal=0 WHERE (c.coa.acCodeType=org.ace.accounting.system.chartaccount.AccountCodeType.DETAIL AND c.coa.groupId!=null AND ");
		sf.append("c.coa.groupId IN :groupIDList ) OR (c.coa.acType in :acTypeList)");
		try {
			Query q = em.createQuery(sf.toString());
			q.setParameter("groupIDList", Arrays.asList(tAc.getId(), plAc.getId()));
			q.setParameter("acTypeList", Arrays.asList(AccountType.E, AccountType.I));
			q.executeUpdate();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to updateCcoaBalByTacAndPlAcAndACTYPE by Yearly Posting", pe);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateCcoaAllZeroAndBudgetYear(String budgetYear) throws DAOException {
		StringBuffer sf = new StringBuffer("UPDATE CurrencyChartOfAccount c set c.bF=0, c.monthlyRate.m2=0, c.monthlyRate.m3=0, c.monthlyRate.m4=0,");
		sf.append(" c.monthlyRate.m5=0, c.monthlyRate.m6=0, c.monthlyRate.m7=0, c.monthlyRate.m8=0,   c.monthlyRate.m9=0, c.monthlyRate.m10=0, ");
		sf.append("c.monthlyRate.m11=0, c.monthlyRate.m12=0, c.monthlyRate.m13=0,  c.msrcRate.msrc2=0 , c.msrcRate.msrc3=0, c.msrcRate.msrc4=0, ");
		sf.append("c.msrcRate.msrc5=0, c.msrcRate.msrc6=0, c.msrcRate.msrc7=0, c.msrcRate.msrc8=0,  c.msrcRate.msrc9=0, c.msrcRate.msrc10=0,");
		sf.append(" c.msrcRate.msrc11=0, c.msrcRate.msrc12=0, c.msrcRate.msrc13=0,  c.bfRate.bf1=0,c.bfRate.bf2=0,c.bfRate.bf3=0,c.bfRate.bf4=0,");
		sf.append("c.bfRate.bf5=0,c.bfRate.bf6=0,c.bfRate.bf7=0,c.bfRate.bf8=0,c.bfRate.bf9=0,  c.bfRate.bf10=0,c.bfRate.bf11=0,");
		sf.append("c.bfRate.bf12=0,c.bfRate.bf13=0, c.bfsrcRate.bfsrc1=0, c.bfsrcRate.bfsrc2=0, c.bfsrcRate.bfsrc3=0, c.bfsrcRate.bfsrc4=0,");
		sf.append(" c.bfsrcRate.bfsrc5=0, c.bfsrcRate.bfsrc6=0, c.bfsrcRate.bfsrc7=0,  c.bfsrcRate.bfsrc8=0, c.bfsrcRate.bfsrc9=0, ");
		sf.append("c.bfsrcRate.bfsrc10=0, c.bfsrcRate.bfsrc11=0, c.bfsrcRate.bfsrc12=0, c.bfsrcRate.bfsrc13=0,  c.mrevRate.mrev1=0, ");
		sf.append("c.mrevRate.mrev2=0, c.mrevRate.mrev3=0, c.mrevRate.mrev4=0, c.mrevRate.mrev5=0, c.mrevRate.mrev6=0, c.mrevRate.mrev7=0, ");
		sf.append("c.mrevRate.mrev8=0,   c.mrevRate.mrev9=0, c.mrevRate.mrev10=0, c.mrevRate.mrev11=0, c.mrevRate.mrev12=0, c.mrevRate.mrev13=0, ");
		sf.append("  c.lymsrcRate.lymsrc1=0, c.lymsrcRate.lymsrc2=0, c.lymsrcRate.lymsrc3=0, c.lymsrcRate.lymsrc4=0, c.lymsrcRate.lymsrc5=0, ");
		sf.append("c.lymsrcRate.lymsrc6=0,   c.lymsrcRate.lymsrc7=0, c.lymsrcRate.lymsrc8=0, c.lymsrcRate.lymsrc9=0, c.lymsrcRate.lymsrc10=0,");
		sf.append(" c.lymsrcRate.lymsrc11=0, c.lymsrcRate.lymsrc12=0,    c.lymsrcRate.lymsrc13=0, c.sccrBal=0, c.accrued=0,c.budget=:budgetYear");
		try {
			Query q = em.createQuery(sf.toString());
			q.setParameter("budgetYear", budgetYear);
			q.executeUpdate();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to updateCcoaAllZeroAndBudgetYear by Yearly Posting", pe);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void increaseBudgetYear() throws DAOException {
		try {
			Date budgetStartDate = DateUtils.formatStringToDate(setupDAO.findSetupValueByVariable(BusinessUtil.BUDGETSDATE));
			Date budgetEndDate = DateUtils.formatStringToDate(setupDAO.findSetupValueByVariable(BusinessUtil.BUDGETEDATE));
			budgetStartDate = DateUtils.plusYears(budgetStartDate, 1);
			budgetEndDate = DateUtils.plusYears(budgetEndDate, 1);

			setupDAO.updateSetupValueByVariable(BusinessUtil.BUDGETSDATE, DateUtils.formatDateToString(budgetStartDate));
			setupDAO.updateSetupValueByVariable(BusinessUtil.BUDGETEDATE, DateUtils.formatDateToString(budgetEndDate));
		} catch (PersistenceException pe) {
			throw translate("Failed to increaseBudgetYear by Yearly Posting", pe);
		}
	}

}
