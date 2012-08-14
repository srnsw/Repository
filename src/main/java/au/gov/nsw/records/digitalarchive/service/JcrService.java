package au.gov.nsw.records.digitalarchive.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Set;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.modeshape.common.collection.Problem;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrTools;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JcrService {

	private JcrEngine engine;
	private ClassPathResource cpr;

	public JcrService(){
		cpr = new ClassPathResource("configRepository.xml");
		JcrConfiguration configuration = new JcrConfiguration();
		try {
			configuration.loadFrom(cpr.getFile());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		// Now create the JCR engine ...
		engine = configuration.build();
		engine.start();

		if (engine.getProblems().hasProblems()) {
			for (Problem problem : engine.getProblems()) {
				System.err.println(problem.getMessageString());
			}
			throw new RuntimeException("Could not start due to problems");
		}
		System.out.println("JCR Started");
	}

	public void createNode(String workspaceName, String path, InputStream is) throws RepositoryException{
		Session session = null;
		try {
			javax.jcr.Repository repo = engine.getRepository("mode:" + workspaceName);
			session = repo.login();
			JcrTools tools = new JcrTools();
			// Create the node at the supplied path ...
			Node node = tools.findOrCreateNode(session, path, "nt:folder", "nt:file");

			// Upload the file to that node ...
			Node contentNode = tools.findOrCreateChild(node, "jcr:content", "nt:resource");
			contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
			Binary binaryValue = session.getValueFactory().createBinary(is);
			contentNode.setProperty("jcr:data", binaryValue);

			session.save();

		}finally{
			if (session!=null){
				session.logout();
			}
		}
	}

	public String getPathToWorkspace(String name) throws ParserConfigurationException, SAXException{

		try {
			File fXmlFile = cpr.getFile();

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("source");
			for (int temp = 0; temp < nList.getLength(); temp++) { 
				org.w3c.dom.Node nNode = nList.item(temp);
				Element eElement = (Element) nNode;
//				System.out.println(eElement.getAttribute("workspaceRootPath"));
//				System.out.println(eElement.getAttribute("defaultWorkspaceName"));
//				System.out.println(eElement.getAttribute("jcr:name"));
				if (eElement.getAttribute("jcr:name").equalsIgnoreCase(name)){
					return eElement.getAttribute("workspaceRootPath") + File.separatorChar + eElement.getAttribute("defaultWorkspaceName") + File.separatorChar + eElement.getAttribute("jcr:name");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}


	public Binary getBinaryNode(String workspaceName, String pathToNode){
		try {
			JcrRepository jcrRepository = engine.getRepository("mode:" + workspaceName);
			Session session = jcrRepository.login();

			pathToNode = StringUtils.removeStart(pathToNode, "/");
			// Get the node by path ...
			Node root = session.getRootNode();
			Node node = root;
			if (pathToNode.length() != 0) {
				if (!pathToNode.endsWith("]")) pathToNode = pathToNode + "[1]";
				node = pathToNode.equals("") ? root : root.getNode(pathToNode);
			}

			Node jcrContent = node.getNode("jcr:content");
			//String fileName = node.getName();
			return jcrContent.getProperty("jcr:data").getBinary();
		} catch (PathNotFoundException e) {
			e.printStackTrace();
		} catch (ValueFormatException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Set<String> getRepositoryNames(){
		return engine.getRepositoryNames();
	}
}
