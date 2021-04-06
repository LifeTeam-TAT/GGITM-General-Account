package org.ace.accounting.posting.persistence.interfaces;

import java.util.Date;

import org.ace.accounting.dto.YPDto;
import org.ace.java.component.persistence.exception.DAOException;

public interface IYearlyPostingDAO {

	public void moveTlfToHistory(Date postingDate) throws DAOException;

	public void insertTLFHISTByPostingDate(Date postingDate) throws DAOException;

	public void deleteTLFByPostingDate(Date postingDate) throws DAOException;

	public void moveCcoaToHistory(String budgetYear, YPDto tAc, YPDto plAc) throws DAOException;

	public void insertCcoaHistory() throws DAOException;

	public void updateCcoaBal() throws DAOException;

	public void updateCcoaBalByTacAndPlAcAndACTYPE(YPDto tAc, YPDto plAc) throws DAOException;

	public void updateCcoaAllZeroAndBudgetYear(String budgetYear) throws DAOException;

	public void increaseBudgetYear() throws DAOException;
}
