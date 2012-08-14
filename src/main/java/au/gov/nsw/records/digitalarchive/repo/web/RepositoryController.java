package au.gov.nsw.records.digitalarchive.repo.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.PostConstruct;
import javax.jcr.Binary;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import au.gov.nsw.records.digitalarchive.service.CommandExecutor;
import au.gov.nsw.records.digitalarchive.service.JcrService;
import au.gov.nsw.records.digitalarchive.service.PathProcessor;

@RequestMapping("/repository")
@Controller
public class RepositoryController {

	private static final Log log = LogFactory.getLog(RepositoryController.class);
	private static JcrService service = new JcrService();
	private String prefix = "/repository";
	private String nativePathToWorkspace;
	
	@PostConstruct
	public void init(){
		try {
			nativePathToWorkspace = service.getPathToWorkspace("da");
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}
	
	@RequestMapping(value = "/**", method = RequestMethod.POST)
	public void create(MultipartFile content, HttpServletRequest request) throws RepositoryException, CreatedEvent, InternalErrorEvent {

		String pathToNode = PathProcessor.getPathToNode(StringUtils.removeStart(request.getRequestURI(), request.getContextPath() + prefix));

		if (!StringUtils.endsWith(pathToNode, "/")){
			pathToNode = pathToNode + "/";
		}
		
		String fileName = "";
		try {
			fileName = content.getOriginalFilename();
			InputStream is = new ByteArrayInputStream(content.getBytes());
			service.createNode("da", pathToNode + fileName, is);
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new InternalErrorEvent();
		}

		throw new CreatedEvent();
	}

	@RequestMapping(value = "/**", method = RequestMethod.PUT)
	public void setReadonly(@RequestParam("readonly") boolean readonly, HttpServletRequest request) throws IOException, OKEvent{
		
		if (readonly){
			String pathToNode = PathProcessor.getPathToNode(StringUtils.removeStart(request.getRequestURI(), request.getContextPath() + prefix));
			String filePath = nativePathToWorkspace + File.separatorChar + pathToNode;
			// change to read only if it's an original
			String[] args = new String[]{"chmod", "-w", filePath};
			CommandExecutor.exec(args);
			log.info("Executed " + args);
			throw new OKEvent();
		}
	}
	
	@RequestMapping(value = "/**", method =  RequestMethod.GET)
	public @ResponseBody String download(HttpServletRequest request, HttpServletResponse response) throws RepositoryException, IOException {

	String pathToNode = PathProcessor.getPathToNode(StringUtils.removeStart(request.getRequestURI(), request.getContextPath() + prefix));
	String repositoryName = PathProcessor.getRepositoryName(StringUtils.removeStart(request.getRequestURI(), request.getContextPath() + prefix));
		if (!StringUtils.isEmpty(repositoryName)){
			if (!StringUtils.isEmpty(pathToNode)){
				response.setHeader("Content-Disposition", "attachment; filename=\"" + PathProcessor.getNodeName(request.getRequestURI()) + "\"");
				Binary binary = service.getBinaryNode(repositoryName, pathToNode);
				if (binary!=null){
					InputStream in = binary.getStream();
					if (in!=null){
						OutputStream out = response.getOutputStream();
						IOUtils.copy(in, out);	
						response.flushBuffer();
					}
					binary.dispose();
					return "<html>downloading... " + PathProcessor.getNodeName(request.getRequestURI()) + "</html>";	
				}else{
					throw new PathNotFoundException();
				}
			}else{
				throw new PathNotFoundException();	
			}
		}else{
			StringBuffer sb = new StringBuffer();
			for (String repo:service.getRepositoryNames()){
				sb.append("<br>" + repo);
			}
			throw new PathNotFoundException();
		}
	}
	
	
	@ExceptionHandler(CreatedEvent.class)
	@ResponseStatus(HttpStatus.CREATED)
	public void whenCreated() {
		log.info("Returned created status");
	}

	@ExceptionHandler({InternalErrorEvent.class, RepositoryException.class, IOException.class})
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public void whenError() {
		log.info("Returned internal server error status");
	}
	
	@ExceptionHandler({NotfoundEvent.class, PathNotFoundException.class})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public void whenNotFound() {
		log.info("Returned not found status");
	}
	
	@ExceptionHandler({OKEvent.class})
	@ResponseStatus(HttpStatus.OK)
	public void whenOK() {
		log.info("Returned not found status");
	}
	
	@SuppressWarnings("serial")
	private class OKEvent extends Exception{}
	@SuppressWarnings("serial")
	private class CreatedEvent extends Exception{}
	@SuppressWarnings("serial")
	private class NotfoundEvent extends Exception{}
	@SuppressWarnings("serial")
	private class InternalErrorEvent extends Exception{}
}
