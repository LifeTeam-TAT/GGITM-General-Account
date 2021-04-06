package org.ace.accounting.system.setup.persistence;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.ace.accounting.system.setup.Setup;
import org.ace.accounting.system.setup.persistence.interfaces.ISetupDAO;
import org.ace.java.component.persistence.BasicDAO;
import org.ace.java.component.persistence.exception.DAOException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository("SetupDAO")
public class SetupDAO extends BasicDAO implements ISetupDAO {

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Setup> findAll() throws DAOException {
		List<Setup> result = null;
		try {
			Query q = em.createNamedQuery("Setup.findAll");
			result = q.getResultList();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to find all of User", pe);
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public String findSetupValueByVariable(String variable) throws DAOException {
		String result = null;
		try {
			Query q = em.createNamedQuery("Setup.findSetupValueByVariable");
			q.setParameter("variable", variable);
			result = (String) q.getSingleResult();
			em.flush();
		} catch (NoResultException e) {
			return null;
		} catch (PersistenceException pe) {
			throw translate("Failed to find all of User", pe);
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateSetupValueByVariable(String variable, String value) {
		try {
			Query q = em.createNamedQuery("Setup.updateSetupValueByVariable");
			q.setParameter("variable", variable);
			q.setParameter("value", value);
			q.executeUpdate();
			em.flush();
		} catch (PersistenceException pe) {
			throw translate("Failed to find all of User", pe);
		}
	}

}
