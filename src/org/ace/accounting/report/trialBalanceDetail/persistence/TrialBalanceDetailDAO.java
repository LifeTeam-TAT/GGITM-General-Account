package org.ace.accounting.report.trialBalanceDetail.persistence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.ace.accounting.common.CurrencyType;
import org.ace.accounting.common.utils.BusinessUtil;
import org.ace.accounting.dto.TrialBalanceCriteriaDto;
import org.ace.accounting.dto.TrialBalanceReportDto;
import org.ace.accounting.report.TrialBalanceAccountType;
import org.ace.accounting.report.trialBalanceDetail.persistence.interfaces.ITrialBalanceDetailDAO;
import org.ace.accounting.system.chartaccount.AccountType;
import org.ace.accounting.system.setup.persistence.interfaces.ISetupDAO;
import org.ace.java.component.persistence.BasicDAO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Aung
 *
 */

@Repository("TrialBalanceDetailDAO")
public class TrialBalanceDetailDAO extends BasicDAO implements ITrialBalanceDetailDAO {

	@Resource(name = "SetupDAO")
	private ISetupDAO setupDAO;

	@Override
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED)
	public List<TrialBalanceReportDto> findTrialBalanceDetailList(TrialBalanceCriteriaDto dto) {
		List<TrialBalanceReportDto> dtoList = null;
		try {

			Calendar cal = Calendar.getInstance();
			cal.set(dto.getRequiredYear(), dto.getRequiredMonth(), 1);
			Date reportDate = cal.getTime();
			String reportBudgetMonth = BusinessUtil.getBudgetInfo(reportDate, 3);
			String amountColumn = "";

			if (dto.getCurrencyType().equals(CurrencyType.HOMECURRENCY) || dto.isHomeCurrencyConverted()) {
				reportBudgetMonth = "MSRC" + reportBudgetMonth;
				amountColumn = "homeAmount";
			} else {
				reportBudgetMonth = "M" + reportBudgetMonth;
				amountColumn = "localAmount";
			}

			StringBuffer queryString = new StringBuffer("SELECT ACODE,ACNAME,ACTYPE,SUM(mDEBIT),SUM(mCREDIT),SUM(DEBIT),SUM(CREDIT) FROM (");

			// for TLF
			queryString.append("SELECT C.ACTYPE AS ACTYPE, CC.DEPARTMENTID");
			if (dto.isGroup()) {
				if (dto.getAccountType().equals(TrialBalanceAccountType.Gl_ACODE)) {
					queryString.append(" , CASE WHEN ACTYPE IN('A','L') THEN C.ACCODE ELSE ");
					queryString.append(" (SELECT ACCODE FROM COA C2 WHERE C2.ID=C.HEADID) END AS ACODE ");
				} else {
					queryString.append(" , C.IBSBACODE AS ACODE ");
				}
				queryString.append(" , CASE WHEN ACTYPE IN('A','L') THEN C.ACNAME ");
				queryString.append(" ELSE (SELECT ACNAME FROM COA C2 WHERE C2.ID = C.HEADID ) END AS ACNAME ");
			} else {
				if (dto.getAccountType().equals(TrialBalanceAccountType.Gl_ACODE)) {
					queryString.append(" , C.ACCODE AS ACODE ");
				} else {
					queryString.append(" , C.IBSBACODE AS ACODE ");
				}
				if (dto.getBranch() == null) {
					queryString.append(" , C.ACNAME AS ACNAME ");
				} else {
					queryString.append(" , CC.ACNAME AS ACNAME ");
				}
			}

			queryString.append(" , CASE WHEN (  T.TRANTYPEID ='2' OR T.TRANTYPEID ='3' )  THEN ABS(T." + amountColumn + ") ELSE 0 END AS mDEBIT ");
			queryString.append(" ,CASE WHEN (  T.TRANTYPEID ='1' OR T.TRANTYPEID ='4' )  THEN ABS(T." + amountColumn + ") ELSE 0 END AS mCREDIT,0 AS DEBIT,0 AS CREDIT");
			queryString.append(" FROM COA AS C ");
			queryString.append(" LEFT JOIN VW_CCOA AS CC ON C.ID=CC.COAID ");
			queryString.append(" LEFT JOIN TLF T ON CC.ID = T.CCOAID");
			queryString.append(" WHERE CC.BUDGET=?budget");
			queryString.append(" AND DATEPART(MONTH,T.SETTLEMENTDATE) =?month");
			if (dto.isGroup()) {
				queryString.append(" AND C.ACCODETYPE='GROUP' ");
			} else {
				queryString.append(" AND C.ACCODETYPE='DETAIL' ");
			}
			if (dto.getCurrencyType().equals(CurrencyType.SOURCECURRENCY) && dto.getCurrency() != null) {
				queryString.append(" AND CC.CURRENCYID=?currencyid ");
			}

			if (dto.getBranch() != null) {
				queryString.append(" AND CC.BRANCHID=?branchid ");
			}

			queryString.append("UNION ALL ");

			// FOR CCOA

			queryString.append("SELECT C.ACTYPE AS ACTYPE, CC.DEPARTMENTID  , C.ACCODE AS ACODE  , C.ACNAME AS ACNAME  ,0 AS mDEBIT,0 AS mCREDIT ");
			queryString.append(" ,CASE WHEN ( ");
			queryString.append(" (ACTYPE IN ('I','L')  AND CC." + reportBudgetMonth + " < 0 ) OR ");
			queryString.append(" (ACTYPE IN ('A','E')  AND CC." + reportBudgetMonth + " > 0 ) ");
			queryString.append(" ) THEN ABS(CC." + reportBudgetMonth + ") ELSE 0 END AS DEBIT ");
			queryString.append(" ,CASE WHEN ( ");
			queryString.append(" (ACTYPE IN ('I','L')  AND CC." + reportBudgetMonth + " > 0 ) OR ");
			queryString.append(" (ACTYPE IN ('A','E')  AND CC." + reportBudgetMonth + " < 0 ) ");
			queryString.append(" ) THEN ABS(CC." + reportBudgetMonth + ") ELSE 0 END AS CREDIT ");
			queryString.append(" FROM COA AS C INNER JOIN VW_CCOA AS CC ON C.ID=CC.COAID WHERE CC.BUDGET=?budget ");
			// queryString.append(" AND " + reportBudgetMonth + "<> 0 ");
			if (dto.isGroup()) {
				queryString.append(" AND C.ACCODETYPE='GROUP' ");
			} else {
				queryString.append(" AND C.ACCODETYPE='DETAIL' ");
			}

			if (dto.getCurrencyType().equals(CurrencyType.SOURCECURRENCY) && dto.getCurrency() != null) {
				queryString.append(" AND CC.CURRENCYID=?currencyid ");
			}

			if (dto.getBranch() != null) {
				queryString.append(" AND CC.BRANCHID=?branchid ");
			}
			/*
			 * 
			 * queryString.
			 * append(" SELECT C.ACTYPE AS ACTYPE, CC.DEPARTMENTID "); if
			 * (dto.isGroup()) { if
			 * (dto.getAccountType().equals(TrialBalanceAccountType.Gl_ACODE ))
			 * { queryString.
			 * append(" , CASE WHEN ACTYPE IN('A','L') THEN C.ACCODE ELSE " );
			 * queryString.
			 * append(" (SELECT ACCODE FROM COA C2 WHERE C2.ID=C.HEADID) END AS ACODE "
			 * ); } else { queryString.append(" , C.IBSBACODE AS ACODE "); }
			 * queryString.
			 * append(" , CASE WHEN ACTYPE IN('A','L') THEN C.ACNAME ");
			 * queryString.
			 * append(" ELSE (SELECT ACNAME FROM COA C2 WHERE C2.ID = C.HEADID ) END AS ACNAME "
			 * ); } else { if
			 * (dto.getAccountType().equals(TrialBalanceAccountType.Gl_ACODE ))
			 * { queryString.append(" , C.ACCODE AS ACODE "); } else {
			 * queryString.append(" , C.IBSBACODE AS ACODE "); } if
			 * (dto.getBranch() == null) {
			 * queryString.append(" , C.ACNAME AS ACNAME "); } else {
			 * queryString.append(" , CC.ACNAME AS ACNAME "); } } // FROM TLF
			 * queryString.append(" ,CASE WHEN ( "); queryString.
			 * append(" T.TRANTYPEID ='2' OR T.TRANTYPEID ='3' ) ");
			 * queryString.append(" THEN ABS(T." + amountColumn +
			 * ") ELSE 0 END AS mDEBIT "); queryString.append(" ,CASE WHEN ( ");
			 * queryString.
			 * append(" T.TRANTYPEID ='1' OR T.TRANTYPEID ='4' ) ");
			 * queryString.append(" THEN ABS(T." + amountColumn +
			 * ") ELSE 0 END AS mCREDIT ");
			 * 
			 * // FROM COA queryString.append(" ,CASE WHEN ( ");
			 * queryString.append(" (ACTYPE IN ('I','L')  AND CC." +
			 * reportBudgetMonth + " < 0 ) OR ");
			 * queryString.append(" (ACTYPE IN ('A','E')  AND CC." +
			 * reportBudgetMonth + " > 0 ) ");
			 * queryString.append(" ) THEN ABS(CC." + reportBudgetMonth +
			 * ") ELSE 0 END AS DEBIT "); queryString.append(" ,CASE WHEN ( ");
			 * queryString.append(" (ACTYPE IN ('I','L')  AND CC." +
			 * reportBudgetMonth + " > 0 ) OR ");
			 * queryString.append(" (ACTYPE IN ('A','E')  AND CC." +
			 * reportBudgetMonth + " < 0 ) ");
			 * queryString.append(" ) THEN ABS(CC." + reportBudgetMonth +
			 * ") ELSE 0 END AS CREDIT "); queryString.
			 * append(" FROM COA AS C INNER JOIN VW_CCOA AS CC ON C.ID=CC.COAID RIGHT JOIN TLF T ON CC.ID = T.CCOAID WHERE CC.BUDGET=?budget "
			 * ); queryString.
			 * append(" AND DATEPART(MONTH,T.SETTLEMENTDATE) =?month");
			 * queryString.append(" AND " + reportBudgetMonth + "<> 0 "); if
			 * (dto.isGroup()) {
			 * queryString.append(" AND C.ACCODETYPE='GROUP' "); } else {
			 * queryString.append(" AND C.ACCODETYPE='DETAIL' "); }
			 * 
			 * if (dto.getCurrencyType().equals(CurrencyType.SOURCECURRENCY) &&
			 * dto.getCurrency() != null) {
			 * queryString.append(" AND CC.CURRENCYID=?currencyid "); }
			 * 
			 * if (dto.getBranch() != null) {
			 * queryString.append(" AND CC.BRANCHID=?branchid "); }
			 */
			queryString.append(" ) T");
			queryString.append(" GROUP BY ACODE,ACNAME,ACTYPE ORDER BY ACODE");

			Query q = em.createNativeQuery(queryString.toString());

			q.setParameter("budget", BusinessUtil.getBudgetInfo(reportDate, 2));

			if (dto.getCurrencyType().equals(CurrencyType.SOURCECURRENCY) && dto.getCurrency() != null) {
				q.setParameter("currencyid", dto.getCurrency().getId());
			}
			if (dto.getBranch() != null) {
				q.setParameter("branchid", dto.getBranch().getId());
			}

			q.setParameter("month", (cal.get(Calendar.MONTH)) + 1);

			List<Object[]> objList = q.getResultList();
			if (objList != null) {
				dtoList = new ArrayList<>();
				for (Object[] obj : objList) {
					TrialBalanceReportDto reportDto = new TrialBalanceReportDto();
					reportDto.setAcode(obj[0].toString());
					reportDto.setAcname(obj[1].toString());
					reportDto.setAcType(AccountType.valueOf(obj[2].toString()));
					reportDto.setmDebit(new BigDecimal(obj[3].toString()));
					reportDto.setmCredit(new BigDecimal(obj[4].toString()));
					reportDto.setDebit(new BigDecimal(obj[5].toString()));
					reportDto.setCredit(new BigDecimal(obj[6].toString()));
					dtoList.add(reportDto);
				}
				for (TrialBalanceReportDto reportDto : dtoList) {
					if (reportDto.getAcType().equals(AccountType.A) || reportDto.getAcType().equals(AccountType.E)) {
						reportDto.setmDebit(reportDto.getmDebit().subtract(reportDto.getmCredit()));
						reportDto.setmCredit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
					} else if (reportDto.getAcType().equals(AccountType.I) || reportDto.getAcType().equals(AccountType.L)) {
						reportDto.setmCredit(reportDto.getmCredit().subtract(reportDto.getmDebit()));
						reportDto.setmDebit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
					}
				}
			}

		} catch (PersistenceException pe) {
			throw translate("Failed to findTrialBalanceDetailList", pe);
		}
		return dtoList;
	}

	@Override
	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED)
	public List<TrialBalanceReportDto> findTrialBalanceOldFormat(TrialBalanceCriteriaDto dto) {
		List<TrialBalanceReportDto> dtoList = null;
		try {

			Calendar cal = Calendar.getInstance();
			cal.set(dto.getRequiredYear(), dto.getRequiredMonth(), 1);
			Date reportDate = cal.getTime();
			String reportBudgetMonth = BusinessUtil.getBudgetInfo(reportDate, 3);
			String amountColumn = "";

			if (dto.getCurrencyType().equals(CurrencyType.HOMECURRENCY) || dto.isHomeCurrencyConverted()) {
				reportBudgetMonth = "MSRC" + reportBudgetMonth;
				amountColumn = "homeAmount";
			} else {
				reportBudgetMonth = "M" + reportBudgetMonth;
				amountColumn = "localAmount";
			}

			StringBuffer queryString = new StringBuffer("SELECT ACODE,ACNAME,ACTYPE,SUM(mDEBIT),SUM(mCREDIT),SUM(DEBIT),SUM(CREDIT) FROM (");

			queryString.append("SELECT C.ACTYPE AS ACTYPE, CC.DEPARTMENTID  , 0 AS mDEBIT,0 AS mCREDIT ");
			// for TLF
			// queryString.append("SELECT C.ACTYPE AS ACTYPE, CC.DEPARTMENTID");
			if (dto.isGroup()) {
				if (dto.getAccountType().equals(TrialBalanceAccountType.Gl_ACODE)) {
					queryString.append(" , CASE WHEN ACTYPE IN('A','L') THEN C.ACCODE ELSE ");
					queryString.append(" (SELECT ACCODE FROM COA C2 WHERE C2.ID=C.HEADID) END AS ACODE ");
				} else {
					queryString.append(" , C.IBSBACODE AS ACODE ");
				}
				queryString.append(" , CASE WHEN ACTYPE IN('A','L') THEN C.ACNAME ");
				queryString.append(" ELSE (SELECT ACNAME FROM COA C2 WHERE C2.ID = C.HEADID ) END AS ACNAME ");
			} else {
				if (dto.getAccountType().equals(TrialBalanceAccountType.Gl_ACODE)) {
					queryString.append(" , C.ACCODE AS ACODE ");
				} else {
					queryString.append(" , C.IBSBACODE AS ACODE ");
				}
				if (dto.getBranch() == null) {
					queryString.append(" , C.ACNAME AS ACNAME ");
				} else {
					queryString.append(" , CC.ACNAME AS ACNAME ");
				}
			}

			// queryString.append(" , CASE WHEN ( T.TRANTYPEID ='2' OR
			// T.TRANTYPEID ='3' ) THEN ABS(T." + amountColumn + ") ELSE 0 END
			// AS mDEBIT ");
			// queryString.append(" ,CASE WHEN ( T.TRANTYPEID ='1' OR
			// T.TRANTYPEID ='4' ) THEN ABS(T." + amountColumn + ") ELSE 0 END
			// AS mCREDIT,0 AS DEBIT,0 AS CREDIT");
			// queryString.append(" FROM COA AS C ");
			// queryString.append(" LEFT JOIN VW_CCOA AS CC ON C.ID=CC.COAID ");
			// queryString.append(" LEFT JOIN TLF T ON CC.ID = T.CCOAID");
			// queryString.append(" WHERE CC.BUDGET=?budget");
			// queryString.append(" AND DATEPART(MONTH,T.SETTLEMENTDATE)
			// =?month");
			// if (dto.isGroup()) {
			// queryString.append(" AND C.ACCODETYPE='GROUP' ");
			// } else {
			// queryString.append(" AND C.ACCODETYPE='DETAIL' ");
			// }
			// if (dto.getCurrencyType().equals(CurrencyType.SOURCECURRENCY) &&
			// dto.getCurrency() != null) {
			// queryString.append(" AND CC.CURRENCYID=?currencyid ");
			// }
			//
			// if (dto.getBranch() != null) {
			// queryString.append(" AND CC.BRANCHID=?branchid ");
			// }
			//
			// queryString.append("UNION ALL ");

			// FOR CCOA

			queryString.append(" ,CASE WHEN ( ");
			queryString.append(" (ACTYPE IN ('I','L')  AND CC." + reportBudgetMonth + " < 0 ) OR ");
			queryString.append(" (ACTYPE IN ('A','E')  AND CC." + reportBudgetMonth + " > 0 ) ");
			queryString.append(" ) THEN ABS(CC." + reportBudgetMonth + ") ELSE 0 END AS DEBIT ");
			queryString.append(" ,CASE WHEN ( ");
			queryString.append(" (ACTYPE IN ('I','L')  AND CC." + reportBudgetMonth + " > 0 ) OR ");
			queryString.append(" (ACTYPE IN ('A','E')  AND CC." + reportBudgetMonth + " < 0 ) ");
			queryString.append(" ) THEN ABS(CC." + reportBudgetMonth + ") ELSE 0 END AS CREDIT ");
			queryString.append(" FROM COA AS C INNER JOIN VW_CCOA AS CC ON C.ID=CC.COAID WHERE CC.BUDGET=?budget ");
			queryString.append(" AND " + reportBudgetMonth + "<> 0 ");
			if (dto.isGroup()) {
				queryString.append(" AND C.ACCODETYPE='GROUP' ");
			} else {
				queryString.append(" AND C.ACCODETYPE='DETAIL' ");
			}

			if (dto.getCurrencyType().equals(CurrencyType.SOURCECURRENCY) && dto.getCurrency() != null) {
				queryString.append(" AND CC.CURRENCYID=?currencyid ");
			}

			if (dto.getBranch() != null) {
				queryString.append(" AND CC.BRANCHID=?branchid ");
			}
			/*
			 * 
			 * queryString.
			 * append(" SELECT C.ACTYPE AS ACTYPE, CC.DEPARTMENTID "); if
			 * (dto.isGroup()) { if
			 * (dto.getAccountType().equals(TrialBalanceAccountType.Gl_ACODE ))
			 * { queryString.
			 * append(" , CASE WHEN ACTYPE IN('A','L') THEN C.ACCODE ELSE " );
			 * queryString.
			 * append(" (SELECT ACCODE FROM COA C2 WHERE C2.ID=C.HEADID) END AS ACODE "
			 * ); } else { queryString.append(" , C.IBSBACODE AS ACODE "); }
			 * queryString.
			 * append(" , CASE WHEN ACTYPE IN('A','L') THEN C.ACNAME ");
			 * queryString.
			 * append(" ELSE (SELECT ACNAME FROM COA C2 WHERE C2.ID = C.HEADID ) END AS ACNAME "
			 * ); } else { if
			 * (dto.getAccountType().equals(TrialBalanceAccountType.Gl_ACODE ))
			 * { queryString.append(" , C.ACCODE AS ACODE "); } else {
			 * queryString.append(" , C.IBSBACODE AS ACODE "); } if
			 * (dto.getBranch() == null) {
			 * queryString.append(" , C.ACNAME AS ACNAME "); } else {
			 * queryString.append(" , CC.ACNAME AS ACNAME "); } } // FROM TLF
			 * queryString.append(" ,CASE WHEN ( "); queryString.
			 * append(" T.TRANTYPEID ='2' OR T.TRANTYPEID ='3' ) ");
			 * queryString.append(" THEN ABS(T." + amountColumn +
			 * ") ELSE 0 END AS mDEBIT "); queryString.append(" ,CASE WHEN ( ");
			 * queryString.
			 * append(" T.TRANTYPEID ='1' OR T.TRANTYPEID ='4' ) ");
			 * queryString.append(" THEN ABS(T." + amountColumn +
			 * ") ELSE 0 END AS mCREDIT ");
			 * 
			 * // FROM COA queryString.append(" ,CASE WHEN ( ");
			 * queryString.append(" (ACTYPE IN ('I','L')  AND CC." +
			 * reportBudgetMonth + " < 0 ) OR ");
			 * queryString.append(" (ACTYPE IN ('A','E')  AND CC." +
			 * reportBudgetMonth + " > 0 ) ");
			 * queryString.append(" ) THEN ABS(CC." + reportBudgetMonth +
			 * ") ELSE 0 END AS DEBIT "); queryString.append(" ,CASE WHEN ( ");
			 * queryString.append(" (ACTYPE IN ('I','L')  AND CC." +
			 * reportBudgetMonth + " > 0 ) OR ");
			 * queryString.append(" (ACTYPE IN ('A','E')  AND CC." +
			 * reportBudgetMonth + " < 0 ) ");
			 * queryString.append(" ) THEN ABS(CC." + reportBudgetMonth +
			 * ") ELSE 0 END AS CREDIT "); queryString.
			 * append(" FROM COA AS C INNER JOIN VW_CCOA AS CC ON C.ID=CC.COAID RIGHT JOIN TLF T ON CC.ID = T.CCOAID WHERE CC.BUDGET=?budget "
			 * ); queryString.
			 * append(" AND DATEPART(MONTH,T.SETTLEMENTDATE) =?month");
			 * queryString.append(" AND " + reportBudgetMonth + "<> 0 "); if
			 * (dto.isGroup()) {
			 * queryString.append(" AND C.ACCODETYPE='GROUP' "); } else {
			 * queryString.append(" AND C.ACCODETYPE='DETAIL' "); }
			 * 
			 * if (dto.getCurrencyType().equals(CurrencyType.SOURCECURRENCY) &&
			 * dto.getCurrency() != null) {
			 * queryString.append(" AND CC.CURRENCYID=?currencyid "); }
			 * 
			 * if (dto.getBranch() != null) {
			 * queryString.append(" AND CC.BRANCHID=?branchid "); }
			 */
			queryString.append(" ) T");
			queryString.append(" GROUP BY ACODE,ACNAME,ACTYPE ORDER BY ACODE");

			Query q = em.createNativeQuery(queryString.toString());

			q.setParameter("budget", BusinessUtil.getBudgetInfo(reportDate, 2));

			if (dto.getCurrencyType().equals(CurrencyType.SOURCECURRENCY) && dto.getCurrency() != null) {
				q.setParameter("currencyid", dto.getCurrency().getId());
			}
			if (dto.getBranch() != null) {
				q.setParameter("branchid", dto.getBranch().getId());
			}

			q.setParameter("month", (cal.get(Calendar.MONTH)) + 1);

			List<Object[]> objList = q.getResultList();
			if (objList != null) {
				dtoList = new ArrayList<>();
				for (Object[] obj : objList) {
					TrialBalanceReportDto reportDto = new TrialBalanceReportDto();
					reportDto.setAcode(obj[0].toString());
					reportDto.setAcname(obj[1].toString());
					reportDto.setAcType(AccountType.valueOf(obj[2].toString()));
					reportDto.setmDebit(new BigDecimal(obj[3].toString()));
					reportDto.setmCredit(new BigDecimal(obj[4].toString()));
					reportDto.setDebit(new BigDecimal(obj[5].toString()));
					reportDto.setCredit(new BigDecimal(obj[6].toString()));
					dtoList.add(reportDto);
				}
				// trial with Negative Amount in Debit Side
				/*
				 * for (TrialBalanceReportDto reportDto : dtoList) { if
				 * (reportDto.getAcType().equals(AccountType.A) ||
				 * reportDto.getAcType().equals(AccountType.E)) {
				 * reportDto.setDebit(reportDto.getDebit().subtract(reportDto.
				 * getCredit())); //
				 * reportDto.setmDebit(reportDto.getmDebit().subtract(reportDto.
				 * getmCredit()));
				 * reportDto.setCredit(BigDecimal.valueOf(0.00)); } else if
				 * (reportDto.getAcType().equals(AccountType.I) ||
				 * reportDto.getAcType().equals(AccountType.L)) {
				 * reportDto.setCredit(reportDto.getCredit().subtract(reportDto.
				 * getDebit())); //
				 * reportDto.setmCredit(reportDto.getmCredit().subtract(
				 * reportDto.getmDebit()));
				 * reportDto.setDebit(BigDecimal.valueOf(0.00)); } }
				 */
				for (TrialBalanceReportDto reportDto : dtoList) {
					if (reportDto.getAcType().equals(AccountType.A) || reportDto.getAcType().equals(AccountType.E)) {
						BigDecimal amount = reportDto.getDebit().subtract(reportDto.getCredit());
						if (amount.compareTo(BigDecimal.ZERO) > 0) {
							reportDto.setDebit(amount);
							reportDto.setCredit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
						} else {
							reportDto.setCredit((amount.abs()));
							reportDto.setDebit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
						}
					} else if (reportDto.getAcType().equals(AccountType.I) || reportDto.getAcType().equals(AccountType.L)) {
						BigDecimal amount = reportDto.getCredit().subtract(reportDto.getDebit());
						if (amount.compareTo(BigDecimal.ZERO) > 0) {
							reportDto.setCredit(amount);
							reportDto.setDebit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
						} else {
							reportDto.setDebit(amount.abs());
							reportDto.setCredit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
						}
					}
				}
			}

		} catch (PersistenceException pe) {
			throw translate("Failed to findTrialBalanceDetailList", pe);
		}
		return dtoList;
	}
}
