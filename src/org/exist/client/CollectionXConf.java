/*
 * eXist Open Source Native XML Database
 *
 * Copyright (C) 2001-06 Wolfgang M. Meier wolfgang@exist-db.org
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */
package org.exist.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.exist.storage.DBBroker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

/**
 * Class to represent a collection.xconf which holds the configuration data for a collection
 * 
 * @author Adam Retter <adam.retter@devon.gov.uk>
 * @serial 2006-05-04
 * @version 1.1
 */
public class CollectionXConf
{	
	private String path = null;			//path of the collection.xconf file
	Collection collection = null;		//the configuration collection
	Resource resConfig = null;			//the collection.xconf resource
	
	private FullTextIndex fulltextIndex = null;		//fulltext index model
	private RangeIndex[] rangeIndexes = null;		//range indexes model
	private QNameIndex[] qnameIndexes;				//qname indexes model
	private Trigger[] triggers = null;				//triggers model
	
	private boolean hasChanged = false;	//indicates if changes have been made to the current collection configuration
	
	
	/**
	 * Constructor
	 * 
	 * @param collectionName	The path of the collection to retreive the collection.xconf for
	 * @param client	The interactive client
	 */
	CollectionXConf(String CollectionName, InteractiveClient client) throws XMLDBException
	{
		//get configuration collection for the named collection
		path = DBBroker.CONFIG_COLLECTION + CollectionName;
		collection = client.getCollection(path);
		
		if(collection == null) //if no config collection for this collection exists, just return
			return;
		
		//get the resource from the db
		resConfig = collection.getResource(DBBroker.COLLECTION_CONFIG_FILENAME);
		
		if(resConfig == null) //if, no config file exists for that collection
			return;
		
		
		//Parse the configuration file into a DOM
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document docConfig = null;
		try
		{
			DocumentBuilder builder = factory.newDocumentBuilder();
			docConfig = builder.parse( new java.io.ByteArrayInputStream(resConfig.getContent().toString().getBytes()) );
		}
		catch(ParserConfigurationException pce)
		{
			//TODO: do something here, throw xmldbexception?
		} 
		catch(SAXException se)
		{
			//TODO: do something here, throw xmldbexception?
		}
		catch(IOException ioe)
		{
			//TODO: do something here, throw xmldbexception?
		}
		
		//Get the root of the collection.xconf
		Element xconf = docConfig.getDocumentElement();
		
		//Read FullText Index from xconf
		fulltextIndex = getFullTextIndex(xconf);
		
		//Read Range Indexes from xconf
		rangeIndexes = getRangeIndexes(xconf);
		
		//Read QName Indexes from xconf
		qnameIndexes = getQNameIndexes(xconf);
		
		//read Triggers from xconf
		triggers = getTriggers(xconf);
	}
	
	/**
	 * Indicates whether the fulltext index defaults to indexing all nodes
	 *
	 * @return true indicates all nodes are indexed, false indicates no nodes are indexed by default
	 */
	public boolean getFullTextIndexDefaultAll()
	{
		return fulltextIndex != null ? fulltextIndex.getDefaultAll() : false;
	}
	
	/**
	 * Set whether all nodes should be indexed into the fulltext index
	 * 
	 * @param defaultAll	true indicates all nodes should be indexed, false indicates no nodes should be indexed by default
	 */
	public void setFullTextIndexDefaultAll(boolean defaultAll)
	{
		hasChanged = true;
		if(fulltextIndex == null)
		{
			fulltextIndex = new FullTextIndex(true, false, false, null);
		}
		else
		{
			fulltextIndex.setDefaultAll(defaultAll);
		}
	}
	
	/**
	 * Indicates whether the fulltext index indexes attributes
	 *
	 * @return true indicates attributes are indexed, false indicates attributes are not indexed
	 */
	public boolean getFullTextIndexAttributes()
	{
		return fulltextIndex != null ? fulltextIndex.getAttributes() : false;
	}
	
	/**
	 * Set whether attributes should be indexed into the fulltext index
	 * 
	 * @param attributes	true indicates attributes should be indexed, false indicates attributes should not be indexed
	 */
	public void setFullTextIndexAttributes(boolean attributes)
	{
		hasChanged = true;
		
		if(fulltextIndex == null)
		{
			fulltextIndex = new FullTextIndex(false, true, false, null);
		}
		else
		{
			fulltextIndex.setAttributes(attributes);
		}
	}
	
	/**
	 * Indicates whether the fulltext index indexes alphanumeric values
	 *
	 * @return true indicates alphanumeric values are indexed, false indicates alphanumeric values are not indexed
	 */
	public boolean getFullTextIndexAlphanum()
	{
		return fulltextIndex != null ? fulltextIndex.getAlphanum() : false;
	}
	
	/**
	 * Set whether alphanumeric values should be indexed into the fulltext index
	 * 
	 * @param alphanum	true indicates alphanumeric values should be indexed, false indicates alphanumeric values should not be indexed
	 */
	public void setFullTextIndexAlphanum(boolean alphanum)
	{
		hasChanged = true;
		
		if(fulltextIndex == null)
		{
			fulltextIndex = new FullTextIndex(false, false, true, null);
		}
		else
		{
			fulltextIndex.setAlphanum(alphanum);
		}
	}
	
	/**
	 * Returns a full text index path
	 * 
	 * @param index	The numeric index of the fulltext index path to retreive
	 * 
	 * @return The XPath
	 */
	public String getFullTextIndexPath(int index)
	{
		return fulltextIndex.getXPath(index);
	}
	
	/**
	 * Returns a full text index path action
	 * 
	 * @param index	The numeric index of the fulltext index path action to retreive
	 * 
	 * @return The Action, either "include" or "exclude"
	 */
	public String getFullTextIndexPathAction(int index)
	{
		return fulltextIndex.getAction(index);
	}
	
	/**
	 * Returns the number of full text index paths defined
	 *  
	 * @return The number of paths
	 */
	public int getFullTextPathCount()
	{
		if(fulltextIndex != null)
		{
			return fulltextIndex.getLength();
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 * Add a path to the full text index
	 *
	 * @param XPath		The XPath to index
	 * @param action	The action to take on the path, either "include" or "exclude"
	 */
	public void addFullTextIndex(String XPath, String action)
	{
		hasChanged = true;
		
		
		if(fulltextIndex == null)
		{
			fulltextIndex = new FullTextIndex(false, false, false, null);
		}
		
		fulltextIndex.addIndex(XPath, action);
	}
	
	/**
	 * Update the details of a full text index path
	 *
	 * @param index		The numeric index of the path to update
	 * @param XPath		The new XPath, or null to just set the action
	 * @param action	The new action, either "include" or "exclude", or null to just set the XPath
	 */
	public void updateFullTextIndex(int index, String XPath, String action)
	{
		hasChanged = true;
		
		if(XPath != null)
			fulltextIndex.setXPath(index, XPath);
		
		if(action != null)
			fulltextIndex.setAction(index, action);
	}

	/**
	 * Delete a path from the full text index
	 * 
	 * @param index	The numeric index of the path to delete
	 */
	public void deleteFullTextIndex(int index)
	{
		hasChanged = true;
		
		fulltextIndex.deleteIndex(index);
	}
	
	/**
	 * Returns an array of the Range Indexes
	 * 
	 * @return Array of Range Indexes
	 */
	public RangeIndex[] getRangeIndexes()
	{
		return rangeIndexes;
	}
	
	/**
	 * Returns n specific Range Index
	 * 
	 * @param index	The numeric index of the Range Index to return
	 * 
	 * @return The Range Index
	 */
	public RangeIndex getRangeIndex(int index)
	{
		return rangeIndexes[index];
	}
	
	/**
	 * Returns the number of Range Indexes defined
	 *  
	 * @return The number of Range indexes
	 */
	public int getRangeIndexCount()
	{
		if(rangeIndexes != null)
		{
			return rangeIndexes.length;
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 * Delete a Range Index
	 * 
	 * @param index	The numeric index of the Range Index to delete
	 */
	public void deleteRangeIndex(int index)
	{
		//can only remove an index which is in the array
		if(index < rangeIndexes.length)
		{
			hasChanged = true;
			
			//if its the last item in the array just null the array 
			if(rangeIndexes.length == 1)
			{
				rangeIndexes = null;
			}
			else
			{
				//else remove the item at index from the array
				RangeIndex newRangeIndexes[] = new RangeIndex[rangeIndexes.length - 1];
				int x = 0;
				for(int i = 0; i < rangeIndexes.length; i++)
				{
					if(i != index)
					{
						newRangeIndexes[x] = rangeIndexes[i];
						x++;
					}
				}	
				rangeIndexes = newRangeIndexes;
			}
		}
	}
	
	/**
	 * Update the details of a Range Index
	 *
	 * @param index		The numeric index of the range index to update
	 * @param XPath		The new XPath, or null to just set the type
	 * @param xsType	The new type of the path, a valid xs:type, or just null to set the path
	 */
	public void updateRangeIndex(int index, String XPath, String xsType)
	{
		hasChanged = true;
		
		if(XPath != null)
			rangeIndexes[index].setXPath(XPath);
		
		if(xsType != null)
			rangeIndexes[index].setxsType(xsType);
	}
	
	/**
	 * Add a Range Index
	 *
	 * @param XPath		The XPath to index
	 * @param xsType	The type of the path, a valid xs:type
	 */
	public void addRangeIndex(String XPath, String xsType)
	{
		hasChanged = true;
		
		if(rangeIndexes == null)
		{
			rangeIndexes = new RangeIndex[1];
			rangeIndexes[0] = new RangeIndex(XPath, xsType);
		}
		else
		{
			RangeIndex newRangeIndexes[] = new RangeIndex[rangeIndexes.length + 1];
			System.arraycopy(rangeIndexes, 0, newRangeIndexes, 0, rangeIndexes.length);
			newRangeIndexes[rangeIndexes.length] = new RangeIndex(XPath, xsType);
			rangeIndexes = newRangeIndexes;
		}
	}
	
	/**
	 * Returns an array of the QName Indexes
	 * 
	 * @return Array of QName Indexes
	 */
	public QNameIndex[] getQNameIndexes()
	{
		return qnameIndexes;
	}
	
	/**
	 * Returns a specific QName Index
	 * 
	 * @param index	The numeric index of the QName index to return
	 * 
	 * @return The QName Index
	 */
	public QNameIndex getQNameIndex(int index)
	{
		return qnameIndexes[index];
	}
	
	/**
	 * Returns the number of QName Indexes defined
	 *  
	 * @return The number of QName indexes
	 */
	public int getQNameIndexCount()
	{
		if(qnameIndexes != null)
		{
			return qnameIndexes.length;
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 * Delete a QName Index
	 * 
	 * @param index	The numeric index of the QName Index to delete
	 */
	public void deleteQNameIndex(int index)
	{
		//can only remove an index which is in the array
		if(index < qnameIndexes.length)
		{
			hasChanged = true;
			
			//if its the last item in the array just null the array 
			if(qnameIndexes.length == 1)
			{
				qnameIndexes = null;
			}
			else
			{
				QNameIndex newQNameIndexes[] = new QNameIndex[qnameIndexes.length - 1];
				int x = 0;
				for(int i = 0; i < qnameIndexes.length; i++)
				{
					if(i != index)
					{
						newQNameIndexes[x] = qnameIndexes[i];
						x++;
					}
				}
				
				qnameIndexes = newQNameIndexes;
			}
		}
	}
	
	/**
	 * Update the details of a QName Index
	 *
	 * @param index		The numeric index of the qname index to update
	 * @param QName		The new QName, or null to just set the type
	 * @param xsType	The new type of the path, a valid xs:type, or just null to set the QName
	 */
	public void updateQNameIndex(int index, String QName, String xsType)
	{
		hasChanged = true;
		
		if(QName != null)
			qnameIndexes[index].setQName(QName);
		
		if(xsType != null)
			qnameIndexes[index].setxsType(xsType);
	}
	
	/**
	 * Add a QName Index
	 *
	 * @param QName		The QName to index
	 * @param xsType	The type of the QName, a valid xs:type
	 */
	public void addQNameIndex(String QName, String xsType)
	{
		hasChanged = true;
		
		if(qnameIndexes == null)
		{
			qnameIndexes = new QNameIndex[1];
			qnameIndexes[0] = new QNameIndex(QName, xsType);
		}
		else
		{
			QNameIndex newQNameIndexes[] = new QNameIndex[qnameIndexes.length + 1];
			System.arraycopy(qnameIndexes, 0, newQNameIndexes, 0, qnameIndexes.length);
			newQNameIndexes[qnameIndexes.length] = new QNameIndex(QName, xsType);
			qnameIndexes = newQNameIndexes;
		}
	}
	
	//given the root element of collection.xconf it will return the fulltext index
	private FullTextIndex getFullTextIndex(Element xconf)
	{
		NodeList nlFullTextIndex = xconf.getElementsByTagName("fulltext");
		if(nlFullTextIndex.getLength() > 0)
		{
			boolean defaultAll = true;
			boolean attributes = false;
			boolean alphanum = false;
			FullTextIndexPath[] paths = null;
			
			Element elemFullTextIndex = (Element)nlFullTextIndex.item(0);
			defaultAll = elemFullTextIndex.getAttribute("default").equals("all");
			attributes = elemFullTextIndex.getAttribute("attributes").equals("true");
			alphanum = elemFullTextIndex.getAttribute("alphanum").equals("true");
			
			NodeList nlInclude = elemFullTextIndex.getElementsByTagName("include");
			NodeList nlExclude = elemFullTextIndex.getElementsByTagName("exclude");
			
			int iPaths = nlInclude.getLength() + nlExclude.getLength();
			
			if(iPaths > 0 )
			{
				paths = new FullTextIndexPath[iPaths];
			
				if(nlInclude.getLength() > 0)
				{
					for(int i = 0; i < nlInclude.getLength(); i++)
					{
						paths[i] = new FullTextIndexPath(((Element)nlInclude.item(i)).getAttribute("path"), FullTextIndexPath.ACTION_INCLUDE);
					}
				}
				
				if(nlExclude.getLength() > 0)
				{	
					for(int i = 0; i < nlExclude.getLength(); i++)
					{
						paths[i] = new FullTextIndexPath(((Element)nlExclude.item(i)).getAttribute("path"), FullTextIndexPath.ACTION_EXCLUDE);
					}
				}
			}
			return new FullTextIndex(defaultAll, attributes, alphanum, paths);
			
		}
		return null;
		
	}
	
	//given the root element of collection.xconf it will return an array of range indexes
	private RangeIndex[] getRangeIndexes(Element xconf)
	{
		ArrayList alRangeIndexes = new ArrayList();
		
		NodeList nlRangeIndexes = xconf.getElementsByTagName("create");
		if(nlRangeIndexes.getLength() > 0)
		{
			for(int i = 0; i < nlRangeIndexes.getLength(); i++)
			{	
				Element rangeIndex = (Element)nlRangeIndexes.item(i);
				//is it a range index or a qname index
				if(rangeIndex.getAttribute("path").length() > 0)
				{
					alRangeIndexes.add(new RangeIndex(rangeIndex.getAttribute("path"), rangeIndex.getAttribute("type")));
				}
			}
			
			RangeIndex[] rangeIndexes = new RangeIndex[alRangeIndexes.size()];
			for(int i=0; i < alRangeIndexes.size(); i++)
			{
				rangeIndexes[i] = (RangeIndex)alRangeIndexes.get(i);
			}
			return rangeIndexes;
		}
		return null;
	}

	//given the root element of collection.xconf it will return an array of qname indexes
	private QNameIndex[] getQNameIndexes(Element xconf)
	{		
		ArrayList alQNameIndexes = new ArrayList();
		
		NodeList nlQNameIndexes = xconf.getElementsByTagName("create");
		if(nlQNameIndexes.getLength() > 0)
		{ 
			for(int i = 0; i < nlQNameIndexes.getLength(); i++)
			{	
				Element qnameIndex = (Element)nlQNameIndexes.item(i);
				//is it a range index or a qname index
				if(qnameIndex.getAttribute("qname").length() > 0)
				{
					alQNameIndexes.add(new QNameIndex(qnameIndex.getAttribute("qname"), qnameIndex.getAttribute("type")));
				}
			}
			
			QNameIndex[] qnameIndexes = new QNameIndex[alQNameIndexes.size()];
			for(int i=0; i < alQNameIndexes.size(); i++)
			{
				qnameIndexes[i] = (QNameIndex)alQNameIndexes.get(i);
			}
			return qnameIndexes;
		}
		return null;
	}
	
	//given the root element of collection.xconf it will return an array of triggers
	private Trigger[] getTriggers(Element xconf)
	{
		NodeList nlTriggers = xconf.getElementsByTagName("trigger");
		if(nlTriggers.getLength() > 0)
		{
			Trigger[] triggers = new Trigger[nlTriggers.getLength()]; 
			
			for(int i = 0; i < nlTriggers.getLength(); i++)
			{	
				Element trigger = (Element)nlTriggers.item(i);
				
				Properties parameters = new Properties();
				NodeList nlTriggerParameters = trigger.getElementsByTagName("parameter");
				if(nlTriggerParameters.getLength() > 0)
				{
					for(int x = 0; x < nlTriggerParameters.getLength(); x++)
					{
						Element parameter = (Element)nlTriggerParameters.item(x);
						parameters.setProperty(parameter.getAttribute("name"), parameter.getAttribute("value"));
					}
				}
				
				//create the trigger
				triggers[i] = new Trigger(trigger.getAttribute("event"), trigger.getAttribute("class"), parameters);
			}
			
			return triggers;
		}
		return null;
	}
	
	//has the collection.xconf been modified?
	/**
	 * Indicates whether the collection configuration has changed
	 * 
	 * @return true if the configuration has changed, false otherwise
	 */
	public boolean hasChanged()
	{
		return hasChanged;
	}
	
	//produces a string of XML describing the collection.xconf
	private String toXMLString()
	{
		StringBuffer xconf = new StringBuffer();
		
		xconf.append("<collection xmlns=\"http://exist-db.org/collection-config/1.0\">");
		xconf.append(System.getProperty("line.separator"));
		xconf.append('\t');
		xconf.append("<index>");
		xconf.append(System.getProperty("line.separator"));
	
		//fulltext indexes
		xconf.append("\t\t");
		xconf.append(fulltextIndex.toXMLString());
		xconf.append(System.getProperty("line.separator"));
		
		//range indexes
		for(int r = 0; r < rangeIndexes.length; r ++)
		{
			xconf.append("\t\t\t");
			xconf.append(rangeIndexes[r].toXMLString());
			xconf.append(System.getProperty("line.separator"));
		}
		
		//qname indexes
		for(int q = 0; q < qnameIndexes.length; q ++)
		{
			xconf.append("\t\t\t");
			xconf.append(rangeIndexes[q].toXMLString());
			xconf.append(System.getProperty("line.separator"));
		}
		
		xconf.append('\t');
		xconf.append("</index>");
		xconf.append(System.getProperty("line.separator"));
		xconf.append("</collection>");
		
		return xconf.toString();
	}
	
	/**
	 * Saves the collection configuation back to the collection.xconf
	 * 
	 * @return true if the save succeeds, false otherwise
	 */
	public boolean Save()
	{
		try
		{
			//set the content of the collection.xconf
			resConfig.setContent(toXMLString());
			
			//store the collection.xconf
			collection.storeResource(resConfig);
		}
		catch(XMLDBException xmldbe)
		{
			return false;
		}
		
		return true;
	}
	
	//represents a path in the fulltext index in the collection.xconf
	protected class FullTextIndexPath
	{
		public final static String ACTION_INCLUDE = "include";
		public final static String ACTION_EXCLUDE = "exclude";
		
		private String xpath = null;
		private String action = null;
		
		FullTextIndexPath(String xpath, String action)
		{
			this.xpath = xpath;
			this.action = action;
		}
		
		public String getXPath()
		{
			return xpath;
		}
		
		public String getAction()
		{
			return action;
		}
		
		public void setXPath(String xpath)
		{
			this.xpath = xpath;
		}
		
		public void setAction(String action)
		{
			this.action = action;
		}
		
	}
	
	//represents the fulltext index in the collection.xconf
	protected class FullTextIndex
	{	
		boolean defaultAll = true;
		boolean attributes = false;
		boolean alphanum = false;
		FullTextIndexPath[] xpaths = null;
	
		
		/**
		 * Constructor
		 * 
		 * @param defaultAll	Should the fulltext index default to indexing all nodes
		 * @param attributes	Should attributes be indexed into the fulltext index 
		 * @param alphanum		Should alphanumeric values be indexed into the fulltext index
		 * @param xpaths		Explicit fulltext index paths to include or exclude, null if there are no explicit paths		
		 */
		FullTextIndex(boolean defaultAll, boolean attributes, boolean alphanum, FullTextIndexPath[] xpaths)
		{
			this.defaultAll = defaultAll;
			this.attributes = attributes;
			this.alphanum = alphanum;
			
			this.xpaths = xpaths;
		}
		
		public boolean getDefaultAll()
		{
			return defaultAll;
		}
		
		public void setDefaultAll(boolean defaultAll)
		{
			this.defaultAll = defaultAll;
		}
		
		public boolean getAttributes()
		{
			return attributes;
		}
		
		public void setAttributes(boolean attributes)
		{
			this.attributes = attributes;
		}
		
		public boolean getAlphanum()
		{
			return alphanum;
		}
		
		public void setAlphanum(boolean alphanum)
		{
			this.alphanum = alphanum;
		}
		
		public String getXPath(int index)
		{
			return xpaths[index].getXPath();
		}
		
		public String getAction(int index)
		{
			return xpaths[index].getAction();
		}
		
		public int getLength()
		{
			return xpaths != null ? xpaths.length : 0;
		}
		
		public void setXPath(int index, String XPath)
		{
			xpaths[index].setXPath(XPath);
		}
		
		public void setAction(int index, String action)
		{
			xpaths[index].setAction(action);
		}
		
		public void addIndex(String XPath, String action)
		{
			if(xpaths == null)
			{
				xpaths = new FullTextIndexPath[1];
				xpaths[0] = new FullTextIndexPath(XPath, action);
			}
			else
			{
				FullTextIndexPath newxpaths[] = new FullTextIndexPath[xpaths.length + 1];
				System.arraycopy(xpaths, 0, newxpaths, 0, xpaths.length);
				newxpaths[xpaths.length] = new FullTextIndexPath(XPath, action);
				xpaths = newxpaths;
			}
		}
		
		public void deleteIndex(int index)
		{
			//can only remove an index which is in the array
			if(index < xpaths.length)
			{
				//if its the last item in the array just null the array 
				if(xpaths.length == 1)
				{
					xpaths = null;
				}
				else
				{
					//else remove the item at index from the array
					FullTextIndexPath newxpaths[] = new FullTextIndexPath[xpaths.length - 1];
					int x = 0;
					for(int i = 0; i < xpaths.length; i++)
					{
						if(i != index)
						{
							newxpaths[x] = xpaths[i];
							x++;
						}
					}	
					xpaths = newxpaths;
				}
			}
		}
		
		//produces a collection.xconf suitable string of XML describing the fulltext index
		protected String toXMLString()
		{
			StringBuffer fulltext = new StringBuffer();
			
			fulltext.append("<fulltext default=\"");
			fulltext.append(defaultAll ? "all" : "none");
			fulltext.append("\" attributes=\"");
			fulltext.append(attributes);
			fulltext.append("\" alphanum=\"");
			fulltext.append(alphanum);
			fulltext.append("\">");
			
			fulltext.append(System.getProperty("line.separator"));
			
			
			for(int i = 0; i < xpaths.length; i++)
			{
				fulltext.append('\t');
				
				fulltext.append("<");
				fulltext.append(xpaths[i].getAction());
				fulltext.append(" path=\"");
				fulltext.append(xpaths[i].getXPath());
				fulltext.append("\"/>");
				
				fulltext.append(System.getProperty("line.separator"));
			}
			
			fulltext.append("</fulltext>");
			
			
			return fulltext.toString();
		}
	}
	
	//represents a range index in the collection.xconf
	protected class RangeIndex
	{
		private String XPath = null;
		private String xsType = null;
		
		RangeIndex(String XPath, String xsType)
		{
			this.XPath = XPath;
			this.xsType = xsType;
		}
		
		public String getXPath()
		{
			return(XPath);	
		}
		
		public String getxsType()
		{
			return(xsType);
		}
		
		public void setXPath(String XPath)
		{
			this.XPath = XPath;
		}
		
		public void setxsType(String xsType)
		{
			this.xsType = xsType;
		}
		
		//produces a collection.xconf suitable string of XML describing the range index
		protected String toXMLString()
		{
			StringBuffer range = new StringBuffer();
			
			range.append("<create path=\"");
			range.append(XPath);
			range.append("\" type=\"");
			range.append(xsType);
			range.append("\"/>");
			
			return range.toString();
		}
	}
	
	//represents a qname index in the collection.xconf
	protected class QNameIndex
	{
		private String QName = null;
		private String xsType = null;
		
		QNameIndex(String QName, String xsType)
		{
			this.QName = QName;
			this.xsType = xsType;
		}
		
		public String getQName()
		{
			return(QName);	
		}
		
		public String getxsType()
		{
			return(xsType);
		}
		
		public void setQName(String QName)
		{
			this.QName = QName;	
		}
		
		public void setxsType(String xsType)
		{
			this.xsType = xsType;
		}
		
		//produces a collection.xconf suitable string of XML describing the qname index
		protected String toXMLString()
		{
			StringBuffer qname = new StringBuffer();
			
			qname.append("<create qname=\"");
			qname.append(QName);
			qname.append("\" type=\"");
			qname.append(xsType);
			qname.append("\"/>");
			
			return qname.toString();
		}
	}
	
	//represents a Trigger in the collection.xconf
	protected class Trigger
	{
		/*public final static int EVENT_STORE_DOCUMENT = 1;
		public final static int EVENT_UPDATE_DOCUMENT = 2;
		public final static int EVENT_REMOVE_DOCUMENT = 3;
		public final static int EVENT_RENAME_COLLECTION = 4;
		public final static int EVENT_CREATE_COLLECTION = 5;
		
		private int triggerEvent = -1;*/
		private String triggerEvent = null;
		private String triggerClass = null;
		Properties parameters = null;
		
		Trigger(String triggerEvent, String triggerClass, Properties parameters)
		{
			this.triggerEvent = triggerEvent;
			this.triggerClass = triggerClass;
			this.parameters = parameters;
		}
		
	}
}
