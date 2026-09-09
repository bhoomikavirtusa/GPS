/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wiley.permissions.web.shared.controllers.sources;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.View;

import com.wiley.permissions.domain.persistence.permissions.SourceFile;
import com.wiley.permissions.persistence.PersistenceException;
import com.wiley.permissions.repositories.SourceRepository;
import com.wiley.permissions.services.SourceService;
import com.wiley.permissions.web.shared.controllers.BaseAnnotatedController;
import com.wiley.permissions.web.shared.util.GenericFileView;

/**
 *
 * @author ttidwell
 */
@Controller
public class ViewSourceFileController
extends BaseAnnotatedController
{
	private final static Log log = LogFactory.getLog(ViewSourceFileController.class);

	private SourceService sourceService = null;
	private SourceRepository sourceRepository = null;


	@GetMapping("/sources/viewSourceFile")
	public View handle(@RequestParam("fileId") int fileId)
	throws PersistenceException
	{
		log.debug("handle(): entered...");

		SourceFile file = getSourceRepository().find(SourceFile.class, fileId);

		GenericFileView view = new GenericFileView();
		view.setFileName(file.getFileName());
		view.setContentType(file.getMimeType());
		view.setData(file.getFileData());

		log.debug("handle(): File Data length: " + file.getFileData().length);

		return view;
	}

	public SourceService getSourceService() {
		return sourceService;
	}

	public void setSourceService(SourceService sourceService) {
		this.sourceService = sourceService;
	}

	public SourceRepository getSourceRepository() {
		return sourceRepository;
	}

	public void setSourceRepository(SourceRepository sourceRepository) {
		this.sourceRepository = sourceRepository;
	}
}
