package org.ace.accounting.system.setup.service;

import org.ace.java.component.persistence.exception.DAOException;

public interface ISetupService {
	public String findSetupValueByVariable(String variable) throws DAOException;
}
