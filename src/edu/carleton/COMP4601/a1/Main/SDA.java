package edu.carleton.COMP4601.a1.Main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;

import edu.carleton.COMP4601.a1.Model.Document;

@Path("/sda")
public class SDA {

	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	final private String PATH = "http://localhost:8080/COMP4601SDA/rest/sda";
	private String name;

	public SDA() {
		name = "COMP4601 Searchable Document Archive";
	}

	// Gets the SDA name as a String
	@GET
	public String printName() {
		return name;
	}

	// Gets the SDA name as XML
	@GET
	@Produces(MediaType.APPLICATION_XML)
	public String sayXML() {
		return "<?xml version=\"1.0\"?>" + "<sda> " + name + " </sda>";
	}

	// Gets the SDA name as HTML
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtml() {
		return "<html> " + "<title>" + name + "</title>" + "<body><h1>" + name
				+ "</body></h1>" + "</html> ";
	}

	// Gets all documents as XML
	@GET
	@Path("documents")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	public ArrayList<Document> getDocumentsXML() {
		ArrayList<Document> documents = DatabaseManager.getInstance().getDocuments();

		if(documents == null) {
			throw new RuntimeException("No documents exist");
		}

		return documents;
	}

	// Gets all the documents as HTML
	@GET
	@Path("documents")
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_XML)
	public String getDocumentsHTML() {
		ArrayList<Document> documents = DatabaseManager.getInstance().getDocuments();

		if(documents == null) {
			return get404();
		}

		return documentsToHTML(documents);
	}

	// Gets all documents with the given tag string as XML
	@GET
	@Path("search/{tags}")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	public ArrayList<Document> getDocumentsByTagXML(@PathParam("tags") String tags) {
		ArrayList<Document> documents = DatabaseManager.getInstance().findDocumentsByTag(splitTags(tags));

		if(documents == null) {
			throw new RuntimeException("No documents exist");
		}

		return documents;
	}

	// Gets all documents with the given tag string as HTML
	@GET
	@Path("search/{tags}")
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_XML)
	public String getDocumentsByTagHTML(@PathParam("tags") String tags) {
		ArrayList<Document> documents = DatabaseManager.getInstance().findDocumentsByTag(splitTags(tags));

		if(documents == null) {
			return get204();
		}

		return documentsToHTML(documents);
	}

	// Deletes a document with a given tag string and returns a HTTP code
	@GET
	@Path("delete/{tags}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response deleteDocumentsByTagXML(@PathParam("tags") String tags) {
		Response res;
		ArrayList<Document> documents = DatabaseManager.getInstance().findDocumentsByTag(splitTags(tags));

		if(documents == null) {
			return Response.noContent().build();
		}

		res = Response.noContent().build();

		for(Document d : documents) {
			if (DatabaseManager.getInstance().removeDocument(d.getId()) != null) {
				res = Response.ok().build();
			}
		}

		return res;
	}

	// Gets a document by ID as XML
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	public Document getDocumentXML(@PathParam("id") String id) {
		try{
			Document d = DatabaseManager.getInstance().findDocument(Integer.parseInt(id));
			if (d == null) {
				throw new RuntimeException("No such Document: " + id);
			}
			return d;
		} catch (Exception e) {
			throw new RuntimeException("Server error: " + id);
		}

	}

	// Gets a document by ID as HTML
	@GET
	@Path("{id}")
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_XML)
	public String getDocumentHTML(@PathParam("id") String id, @Context HttpServletResponse servletResponse) throws IOException {

		try{
			Document d = DatabaseManager.getInstance().findDocument(Integer.parseInt(id));
			if (d == null) {
				return get204();
			}

			return documentToHTML(d);

		} catch (Exception e) {
			return get406();
		}
	}

	// Deletes a document by ID and returns an HTTP code
	@DELETE
	@Path("{id}")
	public Response deleteAccount(@PathParam("id") String id) {
		Response res;

		if (DatabaseManager.getInstance().removeDocument(Integer.parseInt(id)) == null) {
			res = Response.noContent().build();
		}
		else {
			res = Response.ok().build();
		}

		return res;
	}

	// Creates a new document from the given XML and returns an HTTP code
	@POST
	@Consumes(MediaType.APPLICATION_XML)
	public Response createDocument(JAXBElement<Document> doc) {
		Response res;
		Document document = doc.getValue();
		document.setId(DatabaseManager.getInstance().getNextIndex());

		if(DatabaseManager.getInstance().addNewDocument(document)) {
			res = Response.ok().build();
		}
		else {
			res = Response.noContent().build();
		}

		return res;
	}

	// Updates a document by ID with the given document from XML and returns an HTTP Code
	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_XML)
	public Response updateDocument(@PathParam("id") String id, JAXBElement<Document> doc) {
		Response res;
		Document updatedDocument = doc.getValue();

		try{
			Document existingDocument = DatabaseManager.getInstance().findDocument(Integer.parseInt(id));
			if(existingDocument == null) {
				res = Response.noContent().build();
			}
			else {
				/*  These values are not send from the client 
				 *  (assignment only states updating tags and links) 
				 */
				updatedDocument.setId(existingDocument.getId());
				updatedDocument.setName(existingDocument.getName());
				updatedDocument.setText(existingDocument.getText());
				updatedDocument.setScore(existingDocument.getScore());

				if(DatabaseManager.getInstance().updateDocument(updatedDocument, existingDocument)) {
					res = Response.ok().build();
				}
				else {
					res = Response.noContent().build();
				}
			}
		} catch (Exception e) {
			res = Response.notAcceptable(null).build();
		}

		return res;
	}

	// Takes a document object and returns an HTML Mark of its contents
	private String documentToHTML(Document d) {
		StringBuilder htmlBuilder = new StringBuilder();
		htmlBuilder.append("<html>");
		htmlBuilder.append("<head><title>" + d.getName() + "</title></head>");
		htmlBuilder.append("<body><h1>" + d.getName() + "</h1>");
		htmlBuilder.append("<p>" + d.getText() + "</p>");
		htmlBuilder.append("<h1> Links </h1>");
		htmlBuilder.append("<ul>");
		for (String s : d.getLinks())
		{
			htmlBuilder.append("<li>");
			htmlBuilder.append("<a href=\"" + PATH);
			htmlBuilder.append(s);
			htmlBuilder.append("\">");
			htmlBuilder.append(PATH + s);
			htmlBuilder.append("</a>");
			htmlBuilder.append("</li>");
		}
		htmlBuilder.append("</ul>");
		htmlBuilder.append("<h1> Tags </h1>");
		htmlBuilder.append("<ul>");
		for (String s : d.getTags())
		{
			htmlBuilder.append("<li>");
			htmlBuilder.append(s);
			htmlBuilder.append("</li>");
		}
		htmlBuilder.append("</ul></body>");
		htmlBuilder.append("</html>");

		return htmlBuilder.toString();
	}

	// Takes a document list and returns HTML markup for all its contents
	private String documentsToHTML(ArrayList<Document> documents) {
		StringBuilder htmlBuilder = new StringBuilder();
		htmlBuilder.append("<html>");
		htmlBuilder.append("<head><title> All Documents </title></head>");
		htmlBuilder.append("<body>");
		for(Document d : documents) {
			htmlBuilder.append("<h1>" + d.getName() + "</h1>");
			htmlBuilder.append("<p>" + d.getText() + "</p>");
			htmlBuilder.append("<h1> Links </h1>");
			htmlBuilder.append("<ul>");
			for (String s : d.getLinks())
			{
				htmlBuilder.append("<li>");
				htmlBuilder.append("<a href=\"" + PATH);
				htmlBuilder.append(s);
				htmlBuilder.append("\">");
				htmlBuilder.append(PATH + s);
				htmlBuilder.append("</a>");
				htmlBuilder.append("</li>");
			}
			htmlBuilder.append("</ul>");
			htmlBuilder.append("<h1> Tags </h1>");
			htmlBuilder.append("<ul>");
			for (String s : d.getTags())
			{
				htmlBuilder.append("<li>");
				htmlBuilder.append(s);
				htmlBuilder.append("</li>");
			}
			htmlBuilder.append("</ul>");
		}
		htmlBuilder.append("</body>");
		htmlBuilder.append("</html>");

		return htmlBuilder.toString();
	}

	// Splits a tag string by ':' and puts results into an array of strings
	private ArrayList<String> splitTags(String tags) {
		String[] tagArray = tags.split(":");
		ArrayList<String> list = new ArrayList<String>(Arrays.asList(tagArray));
		return list;
	}

	//Link not found HTML
	private String get404() {
		StringBuilder htmlBuilder = new StringBuilder();
		htmlBuilder.append("<head><title>404</title><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\">"
				+ "<script type=\"application/x-javascript\"> addEventListener(\"load\", function() { setTimeout(hideURLbar, 0); }, false); function "
				+ "hideURLbar(){ window.scrollTo(0,1); } </script><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />"
				+ "<link href='http://fonts.googleapis.com/css?family=Metal+Mania' rel='stylesheet' type='text/css'><style type=\"text/css\">body{font-family: "
				+ "'Metal Mania', cursive;}body{background:skyblue;}.wrap{width:100%;margin-top:60px;}.logo h1{font-size:140px;color:yellow;text-align:center;"
				+ "margin:40px 0 0 0;text-shadow:1px 1px 6px #555;}.logo p{color:white;font-size:15px;margin-top:1px;text-align:center;}.logo p span{color:lightgreen;}"
				+ ".sub a{color:yellow;background:#06afd8;text-decoration:none;padding:5px;font-size:12px;font-family: arial, serif;font-weight:bold;}.footer{color:white;"
				+ "position:absolute;right:10px;bottom:1px;}.footer a{color:yellow;}</style></head><body><div class=\"wrap\"><div class=\"logo\"><h1>404</h1>"
				+ "<p>Sorry document is dead - Document not found</p></div></div><div class=\"footer\">"
				+ "Design by-<a href=\"http://w3layouts.com\">W3Layouts</a></div></body>");
		return htmlBuilder.toString();
	}

	//Server error HTML
	@SuppressWarnings("unused")
	private String get500() {
		return "<html> " + "<title>" + "500" + "</title>" + "<body><h1>" + "Server Error - 500" + "</body></h1>" + "</html> ";
	}

	//Document not found HTML
	private String get204() {
		return "<html> " + "<title>" + "204" + "</title>" + "<body><h1>" + "Document not found - 204" + "</body></h1>" + "</html> ";
	}

	//Invalid Arguments HTML
	private String get406() {
		return "<html> " + "<title>" + "406" + "</title>" + "<body><h1>" + "Bad Request - 406" + "</body></h1>" + "</html> ";
	}

	//Link not found XML
	@SuppressWarnings("unused")
	private String linkNotFound() {
		return "<?xml version=\"1.0\"?>" + "<code> " + "404" + " </code>" + "<status> " + "Link not found" + " </status>";
	}

	//Server error XML
	@SuppressWarnings("unused")
	private String serverError() {
		return "<?xml version=\"1.0\"?>" + "<code> " + "500" + " </code>" + "<status> " + "Server Error" + " </status>";
	}

	//Document not found XML
	@SuppressWarnings("unused")
	private String documentNotFound() {
		return "<?xml version=\"1.0\"?>" + "<code> " + "204" + " </code>" + "<status> " + "Document not found" + " </status>";
	}

	//Invalid Arguments XML
	@SuppressWarnings("unused")
	private String badRequest() {
		return "<?xml version=\"1.0\"?>" + "<code> " + "406" + " </code>" + "<status> " + "Bad Request" + " </status>";
	}



}
