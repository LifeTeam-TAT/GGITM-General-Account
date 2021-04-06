package org.ace.accounting.web.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
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

import org.ace.accounting.common.CurrencyType;
import org.ace.accounting.common.MonthNames;
import org.ace.accounting.common.PropertiesManager;
import org.ace.accounting.common.utils.DateUtils;
import org.ace.accounting.common.validation.MessageId;
import org.ace.accounting.dto.TrialBalanceCriteriaDto;
import org.ace.accounting.dto.TrialBalanceReportDto;
import org.ace.accounting.process.interfaces.IUserProcessService;
import org.ace.accounting.report.TrialBalanceAccountType;
import org.ace.accounting.report.trialBalanceDetail.service.interfaces.ITrialBalanceDetailService;
import org.ace.accounting.system.branch.Branch;
import org.ace.accounting.system.branch.service.interfaces.IBranchService;
import org.ace.accounting.system.currency.Currency;
import org.ace.accounting.system.currency.service.interfaces.ICurrencyService;
import org.ace.accounting.system.systempost.SystemPost;
import org.ace.accounting.system.systempost.service.interfaces.ISystemPostService;
import org.ace.java.web.common.BaseBean;
import org.apache.commons.io.FileUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
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

@ManagedBean(name = "TrialBalanceDetailActionBean")
@ViewScoped
public class TrialBalanceDetailActionBean extends BaseBean {

	private TrialBalanceCriteriaDto trialBalanceCriteriaDto;
	private List<TrialBalanceReportDto> reportResultList;
	private List<Currency> currencyList;
	private List<Branch> branchList;
	private EnumSet<MonthNames> monthSet;
	private List<Integer> yearList;
	private boolean isBranchDiabled = true;
	@ManagedProperty(value = "#{PropertiesManager}")
	private PropertiesManager propertiesManager;

	private Date postingDate;

	public void setPropertiesManager(PropertiesManager propertiesManager) {
		this.propertiesManager = propertiesManager;
	}

	@ManagedProperty(value = "#{TrialBalanceDetailService}")
	private ITrialBalanceDetailService trialBalanceService;

	public void setTrialBalanceService(ITrialBalanceDetailService trialBalanceService) {
		this.trialBalanceService = trialBalanceService;
	}

	@ManagedProperty(value = "#{UserProcessService}")
	private IUserProcessService userProcessService;

	public void setUserProcessService(IUserProcessService userProcessService) {
		this.userProcessService = userProcessService;
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

	@ManagedProperty(value = "#{SystemPostService}")
	private ISystemPostService sysService;

	public void setSysService(ISystemPostService sysService) {
		this.sysService = sysService;
	}

	@PostConstruct
	public void init() {
		monthSet = EnumSet.allOf(MonthNames.class);
		yearList = DateUtils.getActiveYears();
		createNewTrialBalance();
		SystemPost sysPost = sysService.findbyPostingName("TRIALPOST");
		postingDate = sysPost.getPostingDate();
		reportResultList = new ArrayList<>();

	}

	public void createNewTrialBalance() {
		branchList = branchService.findAllBranch();
		currencyList = currencyService.findAllCurrency();

		trialBalanceCriteriaDto = new TrialBalanceCriteriaDto();
		if (userProcessService.getLoginUser().isAdmin()) {
			trialBalanceCriteriaDto.setBranch(null);
			isBranchDiabled = false;
		} else {
			trialBalanceCriteriaDto.setBranch(userProcessService.getLoginUser().getBranch());
			isBranchDiabled = true;
		}
		trialBalanceCriteriaDto.setCurrencyType(CurrencyType.HOMECURRENCY);
		trialBalanceCriteriaDto.setAccountType(TrialBalanceAccountType.Gl_ACODE);
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		trialBalanceCriteriaDto.setRequiredMonth(cal.get(Calendar.MONTH));
		trialBalanceCriteriaDto.setRequiredYear(cal.get(Calendar.YEAR));

	}

	public void previewReport() {
		reportResultList.clear();
		reportResultList = trialBalanceService.findTrialBalanceOldFormat(trialBalanceCriteriaDto);
		if (reportResultList.size() == 0) {
			addErrorMessage(null, MessageId.NO_RESULT);
		} else
			try {
				reportResultList = reportResultList.stream().filter(dto ->dto.getDebit().compareTo(dto.getCredit()) !=0).collect(Collectors.toList());
				if (generateReport(reportResultList)) {
					PrimeFaces.current().executeScript("PF('TBPdfDialog').show();");
					/*
					 * RequestContext context =
					 * RequestContext.getCurrentInstance();
					 * context.execute("PF('TBPdfDialog').show();");
					 */
				}
			} catch (Exception e) {
				handleException(e);
			}
	}

	public boolean generateReport(List<TrialBalanceReportDto> reportResultList2) throws IOException, JRException {
		try {
			InputStream inputStream = null;
			inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("trialBalanceReport.jrxml");
			String image = FacesContext.getCurrentInstance().getExternalContext().getRealPath(propertiesManager.getProperties("LOGO"));
			Map<String, Object> parameters = new HashMap<String, Object>();
			String branch;
			String currency;
			if (trialBalanceCriteriaDto.getBranch() == null) {
				branch = "All Branches";
			} else {
				branch = trialBalanceCriteriaDto.getBranch().getName();
			}
			if (trialBalanceCriteriaDto.getCurrency() == null) {
				currency = "All Currencies";
			} else {
				currency = trialBalanceCriteriaDto.getCurrency().getCurrencyCode();
			}
			if (trialBalanceCriteriaDto.isHomeCurrencyConverted()) {
				currency = currency + " By Home Currency Converted";
			}
			String month = null;
			int monthVal = trialBalanceCriteriaDto.getRequiredMonth();
			month = String.valueOf(MonthNames.values()[monthVal]);
			parameters.put("logoPath", image);
			parameters.put("reportDate", DateUtils.formatDateToString(new Date()));
			parameters.put("reportTime", DateUtils.formatDateToStringTime(new Date()));
			parameters.put("postingDate", DateUtils.formatDateToString(postingDate));
			parameters.put("branch", branch);
			parameters.put("currency", currency);
			parameters.put("year", trialBalanceCriteriaDto.getRequiredYear());
			parameters.put("month", month);
			parameters.put("detailLabel", trialBalanceCriteriaDto.isGroup() ? "Group " : "Detail ");
			JRBeanCollectionDataSource source = new JRBeanCollectionDataSource(reportResultList2);
			parameters.put("datasource", source);

			JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
			JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
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
		reportResultList.clear();
		reportResultList = trialBalanceService.findTrialBalanceOldFormat(trialBalanceCriteriaDto);
		StreamedContent result = null;
		if (reportResultList.size() == 0) {
			addErrorMessage(null, MessageId.NO_RESULT);
		} else {
			result = getDownloadValue(reportResultList);
		}
		return result;
	}

	private StreamedContent getDownloadValue(List<TrialBalanceReportDto> reportResultList2) {
		try {
			List<JasperPrint> prints = new ArrayList<JasperPrint>();
			InputStream inputStream = null;
			inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("trialBalanceReport.jrxml");
			String image = FacesContext.getCurrentInstance().getExternalContext().getRealPath(propertiesManager.getProperties("LOGO"));
			Map<String, Object> parameters = new HashMap<String, Object>();
			String branch;
			String currency;
			if (trialBalanceCriteriaDto.getBranch() == null) {
				branch = "All Branches";
			} else {
				branch = trialBalanceCriteriaDto.getBranch().getName();
			}
			if (trialBalanceCriteriaDto.getCurrency() == null) {
				currency = "All Currencies";
			} else {
				currency = trialBalanceCriteriaDto.getCurrency().getCurrencyCode();
			}
			if (trialBalanceCriteriaDto.isHomeCurrencyConverted()) {
				currency = currency + " By Home Currency Converted";
			}
			String month = null;
			int monthVal = trialBalanceCriteriaDto.getRequiredMonth();
			month = String.valueOf(MonthNames.values()[monthVal]);
			parameters.put("logoPath", image);
			parameters.put("reportDate", DateUtils.formatDateToString(new Date()));
			parameters.put("reportTime", DateUtils.formatDateToStringTime(new Date()));
			parameters.put("postingDate", DateUtils.formatDateToString(postingDate));
			parameters.put("branch", branch);
			parameters.put("currency", currency);
			parameters.put("year", trialBalanceCriteriaDto.getRequiredYear());
			parameters.put("month", month);
			parameters.put("detailLabel", trialBalanceCriteriaDto.isGroup() ? "Group " : "Detail ");
			JRBeanCollectionDataSource source = new JRBeanCollectionDataSource(reportResultList2);
			parameters.put("datasource", source);

			JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
			JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
			prints.add(jasperPrint);

			FileUtils.forceMkdir(new File(dirPath));

			File destFile = new File(dirPath + fileName.concat(".xls"));

			JRXlsExporter exporter = new JRXlsExporter();

			exporter.setExporterInput(SimpleExporterInput.getInstance(prints));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));
			SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
			// configuration.setOnePagePerSheet(true);
			configuration.setDetectCellType(true);
			configuration.setIgnoreCellBorder(false);
			configuration.setAutoFitPageHeight(true);
			configuration.setCollapseRowSpan(true);
			configuration.setFontSizeFixEnabled(true);
			configuration.setColumnWidthRatio(1F);

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

	public TrialBalanceCriteriaDto getTrialBalanceDto() {
		return trialBalanceCriteriaDto;
	}

	public List<Currency> getCurrencyList() {
		return currencyList;
	}

	public List<Branch> getBranchList() {
		return branchList;
	}

	public CurrencyType[] getCurrencyTypes() {
		return CurrencyType.values();
	}

	public TrialBalanceAccountType[] getTrialBalanceAcodeTypes() {
		return TrialBalanceAccountType.values();
	}

	public List<Integer> getYearList() {
		return yearList;
	}

	public EnumSet<MonthNames> getMonthSet() {
		return monthSet;
	}

	public boolean isBranchDiabled() {
		return isBranchDiabled;
	}

	// path for jrxml template.
	private String dirPath = "/pdf-report/" + "trialBalanceReport" + "/" + System.currentTimeMillis() + "/";
	// pdf name.
	private final String fileName = "Trial Balance Report";

	public String getStream() {
		String fileFullName = dirPath + fileName.concat(".pdf");
		return fileFullName;
	}

	public TrialBalanceCriteriaDto getTrialBalanceCriteriaDto() {
		return trialBalanceCriteriaDto;
	}

	public void setTrialBalanceCriteriaDto(TrialBalanceCriteriaDto trialBalanceCriteriaDto) {
		this.trialBalanceCriteriaDto = trialBalanceCriteriaDto;
	}

	public List<TrialBalanceReportDto> getReportResultList() {
		return reportResultList;
	}

	public void setReportResultList(List<TrialBalanceReportDto> reportResultList) {
		this.reportResultList = reportResultList;
	}

	public Date getPostingDate() {
		return postingDate;
	}

	public void setPostingDate(Date postingDate) {
		this.postingDate = postingDate;
	}

}
