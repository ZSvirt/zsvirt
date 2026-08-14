package org.zstack.core.log;

import org.w3c.dom.*;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.path.PathUtil;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

/**
 * Created by lining on 2019/7/13.
 */
public class Log4jXMLModifier {
    private String log4jXMLPath;
    private Document doc;

    private static final String TAG_NAME_IFACCUMULATEDFILESIZE = "IfAccumulatedFileSize";
    private static final String TAG_NAME_IFLASTMODIFIED = "IfLastModified";

    public Log4jXMLModifier() throws Exception{
        log4jXMLPath = PathUtil.findFileOnClassPath("log4j2.xml", true).getAbsolutePath();

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        this.doc = docBuilder.parse(log4jXMLPath);
    }

    void validateLog4jXML() {
        getLogRetentionSize(LogConstant.MN_SERVER_LOG_ROLLINGFILE);
        getLogRetentionSize(LogConstant.MN_API_LOG_ROLLINGFILE);
        getLogRetentionDays(LogConstant.MN_SERVER_LOG_ROLLINGFILE);
        getLogRetentionDays(LogConstant.MN_API_LOG_ROLLINGFILE);
    }

    void modifyLogRetentionSize(long sizeGB) throws Exception {
        assert sizeGB != 0;
        setLogRetentionSize(sizeGB);
        updateLog4jXMLFile();
    }

    void setLogRetentionSize(long sizeGB) {
        if (sizeGB >= 1) {
            long sizeMB = SizeUnit.GIGABYTE.toMegaByte(sizeGB);
            long managementServerSize = sizeMB * 8 / 10;
            long apiSize = sizeMB - managementServerSize;
            setAccumulatedFileSize(LogConstant.MN_SERVER_LOG_ROLLINGFILE, managementServerSize);
            setAccumulatedFileSize(LogConstant.MN_API_LOG_ROLLINGFILE, apiSize);
        } else {
            setAccumulatedFileSize(LogConstant.MN_SERVER_LOG_ROLLINGFILE, -1);
            setAccumulatedFileSize(LogConstant.MN_API_LOG_ROLLINGFILE, -1);
        }
    }

    void modifyLogRetentionDays(long day) throws Exception {
        assert day != 0;
        setLogRetentionDays(day);
        updateLog4jXMLFile();
    }

    void setLogRetentionDays(long day) {
        setLastModifiedAge(LogConstant.MN_SERVER_LOG_ROLLINGFILE, day);
        setLastModifiedAge(LogConstant.MN_API_LOG_ROLLINGFILE, day);
    }

    boolean isLogRetentionSizeExpected(long sizeGB) {
        String serverLogSize = getLogRetentionSize(LogConstant.MN_SERVER_LOG_ROLLINGFILE);
        String apiLogSize = getLogRetentionSize(LogConstant.MN_API_LOG_ROLLINGFILE);

        if (sizeGB < 0) {
            if (!"-1 MB".equals(serverLogSize)) {
                return false;
            }

            if (!"-1 MB".equals(apiLogSize)) {
                return false;
            }
        }

        long sizeMB = SizeUnit.GIGABYTE.toMegaByte(sizeGB);
        long managementServerSize = sizeMB * 8 / 10;
        long apiSize = sizeMB - managementServerSize;

        if (!String.format("%s MB", managementServerSize).equals(serverLogSize)) {
            return false;
        }

        if (!String.format("%s MB", apiSize).equals(apiLogSize)) {
            return false;
        }

        return true;
    }

    boolean isLogRetentionDaysExpected(long day) {
        if (!String.format("%sd", day).equals(getLogRetentionDays(LogConstant.MN_SERVER_LOG_ROLLINGFILE))) {
            return false;
        }

        if (!String.format("%sd", day).equals(getLogRetentionDays(LogConstant.MN_API_LOG_ROLLINGFILE))) {
            return false;
        }

        return true;
    }

    public String getLogRetentionSize(String rollingFileName) {
        Node rollingFileNode = getRollingFileNode(rollingFileName);
        assert rollingFileNode != null : "log4j xml file has error";
        Node ifAccumulatedFileSizeNode = findAnyChildNode(rollingFileNode, TAG_NAME_IFACCUMULATEDFILESIZE);
        if (ifAccumulatedFileSizeNode == null) {
            return "-1 MB";
        }

        NamedNodeMap attrs = ifAccumulatedFileSizeNode.getAttributes();
        Node exceedsAttr = attrs.getNamedItem("exceeds");
        assert exceedsAttr != null : "log4j xml file has error";
        return exceedsAttr.getTextContent();
    }

    public String getLogRetentionDays(String rollingFileName) {
        Node rollingFileNode = getRollingFileNode(rollingFileName);
        assert rollingFileNode != null : "log4j xml file has error";
        Node ifLastModifiedNode = findAnyChildNode(rollingFileNode, TAG_NAME_IFLASTMODIFIED);
        if (ifLastModifiedNode == null) {
            return "-1d";
        }

        NamedNodeMap attrs = ifLastModifiedNode.getAttributes();
        Node ageAttr = attrs.getNamedItem("age");
        assert ageAttr != null : "log4j xml file has error";
        return ageAttr.getTextContent();
    }

    void updateLog4jXMLFile() throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();;

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(log4jXMLPath));
        transformer.transform(source, result);
    }

    private void setAccumulatedFileSize(String rollingFileName, long sizeMB) {
        Node rollingFileNode = getRollingFileNode(rollingFileName);
        assert rollingFileNode != null;
        Node deleteNode = findAnyChildNode(rollingFileNode, "Delete");
        assert deleteNode != null;
        Node ifAnyNode = findAnyChildNode(deleteNode, "IfAny");
        assert ifAnyNode != null;
        Node ifAccumulatedFileSizeNode = findAnyChildNode(ifAnyNode, TAG_NAME_IFACCUMULATEDFILESIZE);

        // delete IfAccumulatedFileSize
        if (sizeMB < 0) {
            if (ifAccumulatedFileSizeNode == null) {
                return;
            }

            ifAnyNode.removeChild(ifAccumulatedFileSizeNode);
            return;
        }

        if (ifAccumulatedFileSizeNode == null) {
            Element node = doc.createElement(TAG_NAME_IFACCUMULATEDFILESIZE);
            node.setAttribute("exceeds", String.format("%s MB", sizeMB));
            ifAnyNode.appendChild(node);
            return;
        }

        NamedNodeMap attrs = ifAccumulatedFileSizeNode.getAttributes();
        Node exceedsAttr = attrs.getNamedItem("exceeds");
        assert exceedsAttr != null;
        exceedsAttr.setTextContent(String.format("%s MB", sizeMB));
    }

    private void setLastModifiedAge(String rollingFileName, long day) {
        Node rollingFileNode = getRollingFileNode(rollingFileName);
        assert rollingFileNode != null;
        Node deleteNode = findAnyChildNode(rollingFileNode, "Delete");
        assert deleteNode != null;
        Node ifAnyNode = findAnyChildNode(deleteNode, "IfAny");
        assert ifAnyNode != null;
        Node ifLastModifiedNode = findAnyChildNode(ifAnyNode, TAG_NAME_IFLASTMODIFIED);

        // delete IfLastModified
        if (day < 0) {
            if (ifLastModifiedNode == null) {
                return;
            }

            ifAnyNode.removeChild(ifLastModifiedNode);
            return;
        }

        if (ifLastModifiedNode == null) {
            Element node = doc.createElement(TAG_NAME_IFLASTMODIFIED);
            node.setAttribute("age", String.format("%sd", day));
            ifAnyNode.appendChild(node);
            return;
        }

        NamedNodeMap attrs = ifLastModifiedNode.getAttributes();
        Node ageAttr = attrs.getNamedItem("age");
        assert ageAttr != null;
        ageAttr.setTextContent(String.format("%sd", day));
    }

    private Node getRollingFileNode(String rollingFileName) {
        NodeList rollingFileNodes = doc.getElementsByTagName("RollingFile");
        for (int i = 0; i < rollingFileNodes.getLength(); i++) {
            Node rollingFile = rollingFileNodes.item(i);
            NamedNodeMap attr = rollingFile.getAttributes();
            Node nameAttr = attr.getNamedItem("name");
            if (rollingFileName.equals(nameAttr.getTextContent())) {
                return rollingFile;
            }
        }

        return null;
    }

    private Node findAnyChildNode(Node parentNode, String tagName) {
        if (!hasChildNode(parentNode)) {
            return null;
        }

        NodeList childNodes = parentNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i ++) {
            Node childNode = childNodes.item(i);
            if (tagName.equals(childNode.getNodeName())) {
                return childNode;
            }

            if (hasChildNode(childNode)) {
                childNode = findAnyChildNode(childNode, tagName);
                if (childNode != null) {
                    return childNode;
                }
            }
        }

        return null;
    }

    private boolean hasChildNode(Node parentNode) {
        NodeList childNodes = parentNode.getChildNodes();

        if (childNodes == null || childNodes.getLength() == 0) {
            return false;
        }

        return true;
    }
}
