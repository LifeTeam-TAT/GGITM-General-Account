package org.ace.java.component.idgen.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.annotation.Resource;

import org.ace.accounting.process.interfaces.IUserProcessService;
import org.ace.accounting.system.branch.Branch;
import org.ace.accounting.user.User;
import org.ace.java.component.idgen.IDGen;
import org.ace.java.component.idgen.exception.CustomIDGeneratorException;
import org.ace.java.component.idgen.persistence.interfaces.IDGenDAOInf;
import org.ace.java.component.idgen.service.interfaces.ICustomIDGenerator;
import org.ace.java.component.idgen.service.interfaces.IDConfigLoader;
import org.ace.java.component.persistence.exception.DAOException;
import org.springframework.stereotype.Service;

@SuppressWarnings("rawtypes")
@Service("CustomIDGenerator")
public class CustomIDGenerator implements ICustomIDGenerator {
	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMM");

	@Resource(name = "ID_CONFIG")
	private Properties properties;

	@Resource(name = "IDGenDAO")
	private IDGenDAOInf idGenDAO;

	@Resource(name = "IDConfigLoader")
	private IDConfigLoader idConfigLoader;

	@Resource(name = "UserProcessService")
	private IUserProcessService userProcessService;

	public String getNextId(String key, String productCode) throws CustomIDGeneratorException {
		String id = null;
		try {
			String genName = (String) properties.getProperty(key);
			id = formatId(idGenDAO.getNextId(genName), productCode, key);
		} catch (DAOException e) {
			throw new CustomIDGeneratorException(e.getErrorCode(), "Failed to generate a ID", e);
		}
		return id;
	}

	public String getNextId(String key, String productCode, Branch branch) throws CustomIDGeneratorException {
		String id = null;
		try {
			String genName = (String) properties.getProperty(key);
			id = formatId(idGenDAO.getNextId(genName, branch), productCode, key);
		} catch (DAOException e) {
			throw new CustomIDGeneratorException(e.getErrorCode(), "Failed to generate a ID", e);
		}
		return id;
	}

	private String formatId(IDGen idGen, String productCode, String key) {
		String id = idGen.getMaxValue() + "";
		String prefix = idGen.getPrefix();
		String suffix = idGen.getSuffix();
		int maxLength = idGen.getLength();
		String branchCode = "";

		if (userProcessService.getLoginUser().getBranch() != null) {
			branchCode = userProcessService.getLoginUser().getBranch().getBranchCode();
		}

		int idLength = id.length();
		for (; (maxLength - idLength) > 0; idLength++) {
			id = '0' + id;
		}
		if (suffix == null) {
			suffix = "";
		}
		if (productCode == null) {
			productCode = "";
		}

		id = prefix + productCode + "/" + getDateString() + "/" + id + "/" + branchCode + suffix;
		return id;
	}
	
	private String formatId(IDGen idGen, String productCode,String key, String vocherCode) {
		String id = idGen.getMaxValue() + "";
		String prefix = idGen.getPrefix();
		String suffix = idGen.getSuffix();
		int maxLength = idGen.getLength();
		String branchCode = "";

		if (userProcessService.getLoginUser().getBranch() != null) {
			branchCode = userProcessService.getLoginUser().getBranch().getPrefix();
		}

		int idLength = id.length();
		for (; (maxLength - idLength) > 0; idLength++) {
			id = '0' + id;
		}
		if (suffix == null) {
			suffix = "";
		}
		if (productCode == null) {
			productCode = "";
		}

		id = prefix + productCode + "/" + vocherCode + "/" + branchCode + "/" + getDateString()  + "/" + id;
		return id;
	}

	public String getPrefix(Class cla) {
		return getPrefixStr(cla, null);
	}

	public String getPrefix(Class cla, User user) {
		return getPrefixStr(cla, user);
	}

	private String getPrefixStr(Class cla, User user) {
		String branchCode = "";

		if (userProcessService.getLoginUser().getBranch() != null) {
			branchCode = userProcessService.getLoginUser().getBranch().getBranchCode();
		}

		String prefix = idConfigLoader.getFormat(cla.getName());
		return prefix + branchCode;
	}

	private String getDateString() {
		return simpleDateFormat.format(new Date());
	}

	@Override
	public String getNextId(String key, String productCode, String vocherCodePrefix) throws CustomIDGeneratorException {
		String id = null;
		try {
			String genName = (String) properties.getProperty(key);
			id = formatId(idGenDAO.getNextId(genName), productCode, key,vocherCodePrefix);
		} catch (DAOException e) {
			throw new CustomIDGeneratorException(e.getErrorCode(), "Failed to generate a ID", e);
		}
		return id;
		
	}

	// private String getDateToString() {
	// DateTime calDate = new DateTime();
	// int year = calDate.getYear();
	// if (calDate.getMonthOfYear() <= 3)
	// return "(" + (year - 1) + " - " + year + ")";
	// else
	// return "(" + year + " - " + (year + 1) + ")";
	// }
}
