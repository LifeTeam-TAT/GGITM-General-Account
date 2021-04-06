package org.ace.java.component.idgen.service.interfaces;

import org.ace.accounting.system.branch.Branch;
import org.ace.accounting.user.User;
import org.ace.java.component.idgen.exception.CustomIDGeneratorException;

@SuppressWarnings("rawtypes")
public interface ICustomIDGenerator {
	public String getNextId(String key, String productCode) throws CustomIDGeneratorException;

	public String getNextId(String key, String productCode, Branch branch) throws CustomIDGeneratorException;

	public String getPrefix(Class cla) throws CustomIDGeneratorException;

	public String getPrefix(Class cla, User user) throws CustomIDGeneratorException;
	
	public String getNextId(String key, String productCode,String vocherCodePrefix) throws CustomIDGeneratorException;
}
