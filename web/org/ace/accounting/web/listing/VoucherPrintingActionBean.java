package org.ace.accounting.web.listing;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
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
import javax.faces.context.FacesContext;

import org.ace.accounting.common.PropertiesManager;
import org.ace.accounting.common.validation.MessageId;
import org.ace.accounting.dto.VPFTdto;
import org.ace.accounting.dto.VPdto;
import org.ace.accounting.dto.VoucherPrintingDTO;
import org.ace.accounting.process.interfaces.IUserProcessService;
import org.ace.accounting.report.listingReport.service.interfaces.IListingReportService;
import org.ace.accounting.system.branch.Branch;
import org.ace.accounting.user.User;
import org.ace.java.web.common.BaseBean;
import org.apache.commons.io.FileUtils;
import org.primefaces.PrimeFaces;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

@ManagedBean(name = "VoucherPrintingActionBean")
@ViewScoped
public class VoucherPrintingActionBean extends BaseBean {
	@ManagedProperty(value = "#{UserProcessService}")
	private IUserProcessService userProcessService;

	public void setUserProcessService(IUserProcessService userProcessService) {
		this.userProcessService = userProcessService;
	}

	@ManagedProperty(value = "#{PropertiesManager}")
	private PropertiesManager propertiesManager;

	public void setPropertiesManager(PropertiesManager propertiesManager) {
		this.propertiesManager = propertiesManager;
	}

	@ManagedProperty(value = "#{ListingReportService}")
	private IListingReportService listingReportService;

	public void setListingReportService(IListingReportService listingReportService) {
		this.listingReportService = listingReportService;
	}

	private boolean chkVoucherNo;
	private Date startDate;
	private Date endDate;
	private Branch branch;
	private VPdto dto;
	private List<VPdto> dtoList;
	private List<VPFTdto> voucherNoList = new ArrayList<VPFTdto>();
	private VPFTdto ftdto;
	private VPFTdto tidto;
	private List<VPFTdto> filterVoucherNoList;

	private final String reportName = "voucherPrintingReport";
	private final String pdfDirPath = "/pdf-report/" + reportName + "/" + System.currentTimeMillis() + "/";
	private final String dirPath = getWebRootPath() + pdfDirPath;
	private final String fileName = "Voucher Printing Report.pdf";

	@PostConstruct
	public void init() {
		chkVoucherNo = false;
		ftdto = new VPFTdto();
		ftdto.setStartDate(new Date());

		tidto = new VPFTdto();
		tidto.setEndDate(new Date());

		User user = userProcessService.getLoginUser();
		if (user != null) {
			if (user.isAdmin()) {
				branch = null;
			} else {
				if (user.getBranch() == null) {
					addErrorMessage(null, MessageId.USER_NO_BRANCH);
				} else {
					branch = user.getBranch();
				}
			}
		}
	}

	public boolean validate() {
		Date todayDate = new Date();
		if (ftdto.getStartDate().after(todayDate)) {
			addErrorMessage(null, MessageId.START_DATE_EXCEEDED);
			return false;
		} else if (tidto.getEndDate().after(todayDate)) {
			addErrorMessage(null, MessageId.END_DATE_EXCEEDED);
			return false;
		} else if (ftdto.getStartDate().after(tidto.getEndDate())) {
			addErrorMessage(null, MessageId.SE_INVALID_DATE);
			return false;
		} else if (chkVoucherNo == true) {
			if (ftdto == null) {
				addErrorMessage(null, MessageId.REQUIRED_FROMVOUCHER);
				return false;
			} else if (tidto == null) {
				addErrorMessage(null, MessageId.REQUIRED_TOVOUCHER);
				return false;
			} else if (ftdto.getVoucherNo().compareTo(tidto.getVoucherNo()) > 0) {
				addErrorMessage(null, MessageId.VOUCHER_INVALID);
				return false;
			}
			return true;
		} else {
			return true;
		}

	}

	public void selectFromVoucherNo(VPFTdto ftdto) {
		this.ftdto = ftdto;

	}

	public void selectToVoucherNo(VPFTdto tidto) {
		this.tidto = tidto;
	}

	public void chkClick() {
		if (ftdto.getStartDate() != null && tidto.getEndDate() != null) {
			voucherNoList = listingReportService.findFromVoucherNo(ftdto.getStartDate(), tidto.getEndDate(), branch);
		} else {
			chkVoucherNo = false;
			addErrorMessage(null, MessageId.SE_REQUIRED);
		}

	}

	public void searchResults() {
		if (validate()) {
			if (!chkVoucherNo) {
				dtoList = listingReportService.findVoucherPrinting(ftdto.getStartDate(), tidto.getEndDate());
				if (dtoList.size() == 0) {
					addErrorMessage(null, MessageId.NO_RESULT);
				} else {
					generateReport(dtoList);
				}
			} else {
				dtoList = listingReportService.findVoucherPrintingByVoucherNo(ftdto.getStartDate(), tidto.getEndDate(), ftdto.getVoucherNo(), tidto.getVoucherNo());
				if (dtoList.size() == 0) {
					addErrorMessage(null, MessageId.NO_RESULT);
				} else {
					generateReport(dtoList);
				}

			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void generateReport(List<VPdto> dtoList) {
		try {
			InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("voucherPrintingReport.jrxml");
			Map parameters = new HashMap();
			String image = FacesContext.getCurrentInstance().getExternalContext().getRealPath(propertiesManager.getProperties("LOGO"));
			String runDate = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
			parameters.put("runDate", runDate);
			parameters.put("logoPath", image);
			parameters.put("branch", userProcessService.getLoginUser().getBranch().getName());
			JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
			JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
			JREmptyDataSource source = new JREmptyDataSource();

			List<VoucherPrintingDTO> vpDtoList = new ArrayList<>();
			for (VPdto dto : dtoList) {
				VoucherPrintingDTO vpDto = new VoucherPrintingDTO();
				vpDto.setVoucherNo(dto.getVoucherNo());
				List<VPdto> vpList = new ArrayList<>();
				vpList = dtoList.stream().filter(a -> a.getVoucherNo().equals(dto.getVoucherNo())).collect(Collectors.toList());
				vpDto.setVpDtoList(vpList);
				vpDtoList.add(vpDto);
			}
			vpDtoList = vpDtoList.stream().distinct().collect(Collectors.toList());
			parameters.put("dtoList", new JRBeanCollectionDataSource(vpDtoList));
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JRBeanCollectionDataSource(vpDtoList));
			FileUtils.forceMkdir(new File(dirPath));
			JasperExportManager.exportReportToPdfFile(jasperPrint, dirPath + fileName);
			PrimeFaces.current().executeScript("PF('voucherPrintingDialog').show();");
		} catch (Exception e) {
			e.printStackTrace();
			addErrorMessage(null, MessageId.REPORT_ERROR);
		}
	}

	public String getStream() {
		String fullFilePath = pdfDirPath + fileName;
		return fullFilePath;

	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public VPdto getDto() {
		return dto;
	}

	public void setDto(VPdto dto) {
		this.dto = dto;
	}

	public List<VPdto> getDtoList() {
		return dtoList;
	}

	public void setDtoList(List<VPdto> dtoList) {
		this.dtoList = dtoList;
	}

	public Branch getBranch() {
		return branch;
	}

	public void setBranch(Branch branch) {
		this.branch = branch;
	}

	public VPFTdto getFtdto() {
		return ftdto;
	}

	public void setFtdto(VPFTdto ftdto) {
		this.ftdto = ftdto;
	}

	public boolean isChkVoucherNo() {
		return chkVoucherNo;
	}

	public void setChkVoucherNo(boolean chkVoucherNo) {
		this.chkVoucherNo = chkVoucherNo;
	}

	public Date getTodayDate() {
		return new Date();

	}

	public VPFTdto getTidto() {
		return tidto;
	}

	public void setTidto(VPFTdto tidto) {
		this.tidto = tidto;
	}

	public List<VPFTdto> getVoucherNoList() {
		return voucherNoList;
	}

	public void setVoucherNoList(List<VPFTdto> voucherNoList) {
		this.voucherNoList = voucherNoList;
	}

	public List<VPFTdto> getFilterVoucherNoList() {
		return filterVoucherNoList;
	}

	public void setFilterVoucherNoList(List<VPFTdto> filterVoucherNoList) {
		this.filterVoucherNoList = filterVoucherNoList;
	}

}
