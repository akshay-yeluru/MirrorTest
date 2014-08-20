package com.tscsoftware.warehouse.cfg;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 *
 * @author Troy Makaro
 * @author
 */
public class DOM {

    public static Element getChildElement(Node node, String searchFor) {
        return getElement(node.getChildNodes(), searchFor, true);
    } // getChildElement

    public static Element getElement(NodeList nl, String searchFor) {
        return getElement(nl,searchFor, true);
    } // getElement

    public static Element getChildElement(Node node, String searchFor, boolean throwError) {
        return getElement(node.getChildNodes(), searchFor, throwError);
    } // getChildElement
    
    public static Element getElement(NodeList nl, String searchFor, boolean throwError) {
        for (int intT = 0; intT < nl.getLength(); intT++) {
            Node n = nl.item(intT);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equalsIgnoreCase(searchFor)) {
                    return (Element) n;
                } // end if
            } // end if
        } // next
        if (throwError) {
            throw new DOMException(DOMException.NOT_FOUND_ERR,
                    "could not find element: " + searchFor);
        } else {
            return null;
        }
    } // getElement

    public static Element getFirstChildElement(Node node) {
        return getFirstElement(node.getChildNodes());
    } // getFirstChildElement

    public static Element getFirstElement(NodeList nl) {
        for (int intT = 0; intT < nl.getLength(); intT++) {
            Node n = nl.item(intT);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) n;
            } // end if
        } // next
        return null;
    } // getFirstElement

    /**
     * Recursively build a HashMap or ArrayList for xml.
     * @param parent The HashMap or ArrayList object that will receive the items.
     * @param node The HashMap or ArrayList node.
     */
    public static void parse(Object parent,Node node) {
        NodeList items = node.getChildNodes();
Debugger.print(DOM.class,"    there are "+items.getLength()+" items",Debugger.INFO);
        for (int intT=0;intT<items.getLength();intT++) {
            Node item = items.item(intT);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                String type = ((Element)item).getAttribute("type");
                Object o;
                if (type.equals("java.util.HashMap") || type.equals("java.util.ArrayList")) {
                    if (type.equals("java.util.HashMap")) {
Debugger.print(DOM.class,"    item #"+(intT+1)+" is a HashMap",Debugger.INFO);
                        o = new HashMap();
                    } else {
Debugger.print(DOM.class,"    item #"+(intT+1)+" is an ArrayList",Debugger.INFO);
                        o = new ArrayList();
                    } // end if
                    DOM.parse(o,item.getFirstChild()); // HashMap or ArrayList node
                } else if (type.equals("java.lang.String")) {
Debugger.print(DOM.class,"    item #"+(intT+1)+" is a String",Debugger.INFO);
                    o = ((Element)item).getAttribute("value");
                } else if (type.equals("com.tscsoftware.data.DisconnectedResultSet")) {
Debugger.print(DOM.class,"    item #"+(intT+1)+" is a DisconnectedResultSet",Debugger.INFO);
                    Node drsNode = item.getFirstChild();
                    DisconnectedResultSet drs = new DisconnectedResultSet();
                    drs.parseNode(drsNode);
                    o = drs;
                } else {
Debugger.print(DOM.class,"    item #"+(intT+1)+" is unknown",Debugger.INFO);
                    o = item.getFirstChild();
                } // end if
                if (parent instanceof HashMap) {
                    String key = ((Element)item).getAttribute("key");
                    ((HashMap)parent).put(key,o);
                } else {
                    ((ArrayList)parent).add(o);
                } // end if
            } // end if
        } // next
    } // parse

    /**
     * Given a certain tag, return the value for a given attribute.
     */
    public static String getAttribute(Node n, String itemName) {
        return getAttribute(n, itemName, null);
    } // getAttribute

    /**
     * Given a certain tag, return the value for a given attribute.
     *  If the tag does not exist use the default provided.
     * @param node the node that contains the attribute
     * @param itemName the attribute name
     * @param defaultText default value if the attribute does not exist
     */
    public static String getAttribute(
        Node node,
        String itemName,
        String defaultText) {
        NamedNodeMap mp = node.getAttributes();
        String result = null;
        Node n = mp.getNamedItem(itemName);
        if (n != null) {
            result = n.getNodeValue();
        }
        if (result == null) {
            return defaultText;
        } else {
            return result;
        } // end if
    } // getAttribute

	/**
	 * Given an element, return the tag's content. Returns an empty string if none
	 * found.
	 * @param element the element whose content is to be retrieved
	 * @return content of the tag
	 * 
	 * @author Jason S
	 */
	public static String getContent(Node element){
		NodeList nodelist = element.getChildNodes();
		int numNodes = nodelist.getLength();
		Node node = null;
 	
		for(int curNode = 0; curNode < numNodes; curNode++) {
			node = nodelist.item(curNode);
			if(node.getNodeName().compareTo("#text") == 0){
				return node.getNodeValue();
			}
		}
 	
		String content = "";
		return content; 
	} // getContent

    /**
     * Gets a NodeList of all the child elements of 'node' that match 'match'. Will
     * return null if no matches are found.
     * @param node The node that contains the child elements.
     * @param match The name of the child elements to match on.
     * @return A NodeList of the child elements or null if no matches are found.
     */
    public static NodeList getChildElements(Node node, String match) {
        TSCNodeList newList = null;
        NodeList nl = node.getChildNodes();
        for (int intT=0;intT<nl.getLength();intT++) {
            Node n = nl.item(intT);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equalsIgnoreCase(match)) {
                if (newList == null) {
                    newList = new TSCNodeList();
                } // end if
                newList.add(n);
            } // end if
        } // next
        return newList;
    } // getChildren

    /**
     * A concrete class that implements the NodeList interface so that I can generate
     * my own list of nodes.
     *
     * @author Troy Makaro
     * @version $Revision$
     */
    public static class TSCNodeList implements NodeList {
        private ArrayList _list = new ArrayList();

        public int getLength() {
            return _list.size();
        } // getLength

        public Node item(int index) {
            return (Node)_list.get(index);
        } // getItem

        public void add(Node n) {
            _list.add(n);
        } // add

    } // inner class

} // class
