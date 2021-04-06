package org.ace.accounting.posting.service.interfaces;

import java.util.Date;

import org.ace.accounting.dto.YPDto;

public interface IYearlyPostingService {
	public void handleYearlyPosting(Date postingDate, YPDto tDto, YPDto plDto);
}
