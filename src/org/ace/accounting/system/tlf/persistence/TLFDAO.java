package org.ace.accounting.system.tlf.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.ace.accounting.common.VoucherType;
import org.ace.accounting.common.utils.BusinessUtil;
import org.ace.accounting.dto.EditVoucherDto;
import org.ace.accounting.dto.GainAndLossDTO;
import org.ace.accounting.dto.RateDTO;
import org.ace.accounting.report.balancesheet.BalanceSheetDTO;
import org.ace.accounting.system.tlf.TLF;
import org.ace.accounting.system.tlf.persistence.interfaces.ITLFDAO;
import org.ace.accounting.system.trantype.TranType;
import org.ace.java.component.persistence.BasicDAO;
import org.ace.java.component.persistence.exception.DAOException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository("TLFDAO")
public class TLFDAO extends BasicDAO implements ITLFDAO {

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED)
	public List<TLF> findAll() throws DAOException {
		List<TLF> result = null;
		try {
			Query q = em.createNamedQuery("TLF.findAll");
			result = q.getResultList();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to find all of TLF", pe);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED)
	public List<RateDTO> findRateList(RateDTO ratedto) throws DAOException {
		List<RateDTO> result = new ArrayList<RateDTO>();
		Map<String, String> param = new HashMap<String, String>();
		List<String> coaIdList = null;
		try {
			StringBuffer query = new StringBuffer();
			query.append("SELECT NEW " + RateDTO.class.getName());
			query.append(
					"(c.acCode,c.acName,sum(Case when (t.tranType.id ='2' OR T.tranType.id ='3') THEN T.homeAmount ELSE 0 End ),sum(Case when (t.tranType.id ='1' OR T.tranType.id ='4') THEN T.homeAmount ELSE 0 END ),");
			query.append(
					" sum(Case when (t.tranType.id ='2' OR T.tranType.id ='3') THEN T.localAmount ELSE 0 END), sum(Case when (t.tranType.id ='1' OR T.tranType.id ='4') THEN T.localAmount ELSE 0 END ),");
			query.append("(case when (t.currency.isHomeCur=true) then 1.0000 else t.currency." + BusinessUtil.getMonthlyRateFormula(ratedto.getReportMonth() + 1));
			query.append(" END),t.currency.currencyCode,t.branch.name,c.acType )");
			query.append(" FROM TLF t left join t.ccoa a left join a.coa c where FUNC('MONTH', t.settlementDate)=:month and t.reverse = 'false'");
			if (ratedto.getCurrencyId() != null) {
				query.append(" and t.currency.id=:currencyId");
			}
			if (ratedto.getBranchId() != null) {
				query.append(" and t.branch.id=:branchId");
			}

			if (ratedto.getCoaList().size() > 0) {

				coaIdList = ratedto.getCoaList().stream().map(coa -> coa.getId()).collect(Collectors.toList());
				query.append(" and c.id IN :coaIdList");
				// TODO FIXME PSH
				/*
				 * int i = 0; for (ChartOfAccount coa : ratedto.getCoaList()) {
				 * i += 1; param.put("id" + i, coa.getId()); if
				 * (ratedto.getCoaList().get(0).equals(coa)) {
				 * query.append(" and c.id=:id" + i); } else {
				 * query.append(" or c.id=:id" + i); } }
				 */
			}
			query.append(" group by c.acCode,c.acName,t.currency.currencyCode,t.branch.name,t.currency.isHomeCur,c.acType");
			query.append(" ,t.currency." + BusinessUtil.getMonthlyRateFormula(ratedto.getReportMonth() + 1));
			Query q = em.createQuery(query.toString());
			q.setParameter("month", ratedto.getReportMonth() + 1);
			if (ratedto.getCurrencyId() != null) {
				q.setParameter("currencyId", ratedto.getCurrencyId());
			}
			if (ratedto.getBranchId() != null) {
				q.setParameter("branchId", ratedto.getBranchId());
			}
			if (ratedto.getCoaList().size() > 0) {

				q.setParameter("coaIdList", coaIdList);
				// TODO FIXME PSH
				/*
				 * for (String key : param.keySet()) { q.setParameter(key,
				 * param.get(key)); }
				 */
			}
			result = q.getResultList();
			em.flush();
		} catch (NoResultException nre) {
			return null;
		} catch (PersistenceException pe) {
			throw translate("Failed to find tlf data: ", pe);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED)
	public List<String> findVoucherNoByBranchIdAndVoucherType(String branchId, VoucherType voucherType) throws DAOException {
		List<String> result = null;
		try {
			Query q = em.createNamedQuery("TLF.findVoucherNoByBranchId");
			q.setParameter("branchId", branchId);
			if (voucherType.equals(VoucherType.CASH)) {
				q.setParameter("status", "C%");
			} else {
				q.setParameter("status", "T%");
			}
			q.setParameter("eNo", "VOC%");
			result = q.getResultList();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to find all of Voucher No by " + branchId, pe);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED)
	public List<TLF> findVoucherListByReverseZero(String voucherNo) throws DAOException {
		List<TLF> result = null;
		try {
			Query q = em.createNamedQuery("TLF.findVoucherListByVoucherNo");
			q.setParameter("voucherNo", voucherNo);
			result = q.getResultList();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to find VoucherListByReverseZero", pe);
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public TranType findCashAccountByVoucherNo(String voucherNo) throws DAOException {
		TranType result = null;
		EditVoucherDto dto = null;
		try {
			Query q = em.createNamedQuery("TLF.findCashAccountByVoucherNo");
			q.setParameter("voucherNo", voucherNo);
			q.setMaxResults(1);
			dto = (EditVoucherDto) q.getSingleResult();
			em.flush();
			if (dto != null) {
				result = dto.getTranType();
			}
		} catch (NoResultException e) {
			return null;
		} catch (PersistenceException pe) {
			throw translate("Failed to find CashAccountByVoucherNo", pe);
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateReverseByID(String id, boolean reverse) throws DAOException {
		try {
			Query q = em.createNamedQuery("TLF.updateReverseByID");
			q.setParameter("id", id);
			q.setParameter("reverse", reverse);
			q.executeUpdate();
		} catch (PersistenceException pe) {
			throw translate("Failed to update ReverseByID " + id, pe);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public List<GainAndLossDTO> findGainAndLossList(GainAndLossDTO ratedto) throws DAOException {
		List<GainAndLossDTO> result = new ArrayList<GainAndLossDTO>();
		String mValue = BusinessUtil.getMonthlyRateFormula(ratedto.getReportMonth() + 1);
		String msrcValue = mValue.replace("m", "msrc");
		String homeCurrencyID = BusinessUtil.getHomeCurrency();
		try {
			StringBuffer query = new StringBuffer();
			query.append(
					"SELECT CAST(SUM(T.MONTHLY) AS NUMERIC(18,2)), SUM(T.HOME+T.EXCHANGE),CAST((CAST(SUM(T.MONTHLY) AS NUMERIC(18,2))-SUM(T.HOME+T.EXCHANGE)) AS NUMERIC(18,2)),T.ACCODE FROM");
			query.append(" (SELECT (SUM(CC." + mValue + " * c." + mValue + ")) AS MONTHLY, SUM(" + msrcValue + ") AS HOME,0.0 as EXCHANGE,EF.ACCODE AS ACCODE");
			query.append(" FROM CCOA CC");
			query.append(" INNER JOIN CUR C ON C.ID = CC.CURRENCYID");
			query.append(" INNER JOIN COA CO ON CO.ID = CC.COAID");
			query.append(" INNER JOIN EXCHANGECONFIG EF ON EF.ACCODE = CO.ACCODE");
			query.append(" WHERE 1= 1");

			if (ratedto.getCurrencyId() != null) {
				query.append(" AND C.ID = ?1");
			}
			if (ratedto.getBranchId() != null) {
				query.append(" AND CC.BRANCHID = ?2");
			}

			query.append(" GROUP By EF.ACCODE");
			query.append(" UNION");

			query.append(" SELECT 0 AS MONTHLY,0 as HOME, SUM(" + msrcValue + ") AS EXCHANGE,EF.ACCODE AS ACCODE");
			query.append(" FROM CCOA CC");
			query.append(" INNER JOIN CUR C ON C.ID = CC.CURRENCYID");
			query.append(" INNER JOIN COA CO ON CO.ID = CC.COAID");
			query.append(" INNER JOIN EXCHANGECONFIG EF ON EF.EXCHANGECODE = CO.ACCODE");
			query.append(" WHERE 1= 1");
			query.append(" AND C.ID = ?3");

			if (ratedto.getBranchId() != null) {
				query.append(" AND CC.BRANCHID = ?2");
			}

			query.append(" GROUP By EF.ACCODE");
			query.append(" )  T");

			query.append(" GROUP By T.ACCODE");

			Query q = em.createNativeQuery(query.toString());
			q.setParameter("3", homeCurrencyID);

			if (ratedto.getCurrencyId() != null) {
				q.setParameter("1", ratedto.getCurrencyId());
			}
			if (ratedto.getBranchId() != null) {
				q.setParameter("2", ratedto.getBranchId());
			}

			List<Object[]> resultList = q.getResultList();
			for (Object[] dbResult : resultList) {
				GainAndLossDTO dto = new GainAndLossDTO();
				dto.setMonthlyAmount(new BigDecimal(dbResult[0].toString()));
				dto.setHomeAmount(new BigDecimal(dbResult[1].toString()));
				dto.setDifferenceAmount(new BigDecimal(dbResult[2].toString()));
				dto.setAcCode(dbResult[3].toString());

				result.add(dto);
			}
		} catch (NoResultException nre) {
			return null;
		} catch (PersistenceException pe) {
			throw translate("Failed to find tlf data: ", pe);
		}
		return result;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<BalanceSheetDTO> generateBalanceSheet(String branchId, String currencyId, boolean isHomeCurrency) throws DAOException {
		try {
			List<BalanceSheetDTO> resultList = new ArrayList<>();

			StringBuffer query = new StringBuffer();
			if (isHomeCurrency) {
				query.append(
						" SELECT C.ID,C.ACTYPE,C.ACCODE,C.ACCODETYPE,C.ACNAME,SUM(CC.M1),SUM(CC.M2),SUM(CC.M3),SUM(CC.M4),SUM(CC.M5),SUM(CC.M6),SUM(CC.M7),SUM(CC.M8),SUM(CC.M9),SUM(CC.M10),SUM(CC.M11),SUM(CC.M12),CASE WHEN C.ACCODETYPE = 'DETAIL' THEN C.GROUPID");
			} else {
				query.append(
						" SELECT C.ID,C.ACTYPE,C.ACCODE,C.ACCODETYPE,C.ACNAME,SUM(CC.MSRC1),SUM(CC.MSRC2),SUM(CC.MSRC3),SUM(CC.MSRC4),SUM(CC.MSRC5),SUM(CC.MSRC6),SUM(CC.MSRC7),SUM(CC.MSRC8),SUM(CC.MSRC9),SUM(CC.MSRC10),SUM(CC.MSRC11),SUM(CC.MSRC12),CASE WHEN C.ACCODETYPE = 'DETAIL' THEN C.GROUPID");
			}

			query.append(" WHEN C.ACCODETYPE = 'GROUP' THEN C.HEADID ELSE NULL END AS PARENTID,CASE WHEN C.ACCODETYPE != 'DETAIL' THEN C.ACNAME ELSE NULL END AS PARENTNAME");
			query.append(" FROM CCOA CC INNER JOIN COA C ON C.ID = CC.COAID");
			query.append(" WHERE 1=1");

			if (null != branchId) {
				query.append(" AND CC.BRANCHID =?1");
			}

			if (null != currencyId) {
				query.append(" AND CC.CURRENCYID =?2");
			}

			query.append(" GROUP BY C.ACCODE,C.ACCODETYPE,C.ACNAME,C.ACCODETYPE,C.GROUPID,C.HEADID,C.ID,C.ACTYPE");
			query.append(" ORDER BY C.ACCODE, C.ACCODETYPE");

			Query q = em.createNativeQuery(query.toString());

			if (null != branchId) {
				q.setParameter("1", branchId);
			}

			if (null != currencyId) {
				q.setParameter("2", currencyId);
			}

			List<Object[]> objectList = q.getResultList();

			for (Object[] dbResult : objectList) {
				BalanceSheetDTO dto = new BalanceSheetDTO();
				dto.setId(dbResult[0].toString());
				dto.setAcType(dbResult[1].toString());
				dto.setAcCode(dbResult[2].toString());
				dto.setAcCodeType(dbResult[3].toString());
				dto.setAcName(dbResult[4].toString());

				dto.setM1(new BigDecimal(dbResult[5].toString()));
				dto.setM2(new BigDecimal(dbResult[6].toString()));
				dto.setM3(new BigDecimal(dbResult[7].toString()));
				dto.setM4(new BigDecimal(dbResult[8].toString()));
				dto.setM5(new BigDecimal(dbResult[9].toString()));
				dto.setM6(new BigDecimal(dbResult[10].toString()));
				dto.setM7(new BigDecimal(dbResult[11].toString()));
				dto.setM8(new BigDecimal(dbResult[12].toString()));
				dto.setM9(new BigDecimal(dbResult[13].toString()));
				dto.setM10(new BigDecimal(dbResult[14].toString()));
				dto.setM11(new BigDecimal(dbResult[15].toString()));
				dto.setM12(new BigDecimal(dbResult[16].toString()));

				dto.setParentId(dbResult[17] == null ? "" : dbResult[17].toString());
				dto.setParentName(dbResult[18] == null ? "" : dbResult[18].toString());

				resultList.add(dto);
			}

			return resultList;
		} catch (NoResultException ne) {
			return new ArrayList<>();
		} catch (PersistenceException pe) {
			throw translate("Failed to find Balance Sheet data: ", pe);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<BalanceSheetDTO> generateBalanceSheetByDate(String branchId, String currencyId, Date fromDate, Date toDate, boolean isHomeCurrency) throws DAOException {
		try {
			List<BalanceSheetDTO> resultList = new ArrayList<>();
			String amountColumn = null;
			if (isHomeCurrency) {
				amountColumn = "HOMEAMOUNT";
			} else {
				amountColumn = "LOCALAMOUNT";
			}

			StringBuffer query = new StringBuffer();

			query.append(" SELECT");
			query.append(" ID \"ID\",");
			query.append(" ACTYPE \"ACTYPE\",");
			query.append(" ACCODE \"ACCODE\",");
			query.append(" ACCODETYPE \"ACCODETYPE\",");
			query.append(" RTRIM(ACNAME) \"ACNAME\",");

			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[JANUARY]),0.00)) AS JANUARY,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[FEBRUARY]),0.00)) AS FEBRUARY,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[MARCH]),0.00)) AS MARCH,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[APRIL]),0.00)) AS APRIL,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[MAY]),0.00)) AS MAY,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[JUNE]),0.00)) AS JUNE,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[JULY]),0.00)) AS JULY,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[AUGUST]),0.00)) AS AUGUST,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[SEPTEMBER]),0.00)) AS SEPTEMBER,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[OCTOBER]),0.00)) AS OCTOBER,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[NOVEMBER]),0.00)) AS NOVEMBER,");
			query.append(" SUM(ISNULL (CONVERT(DECIMAL(18,2),[DECEMBER]),0.00)) AS DECEMBER,");

			query.append(" CASE WHEN ACCODETYPE = 'DETAIL' THEN GROUPID WHEN ACCODETYPE = 'GROUP'");
			query.append(" THEN HEADID ELSE NULL END AS PARENTID,");
			query.append(" CASE WHEN ACCODETYPE != 'DETAIL' THEN ACNAME");
			query.append(" ELSE NULL END AS PARENTNAME");

			query.append(" FROM");
			query.append(" (");
			query.append(" SELECT ID \"ID\",ACODE \"ACCODE\", ACNAME \"ACNAME\",ACCODETYPE \"ACCODETYPE\",ACTYPE \"ACTYPE\",GROUPID \"GROUPID\",HEADID \"HEADID\",");
			query.append(" case when Actype = 'A' or Actype ='E' then sum(Debit-Credit)");
			query.append("  WHEN ACTYPE = 'I' OR ACTYPE ='L' THEN SUM(CREDIT-DEBIT)");
			query.append("  END \"HOMEAMOUNT\",MONTHOFBUSINESS FROM");
			query.append(" (SELECT");
			query.append(" CASE WHEN TP.TRANCODE = 'CSCREDIT' OR TP.TRANCODE ='TRCREDIT' THEN SUM(" + amountColumn + ")");
			query.append(" ELSE 0");
			query.append(" END \"CREDIT\",");
			query.append(" CASE WHEN TP.TRANCODE = 'CSDEBIT' OR TP.TRANCODE ='TRDEBIT' THEN SUM(" + amountColumn + ")");
			query.append(" ELSE 0");
			query.append(" END \"DEBIT\",");
			query.append(" TP.TRANCODE \"TRANCODE\",");
			query.append(" C.ID \"ID\",");
			query.append(" C.ACCODE \"ACODE\",");
			query.append(" C.ACNAME \"ACNAME\",");
			query.append(" C.ACCODETYPE \"ACCODETYPE\",");
			query.append(" C.ACTYPE \"ACTYPE\",");
			query.append(" C.GROUPID \"GROUPID\",");
			query.append(" C.HEADID \"HEADID\",");
			query.append(" B.NAME \"BRANCH\",");
			query.append(" CR.CUR \"CURRENCY\",");
			query.append(" FORMAT(T.SETTLEMENTDATE, 'MMMM') \"MONTHOFBUSINESS\"");
			query.append(" FROM TLF  T");
			query.append(" LEFT JOIN CCOA CC");
			query.append(" ON CC.ID = T.CCOAID");
			query.append(" LEFT JOIN COA C");
			query.append(" ON C.ID = CC.COAID");
			query.append(" LEFT JOIN TRANTYPE TP");
			query.append(" ON TP.ID = T.TRANTYPEID");
			query.append(" LEFT JOIN BRANCH B");
			query.append(" ON B.ID = T.BRANCHID");
			query.append(" LEFT JOIN CUR CR");
			query.append(" ON CR.ID = T.CURRENCYID");
			query.append(" WHERE");
			query.append(" T.REVERSE =0 AND T.PAID=1");

			if (null != fromDate) {
				query.append(" AND CAST(T.SETTLEMENTDATE AS DATE)>=?1");
			}

			if (null != toDate) {
				query.append(" AND CAST(T.SETTLEMENTDATE AS DATE)<=?2");
			}

			if (null != branchId) {
				query.append(" AND T.BRANCHID=?3");
			}

			if (null != currencyId) {
				query.append(" AND T.CURRENCYID=?4");
			}

			query.append(" GROUP BY TP.TRANCODE,C.ID,C.ACCODE,C.ACCODETYPE,ACTYPE,C.HEADID,C.GROUPID,B.NAME,CR.CUR,C.ACNAME,T.SETTLEMENTDATE");
			query.append(" UNION ALL ");
			query.append(" SELECT");
			query.append(" 0 \"CREDIT\",0 \"DEBIT\",");
			query.append(" '' \"TRANCODE\",");
			query.append(" C.ID \"ID\",");
			query.append(" C.ACCODE \"ACODE\",");
			query.append(" C.ACNAME \"ACNAME\",");
			query.append(" C.ACCODETYPE \"ACCODETYPE\",");
			query.append(" C.ACTYPE \"ACTYPE\",");
			query.append(" C.GROUPID \"GROUPID\",");
			query.append(" C.HEADID \"HEADID\",");
			query.append(" '' \"BRANCH\",");
			query.append(" '' \"CURRENCY\",");
			query.append(" '' \"MONTHOFBUSINESS\"");
			query.append(" FROM COA C");
			query.append(" )B");
			query.append(" GROUP BY ID,ACODE,ACNAME,ACCODETYPE,HEADID,GROUPID,ACTYPE,MONTHOFBUSINESS");
			query.append(" )A");
			query.append(" PIVOT(SUM(" + amountColumn + ")FOR MONTHOFBUSINESS IN ([JANUARY],[FEBRUARY],[MARCH],[APRIL],[MAY],[JUNE],[JULY],");
			query.append(" [AUGUST],[SEPTEMBER],[OCTOBER],[NOVEMBER],[DECEMBER]))PIV");
			query.append(" GROUP BY ID,ACCODE,ACNAME,ACCODETYPE,ACTYPE,HEADID,GROUPID");

			Query q = em.createNativeQuery(query.toString());

			if (null != fromDate) {
				q.setParameter("1", fromDate);
			}

			if (null != toDate) {
				q.setParameter("2", toDate);
			}

			if (null != branchId) {
				q.setParameter("3", branchId);
			}

			if (null != currencyId) {
				q.setParameter("4", currencyId);
			}

			List<Object[]> objectList = q.getResultList();

			for (Object[] dbResult : objectList) {
				BalanceSheetDTO dto = new BalanceSheetDTO();
				dto.setId(dbResult[0].toString());
				dto.setAcType(dbResult[1].toString());
				dto.setAcCode(dbResult[2].toString());
				dto.setAcCodeType(dbResult[3].toString());
				dto.setAcName(dbResult[4].toString());

				dto.setM1(new BigDecimal(dbResult[5].toString()));
				dto.setM2(new BigDecimal(dbResult[6].toString()));
				dto.setM3(new BigDecimal(dbResult[7].toString()));
				dto.setM4(new BigDecimal(dbResult[8].toString()));
				dto.setM5(new BigDecimal(dbResult[9].toString()));
				dto.setM6(new BigDecimal(dbResult[10].toString()));
				dto.setM7(new BigDecimal(dbResult[11].toString()));
				dto.setM8(new BigDecimal(dbResult[12].toString()));
				dto.setM9(new BigDecimal(dbResult[13].toString()));
				dto.setM10(new BigDecimal(dbResult[14].toString()));
				dto.setM11(new BigDecimal(dbResult[15].toString()));
				dto.setM12(new BigDecimal(dbResult[16].toString()));

				dto.setParentId(dbResult[17] == null ? "" : dbResult[17].toString());
				dto.setParentName(dbResult[18] == null ? "" : dbResult[18].toString());

				resultList.add(dto);
			}

			return resultList;
		} catch (NoResultException ne) {
			return new ArrayList<>();
		} catch (PersistenceException pe) {
			throw translate("Failed to find Balance Sheet data: ", pe);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<BalanceSheetDTO> generateBalanceSheetByGroup() throws DAOException {
		try {
			List<BalanceSheetDTO> resultList = new ArrayList<>();

			StringBuffer query = new StringBuffer();

			query.append(" SELECT ID,ACTYPE,ACCODE,ACCODETYPE,ACNAME FROM COA WHERE ACCODETYPE IN ('HEAD','GROUP')");

			Query q = em.createNativeQuery(query.toString());

			List<Object[]> objectList = q.getResultList();

			for (Object[] dbResult : objectList) {
				BalanceSheetDTO dto = new BalanceSheetDTO();
				dto.setId(dbResult[0].toString());
				dto.setAcType(dbResult[1].toString());
				dto.setAcCode(dbResult[2].toString());
				dto.setAcCodeType(dbResult[3].toString());
				dto.setAcName(dbResult[4].toString());

				dto.setM1(BigDecimal.ZERO);
				dto.setM2(BigDecimal.ZERO);
				dto.setM3(BigDecimal.ZERO);
				dto.setM4(BigDecimal.ZERO);
				dto.setM5(BigDecimal.ZERO);
				dto.setM6(BigDecimal.ZERO);
				dto.setM7(BigDecimal.ZERO);
				dto.setM8(BigDecimal.ZERO);
				dto.setM9(BigDecimal.ZERO);
				dto.setM10(BigDecimal.ZERO);
				dto.setM11(BigDecimal.ZERO);
				dto.setM12(BigDecimal.ZERO);

				resultList.add(dto);
			}

			return resultList;
		} catch (NoResultException ne) {
			return new ArrayList<>();
		} catch (PersistenceException pe) {
			throw translate("Failed to find Balance Sheet data: ", pe);
		}
	}

}
