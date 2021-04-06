package org.ace.accounting.web.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;

import org.ace.accounting.common.CurrencyType;
import org.ace.accounting.common.PropertiesManager;
import org.ace.accounting.common.validation.MessageId;
import org.ace.accounting.report.balancesheet.BalanceSheetCriteria;
import org.ace.accounting.report.balancesheet.BalanceSheetDTO;
import org.ace.accounting.system.branch.Branch;
import org.ace.accounting.system.branch.service.interfaces.IBranchService;
import org.ace.accounting.system.currency.Currency;
import org.ace.accounting.system.currency.service.interfaces.ICurrencyService;
import org.ace.accounting.system.tlf.service.interfaces.ITLFService;
import org.ace.accounting.user.User;
import org.ace.java.web.common.BaseBean;
import org.apache.commons.io.FileUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;

@ViewScoped
@ManagedBean(name = "BalanceSheetReportActionBean")
public class BalanceSheetReportActionBean extends BaseBean {

	@ManagedProperty(value = "#{PropertiesManager}")
	private PropertiesManager propertiesManager;

	public void setPropertiesManager(PropertiesManager propertiesManager) {
		this.propertiesManager = propertiesManager;
	}

	@ManagedProperty(value = "#{TLFService}")
	private ITLFService tlfService;

	public void setTlfService(ITLFService tlfService) {
		this.tlfService = tlfService;
	}

	@ManagedProperty(value = "#{CurrencyService}")
	private ICurrencyService currencyService;

	public void setCurrencyService(ICurrencyService currencyService) {
		this.currencyService = currencyService;
	}

	@ManagedProperty(value = "#{BranchService}")
	private IBranchService branchService;

	public void setBranchService(IBranchService branchService) {
		this.branchService = branchService;
	}

	private List<BalanceSheetDTO> dtoList;

	private String dirPath = "/pdf-report/" + "balancesheetReport" + "/" + System.currentTimeMillis() + "/";
	private final String fileName = "Report";

	private BalanceSheetCriteria criteria;
	private boolean isBranchDisabled;

	@PostConstruct
	public void init() {
		criteria = new BalanceSheetCriteria();
		criteria.setCurrencyType(CurrencyType.HOMECURRENCY);
		criteria.setHomeCur(true);
		criteria.setHomeConverted(true);
		criteria.setReportType("B");
		criteria.setStartDate(new Date());
		criteria.setEndDate(new Date());
		User user = (User) getParam("LoginUser");
		if (user.isAdmin()) {
			isBranchDisabled = false;
		} else {
			isBranchDisabled = true;
			criteria.setBranch(user.getBranch());
		}
		dtoList = new ArrayList<>();
	}

	public void previewReport() {
		dtoList.clear();
		String branchId = criteria.getBranch() == null ? null : criteria.getBranch().getId();
		String currencyId = criteria.getCurrency() == null ? null : criteria.getCurrency().getId();
		if (criteria.isMonth()) {
			dtoList = tlfService.generateBalanceSheet(branchId, currencyId, criteria.isHomeCur());
		} else {
			List<BalanceSheetDTO> dtoDayList = tlfService.generateBalanceSheetByDate(branchId, currencyId, criteria.getStartDate(), criteria.getEndDate(), criteria.isHomeCur());
			if (!dtoDayList.isEmpty()) {
				calculateGroupTotal(dtoList, dtoDayList, criteria.getReportType());
			}
		}

		List<BalanceSheetDTO> orderList = claculateOrder(dtoList, criteria.getReportType());

		if (orderList.size() == 0) {
			addErrorMessage(null, MessageId.NO_RESULT);
		} else if (generateReport(orderList)) {
			PrimeFaces.current().executeScript("PF('balanceSheetDialog').show();");
		}

	}

	public List<BalanceSheetDTO> calculateGroupTotal(List<BalanceSheetDTO> resultList, List<BalanceSheetDTO> targetList, String reportType) {

		List<BalanceSheetDTO> groupList = new ArrayList<>();
		List<BalanceSheetDTO> detailList = new ArrayList<>();
		List<BalanceSheetDTO> headList = new ArrayList<>();
		/*
		 * if (reportType.equals("B")) { groupList =
		 * targetList.stream().filter(dto ->
		 * !dto.getAcCodeType().equals("DETAIL") && (dto.getAcType().equals("A")
		 * || dto.getAcType().equals("L"))) .collect(Collectors.toList());
		 * detailList = targetList.stream().filter(dto ->
		 * dto.getAcCodeType().equals("DETAIL") && (dto.getAcType().equals("A")
		 * || dto.getAcType().equals("L"))) .collect(Collectors.toList()); }
		 * else { groupList = targetList.stream().filter(dto ->
		 * !dto.getAcCodeType().equals("DETAIL") && (dto.getAcType().equals("I")
		 * || dto.getAcType().equals("E"))) .collect(Collectors.toList());
		 * detailList = targetList.stream().filter(dto ->
		 * dto.getAcCodeType().equals("DETAIL") && (dto.getAcType().equals("I")
		 * || dto.getAcType().equals("E"))) .collect(Collectors.toList()); }
		 */
		headList = targetList.stream().filter(dto -> dto.getAcCodeType().equals("HEAD")).collect(Collectors.toList());
		groupList = targetList.stream().filter(dto -> dto.getAcCodeType().equals("GROUP")).collect(Collectors.toList());
		detailList = targetList.stream().filter(dto -> dto.getAcCodeType().equals("DETAIL")).collect(Collectors.toList());

		if (!groupList.isEmpty()) {
			for (BalanceSheetDTO group : groupList) {

				/*
				 * for (BalanceSheetDTO target : detailList) { if
				 * (target.getParentId().equals(group.getId())) {
				 * group.setM1((target.getM1()));
				 * group.getM2().add(target.getM2());
				 * group.getM3().add(target.getM3());
				 * group.getM4().add(target.getM4());
				 * group.getM5().add(target.getM5());
				 * group.getM6().add(target.getM6());
				 * group.getM7().add(target.getM7());
				 * group.getM8().add(target.getM8());
				 * group.getM9().add(target.getM9());
				 * group.getM10().add(target.getM10());
				 * group.getM11().add(target.getM11());
				 * group.getM12().add(target.getM12()); } }
				 */
				BigDecimal m1 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM1()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM1(m1);
				BigDecimal m2 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM2()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM2(m2);
				BigDecimal m3 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM3()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM3(m3);
				BigDecimal m4 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM4()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM4(m4);
				BigDecimal m5 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM5()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM5(m5);
				BigDecimal m6 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM6()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM6(m6);
				BigDecimal m7 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM7()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM7(m7);
				BigDecimal m8 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM8()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM8(m8);
				BigDecimal m9 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM9()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM9(m9);
				BigDecimal m10 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM10()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM10(m10);
				BigDecimal m11 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM11()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM11(m11);
				BigDecimal m12 = detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).map(dto -> dto.getM12()).reduce(BigDecimal.ZERO, BigDecimal::add);
				group.setM12(m12);

			}
		}

		for (BalanceSheetDTO head : headList) {
			BigDecimal m1 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM1()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM1(m1);
			BigDecimal m2 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM2()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM2(m2);
			BigDecimal m3 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM3()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM3(m3);
			BigDecimal m4 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM4()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM4(m4);
			BigDecimal m5 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM5()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM5(m5);
			BigDecimal m6 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM6()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM6(m6);
			BigDecimal m7 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM7()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM7(m7);
			BigDecimal m8 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM8()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM8(m8);
			BigDecimal m9 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM9()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM9(m9);
			BigDecimal m10 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM10()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM10(m10);
			BigDecimal m11 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM11()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM11(m11);
			BigDecimal m12 = groupList.stream().filter(dto -> dto.getParentId().equals(head.getId())).map(dto -> dto.getM12()).reduce(BigDecimal.ZERO, BigDecimal::add);
			head.setM12(m12);
		}

		resultList.clear();
		resultList.addAll(headList);
		resultList.addAll(groupList);
		resultList.addAll(detailList);
		return resultList;
	}

	private List<BalanceSheetDTO> claculateOrder(List<BalanceSheetDTO> dtoList, String reportType) {
		List<BalanceSheetDTO> aHeaderList = new ArrayList<>();
		List<BalanceSheetDTO> lHeaderList = new ArrayList<>();
		List<BalanceSheetDTO> aGroupList = new ArrayList<>();
		List<BalanceSheetDTO> lGroupList = new ArrayList<>();
		List<BalanceSheetDTO> detailList = new ArrayList<>();

		BalanceSheetDTO lastTotal = new BalanceSheetDTO();
		BalanceSheetDTO lastTotalEqu = new BalanceSheetDTO();

		List<BalanceSheetDTO> orderList = new ArrayList<>();
		BigDecimal totalEquitym1 = BigDecimal.ZERO;
		BigDecimal totalEquitym2 = BigDecimal.ZERO;
		BigDecimal totalEquitym3 = BigDecimal.ZERO;
		BigDecimal totalEquitym4 = BigDecimal.ZERO;
		BigDecimal totalEquitym5 = BigDecimal.ZERO;
		BigDecimal totalEquitym6 = BigDecimal.ZERO;
		BigDecimal totalEquitym7 = BigDecimal.ZERO;
		BigDecimal totalEquitym8 = BigDecimal.ZERO;
		BigDecimal totalEquitym9 = BigDecimal.ZERO;
		BigDecimal totalEquitym10 = BigDecimal.ZERO;
		BigDecimal totalEquitym11 = BigDecimal.ZERO;
		BigDecimal totalEquitym12 = BigDecimal.ZERO;

		if (reportType.equals("B")) {
			aHeaderList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("HEAD") && dto.getAcType().equals("A")).collect(Collectors.toList());
			lHeaderList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("HEAD") && dto.getAcType().equals("L")).collect(Collectors.toList());
			aGroupList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("GROUP") && dto.getAcType().equals("A")).collect(Collectors.toList());
			lGroupList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("GROUP") && dto.getAcType().equals("L")).collect(Collectors.toList());
			detailList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("DETAIL") && (dto.getAcType().equals("A") || dto.getAcType().equals("L")))
					.collect(Collectors.toList());

			List<BalanceSheetDTO> iDetailList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("DETAIL") && dto.getAcType().equals("I")).collect(Collectors.toList());
			List<BalanceSheetDTO> eDetailList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("DETAIL") && dto.getAcType().equals("E")).collect(Collectors.toList());

			List<BalanceSheetDTO> iHeaderList = new ArrayList();
			List<BalanceSheetDTO> eHeaderList = new ArrayList();

			iHeaderList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("HEAD") && dto.getAcType().equals("I")).collect(Collectors.toList());
			eHeaderList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("HEAD") && dto.getAcType().equals("E")).collect(Collectors.toList());
			detailList.stream().filter(dto -> dto.getAcCode().equals("03-007-001")).findAny().get();
			BigDecimal totalI = BigDecimal.ZERO;
			BigDecimal totalE = BigDecimal.ZERO;
			BigDecimal m1Different = BigDecimal.ZERO;
			BigDecimal m2Different = BigDecimal.ZERO;
			BigDecimal m3Different = BigDecimal.ZERO;
			BigDecimal m4Different = BigDecimal.ZERO;
			BigDecimal m5Different = BigDecimal.ZERO;
			BigDecimal m6Different = BigDecimal.ZERO;
			BigDecimal m7Different = BigDecimal.ZERO;
			BigDecimal m8Different = BigDecimal.ZERO;
			BigDecimal m9Different = BigDecimal.ZERO;
			BigDecimal m10Different = BigDecimal.ZERO;
			BigDecimal m11Different = BigDecimal.ZERO;
			BigDecimal m12Different = BigDecimal.ZERO;

			totalI = iHeaderList.stream().map(dto -> dto.getM1()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM1()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM1()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM1()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m1Different = (iTotal.subtract(eTotal));
			} else {
				m1Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM2()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM2()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM2()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM2()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m2Different = (iTotal.subtract(eTotal));
			} else {
				m2Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM3()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM3()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM3()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM3()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m3Different = (iTotal.subtract(eTotal));
			} else {
				m3Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM4()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM4()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM4()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM4()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m4Different = (iTotal.subtract(eTotal));
			} else {
				m4Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM5()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM5()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM5()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM5()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m5Different = (iTotal.subtract(eTotal));
			} else {
				m5Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM6()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM6()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM6()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM6()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m6Different = (iTotal.subtract(eTotal));
			} else {
				m6Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM7()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM7()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM7()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM7()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m7Different = (iTotal.subtract(eTotal));
			} else {
				m7Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM8()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM8()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM8()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM8()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m8Different = (iTotal.subtract(eTotal));
			} else {
				m8Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM9()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM9()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM9()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM9()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m9Different = (iTotal.subtract(eTotal));
			} else {
				m9Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM10()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM10()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM10()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM10()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m10Different = (iTotal.subtract(eTotal));
			} else {
				m10Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM11()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM11()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM11()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM11()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m11Different = (iTotal.subtract(eTotal));
			} else {
				m11Different = (totalI.subtract(totalE));
			}

			totalI = iHeaderList.stream().map(dto -> dto.getM12()).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalE = eHeaderList.stream().map(dto -> dto.getM12()).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (totalI.doubleValue() == 0.0 && totalE.doubleValue() == 0.0) {
				BigDecimal iTotal = iDetailList.stream().map(dto -> dto.getM12()).reduce(BigDecimal.ZERO, BigDecimal::add);
				BigDecimal eTotal = eDetailList.stream().map(dto -> dto.getM12()).reduce(BigDecimal.ZERO, BigDecimal::add);
				m12Different = (iTotal.subtract(eTotal));
			} else {
				m12Different = (totalI.subtract(totalE));
			}

			for (BalanceSheetDTO dto : detailList) {
				if (dto.getAcCode().equals("03-007-001")) {
					dto.setM1(dto.getM1().add(m1Different));
					dto.setM2(dto.getM2().add(m2Different));
					dto.setM3(dto.getM3().add(m3Different));
					dto.setM4(dto.getM4().add(m4Different));
					dto.setM5(dto.getM5().add(m5Different));
					dto.setM6(dto.getM6().add(m6Different));
					dto.setM7(dto.getM7().add(m7Different));
					dto.setM8(dto.getM8().add(m8Different));
					dto.setM9(dto.getM9().add(m9Different));
					dto.setM10(dto.getM10().add(m10Different));
					dto.setM11(dto.getM11().add(m11Different));
					dto.setM12(dto.getM12().add(m12Different));
				}
			}

			for (BalanceSheetDTO dto : lGroupList) {
				if (dto.getAcCode().equals("03-007-000")) {
					dto.setM1(dto.getM1().add(m1Different));
					dto.setM2(dto.getM2().add(m2Different));
					dto.setM3(dto.getM3().add(m3Different));
					dto.setM4(dto.getM4().add(m4Different));
					dto.setM5(dto.getM5().add(m5Different));
					dto.setM6(dto.getM6().add(m6Different));
					dto.setM7(dto.getM7().add(m7Different));
					dto.setM8(dto.getM8().add(m8Different));
					dto.setM9(dto.getM9().add(m9Different));
					dto.setM10(dto.getM10().add(m10Different));
					dto.setM11(dto.getM11().add(m11Different));
					dto.setM12(dto.getM12().add(m12Different));
				}
			}

			BigDecimal lm1 = BigDecimal.ZERO;
			BigDecimal lm2 = BigDecimal.ZERO;
			BigDecimal lm3 = BigDecimal.ZERO;
			BigDecimal lm4 = BigDecimal.ZERO;
			BigDecimal lm5 = BigDecimal.ZERO;
			BigDecimal lm6 = BigDecimal.ZERO;
			BigDecimal lm7 = BigDecimal.ZERO;
			BigDecimal lm8 = BigDecimal.ZERO;
			BigDecimal lm9 = BigDecimal.ZERO;
			BigDecimal lm10 = BigDecimal.ZERO;
			BigDecimal lm11 = BigDecimal.ZERO;
			BigDecimal lm12 = BigDecimal.ZERO;

			lm1 = lHeaderList.stream().map(dto -> dto.getM1()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm2 = lHeaderList.stream().map(dto -> dto.getM2()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm3 = lHeaderList.stream().map(dto -> dto.getM3()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm4 = lHeaderList.stream().map(dto -> dto.getM4()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm5 = lHeaderList.stream().map(dto -> dto.getM5()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm6 = lHeaderList.stream().map(dto -> dto.getM6()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm7 = lHeaderList.stream().map(dto -> dto.getM7()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm8 = lHeaderList.stream().map(dto -> dto.getM8()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm9 = lHeaderList.stream().map(dto -> dto.getM9()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm10 = lHeaderList.stream().map(dto -> dto.getM10()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm11 = lHeaderList.stream().map(dto -> dto.getM11()).reduce(BigDecimal.ZERO, BigDecimal::add);
			lm12 = lHeaderList.stream().map(dto -> dto.getM12()).reduce(BigDecimal.ZERO, BigDecimal::add);

			totalEquitym1 = totalEquitym1.add(lm1.add(m1Different));
			totalEquitym2 = totalEquitym2.add(lm2.add(m2Different));
			totalEquitym3 = totalEquitym3.add(lm3.add(m3Different));
			totalEquitym4 = totalEquitym4.add(lm4.add(m4Different));
			totalEquitym5 = totalEquitym5.add(lm5.add(m5Different));
			totalEquitym6 = totalEquitym6.add(lm6.add(m6Different));
			totalEquitym7 = totalEquitym7.add(lm7.add(m7Different));
			totalEquitym8 = totalEquitym8.add(lm8.add(m8Different));
			totalEquitym9 = totalEquitym9.add(lm9.add(m9Different));
			totalEquitym10 = totalEquitym10.add(lm10.add(m10Different));
			totalEquitym11 = totalEquitym11.add(lm11.add(m11Different));
			totalEquitym12 = totalEquitym12.add(lm12.add(m12Different));

			lastTotalEqu.setAcName("Total Equity and Liabilities ");
			lastTotalEqu.setAcCode("");
			lastTotalEqu.setAcCodeType("DETAIL");

			lastTotalEqu.setM1(totalEquitym1);
			lastTotalEqu.setM2(totalEquitym2);
			lastTotalEqu.setM3(totalEquitym3);
			lastTotalEqu.setM4(totalEquitym4);
			lastTotalEqu.setM5(totalEquitym5);
			lastTotalEqu.setM6(totalEquitym6);
			lastTotalEqu.setM7(totalEquitym7);
			lastTotalEqu.setM8(totalEquitym8);
			lastTotalEqu.setM9(totalEquitym9);
			lastTotalEqu.setM10(totalEquitym10);
			lastTotalEqu.setM11(totalEquitym11);
			lastTotalEqu.setM12(totalEquitym12);

		} else {
			aHeaderList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("HEAD") && dto.getAcType().equals("I")).collect(Collectors.toList());
			lHeaderList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("HEAD") && dto.getAcType().equals("E")).collect(Collectors.toList());
			aGroupList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("GROUP") && dto.getAcType().equals("I")).collect(Collectors.toList());
			lGroupList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("GROUP") && dto.getAcType().equals("E")).collect(Collectors.toList());
			detailList = dtoList.stream().filter(dto -> dto.getAcCodeType().equals("DETAIL") && (dto.getAcType().equals("I") || dto.getAcType().equals("E")))
					.collect(Collectors.toList());

			lastTotal.setAcName("Net Profit and Loss");
			lastTotal.setAcCode("");
			lastTotal.setAcCodeType("DETAIL");
			BigDecimal m1 = aHeaderList.stream().map(dto -> dto.getM1()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m1 = m1.subtract(lHeaderList.stream().map(dto -> dto.getM1()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM1(m1);
			BigDecimal m2 = aHeaderList.stream().map(dto -> dto.getM2()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m2 = m2.subtract(lHeaderList.stream().map(dto -> dto.getM2()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM2(m2);
			BigDecimal m3 = aHeaderList.stream().map(dto -> dto.getM3()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m3 = m3.subtract(lHeaderList.stream().map(dto -> dto.getM3()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM3(m3);
			BigDecimal m4 = aHeaderList.stream().map(dto -> dto.getM4()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m4 = m4.subtract(lHeaderList.stream().map(dto -> dto.getM4()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM4(m4);
			BigDecimal m5 = aHeaderList.stream().map(dto -> dto.getM5()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m5 = m5.subtract(lHeaderList.stream().map(dto -> dto.getM5()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM5(m5);
			BigDecimal m6 = aHeaderList.stream().map(dto -> dto.getM6()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m6 = m6.subtract(lHeaderList.stream().map(dto -> dto.getM6()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM6(m6);
			BigDecimal m7 = aHeaderList.stream().map(dto -> dto.getM7()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m7 = m7.subtract(lHeaderList.stream().map(dto -> dto.getM7()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM7(m7);
			BigDecimal m8 = aHeaderList.stream().map(dto -> dto.getM8()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m8 = m8.subtract(lHeaderList.stream().map(dto -> dto.getM8()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM8(m8);
			BigDecimal m9 = aHeaderList.stream().map(dto -> dto.getM9()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m9 = m9.subtract(lHeaderList.stream().map(dto -> dto.getM9()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM9(m9);
			BigDecimal m10 = aHeaderList.stream().map(dto -> dto.getM10()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m10 = m10.subtract(lHeaderList.stream().map(dto -> dto.getM10()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM10(m10);
			BigDecimal m11 = aHeaderList.stream().map(dto -> dto.getM11()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m11 = m11.subtract(lHeaderList.stream().map(dto -> dto.getM11()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM11(m11);
			BigDecimal m12 = aHeaderList.stream().map(dto -> dto.getM12()).reduce(BigDecimal.ZERO, BigDecimal::add);
			m12 = m12.subtract(lHeaderList.stream().map(dto -> dto.getM12()).reduce(BigDecimal.ZERO, BigDecimal::add));
			lastTotal.setM12(m12);
		}

		for (BalanceSheetDTO head : aHeaderList) {
			BalanceSheetDTO headDTO = new BalanceSheetDTO();
			headDTO.setAcName(head.getAcName());
			headDTO.setAcCode(head.getAcCode());
			headDTO.setAcCodeType("DETAIL");
			orderList.add(headDTO);

			for (BalanceSheetDTO group : aGroupList) {

				BalanceSheetDTO groupDTO = new BalanceSheetDTO();
				groupDTO.setAcName(group.getAcName());
				groupDTO.setAcCode(group.getAcCode());
				groupDTO.setAcCodeType("DETAIL");
				orderList.add(groupDTO);

				orderList.addAll(detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).collect(Collectors.toList()));
				orderList.add(group);
			}
			orderList.add(head);
		}

		for (BalanceSheetDTO head : lHeaderList) {
			BalanceSheetDTO headDTO = new BalanceSheetDTO();
			headDTO.setAcName(head.getAcName());
			headDTO.setAcCode(head.getAcCode());
			headDTO.setAcCodeType("DETAIL");
			orderList.add(headDTO);

			for (BalanceSheetDTO group : lGroupList) {
				if (group.getParentId().equals(head.getId())) {

					BalanceSheetDTO groupDTO = new BalanceSheetDTO();
					groupDTO.setAcName(group.getAcName());
					groupDTO.setAcCode(group.getAcCode());
					groupDTO.setAcCodeType("DETAIL");
					orderList.add(groupDTO);

					orderList.addAll(detailList.stream().filter(dto -> dto.getParentId().equals(group.getId())).collect(Collectors.toList()));
					orderList.add(group);
				}

			}
			orderList.add(head);
		}
		for (BalanceSheetDTO dto : orderList) {
			if (!dto.getAcCodeType().equals("DETAIL")) {
				dto.setAcCode("");
				dto.setAcName("Total ".concat(dto.getAcName()));
			}
		}

		if (!reportType.equals("B")) {
			orderList.add(lastTotal);
		} else {
			orderList.add(lastTotalEqu);
		}
		return orderList;
	}

	public boolean generateReport(List<BalanceSheetDTO> dtoList) {
		try {

			InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("balanceSheetReport.jrxml");
			String image = FacesContext.getCurrentInstance().getExternalContext().getRealPath(propertiesManager.getProperties("ICON_RSOURCES"));
			Map<String, Object> parameters = new HashMap<String, Object>();

			String branch = "";
			String currency = "";

			parameters.put("logoPath", image);
			parameters.put("startDate", new Date());

			if (criteria.getBranch() == null) {
				branch = "All Branches";
			} else {
				branch = criteria.getBranch().getName();
			}
			parameters.put("branches", branch);

			if (criteria.getCurrency() == null) {
				currency = "All Currencies";
			} else {
				currency = criteria.getCurrency().getCurrencyCode();
			}
			if (criteria.isHomeConverted()) {
				currency = currency + " By Home Currency Converted";
			}
			parameters.put("currency", currency);

			parameters.put("dtoList", new JRBeanCollectionDataSource(dtoList));
			parameters.put("homeCurrency", criteria.isHomeCur());
			parameters.put("homeCurrencyConverted", criteria.isHomeConverted());

			JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
			JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JRBeanCollectionDataSource(dtoList));

			String path = getWebRootPath() + dirPath;

			FileUtils.forceMkdir(new File(path));
			JasperExportManager.exportReportToPdfFile(jasperPrint, path + fileName.concat(".pdf"));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			addErrorMessage(null, MessageId.REPORT_ERROR);
			return false;
		}
	}

	public StreamedContent getDownload() {
		dtoList.clear();
		String branchId = criteria.getBranch() == null ? null : criteria.getBranch().getId();
		String currencyId = criteria.getCurrency() == null ? null : criteria.getCurrency().getId();
		if (criteria.isMonth()) {
			dtoList = tlfService.generateBalanceSheet(branchId, currencyId, criteria.isHomeCur());
		} else {
			List<BalanceSheetDTO> dtoDayList = tlfService.generateBalanceSheetByDate(branchId, currencyId, criteria.getStartDate(), criteria.getEndDate(), criteria.isHomeCur());
			if (!dtoDayList.isEmpty()) {
				calculateGroupTotal(dtoList, dtoDayList, criteria.getReportType());
			}
		}

		List<BalanceSheetDTO> orderList = claculateOrder(dtoList, criteria.getReportType());
		StreamedContent result = null;
		if (orderList.isEmpty()) {
			addErrorMessage(null, MessageId.NO_RESULT);
		} else {
			result = getDownloadValue(orderList);
		}
		return result;
	}

	private StreamedContent getDownloadValue(List<BalanceSheetDTO> dtoList) {
		try {
			List<JasperPrint> prints = new ArrayList<JasperPrint>();
			InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("balanceSheetReport.jrxml");
			String image = FacesContext.getCurrentInstance().getExternalContext().getRealPath(propertiesManager.getProperties("ICON_RSOURCES"));
			Map<String, Object> parameters = new HashMap<String, Object>();

			String branch = "";
			String currency = "";

			parameters.put("logoPath", image);
			parameters.put("startDate", new Date());

			if (criteria.getBranch() == null) {
				branch = "All Branches";
			} else {
				branch = criteria.getBranch().getName();
			}
			parameters.put("branches", branch);

			if (criteria.getCurrency() == null) {
				currency = "All Currencies";
			} else {
				currency = criteria.getCurrency().getCurrencyCode();
			}
			if (criteria.isHomeConverted()) {
				currency = currency + " By Home Currency Converted";
			}
			parameters.put("currency", currency);

			parameters.put("dtoList", new JRBeanCollectionDataSource(dtoList));
			parameters.put("homeCurrency", criteria.isHomeCur());
			parameters.put("homeCurrencyConverted", criteria.isHomeConverted());

			JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
			JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JRBeanCollectionDataSource(dtoList));
			prints.add(jasperPrint);

			FileUtils.forceMkdir(new File(dirPath));

			File destFile = new File(dirPath + fileName.concat(".xls"));

			JRXlsExporter exporter = new JRXlsExporter();

			exporter.setExporterInput(SimpleExporterInput.getInstance(prints));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
			SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
			// configuration.setOnePagePerSheet(true);
			configuration.setOnePagePerSheet(false);
			configuration.setAutoFitPageHeight(true);
			configuration.setDetectCellType(true);
			configuration.setPrintPageWidth(200);
			configuration.setIgnoreCellBorder(false);
			configuration.setAutoFitPageHeight(true);
			configuration.setCollapseRowSpan(true);

			exporter.setConfiguration(configuration);

			exporter.exportReport();

			StreamedContent download = new DefaultStreamedContent();
			File file = new File(dirPath + fileName.concat(".xls"));
			InputStream input = new FileInputStream(file);
			ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
			download = new DefaultStreamedContent(input, externalContext.getMimeType(file.getName()), file.getName());
			return download;
		} catch (Exception e) {
			e.printStackTrace();
			addErrorMessage(null, MessageId.REPORT_ERROR);
			return null;
		}

	}

	public String getStream() {
		String fullFilePath = dirPath + fileName.concat(".pdf");
		return fullFilePath;
	}

	public List<BalanceSheetDTO> getDtoList() {
		return dtoList;
	}

	public BalanceSheetCriteria getCriteria() {
		return criteria;
	}

	public List<Currency> getCurrencyList() {
		return currencyService.findAllCurrency();
	}

	public CurrencyType[] getCurrencyTypes() {
		return CurrencyType.values();
	}

	public void changeCurrencyType(AjaxBehaviorEvent event) {
		if (criteria.getCurrencyType().equals(CurrencyType.HOMECURRENCY)) {
			criteria.setHomeCur(true);
			criteria.setHomeConverted(true);
			criteria.setCurrency(null);
		} else {
			criteria.setHomeCur(false);
			criteria.setHomeConverted(false);
		}
	}

	public boolean isBranchDisabled() {
		return isBranchDisabled;
	}

	public void setBranchDisabled(boolean isBranchDisabled) {
		this.isBranchDisabled = isBranchDisabled;
	}

	public List<Branch> getBranchList() {
		return branchService.findAllBranch();
	}
}
