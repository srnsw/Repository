package au.gov.nsw.records.digitalarchive.repo.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Arrays;

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
	public void setReadonly(@RequestParam("readonly") boolean readonly, HttpServletRequest request) throws OKEvent, InternalErrorEvent, IOException{
		
		if (readonly){
			String pathToNode = StringUtils.removeStart(request.getRequestURI(), request.getContextPath() + prefix);
			pathToNode =  URLDecoder.decode(pathToNode, "UTF-8");
			String filePath = nativePathToWorkspace + File.separatorChar + pathToNode.replace('\\', File.separatorChar).replace('/', File.separatorChar);
			//for testing in Windows 
			//String[] args = new String[]{"cmd.exe", "/c", "echo", filePath};
			// This clearly works on UNIX only.
			String[] args = new String[]{"chmod", "-w", filePath};
			String result;
			try {
				log.info("Executing: " + Arrays.toString(args));
				result = CommandExecutor.exec(args);
				log.info("Execution result: " + result);
				throw new OKEvent();
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			}
		}
		throw new InternalErrorEvent();
	}
	
	@RequestMapping(value = "/**", method =  RequestMethod.GET)
	public void download(HttpServletRequest request, HttpServletResponse response) throws RepositoryException, IOException {

	String pathToNode = PathProcessor.getPathToNode(StringUtils.removeStart(URLDecoder.decode(request.getRequestURI(),"UTF-8"), request.getContextPath() + prefix));
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
				}
			}
		}
		throw new PathNotFoundException();
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
