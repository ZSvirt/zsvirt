package org.zstack.configuration;

import org.apache.commons.lang.StringEscapeUtils;
import org.zstack.configuration.testlink.schema.RequirementCategory;
import org.zstack.configuration.testlink.schema.RequirementSpecification;
import org.zstack.core.rest.RESTApiJsonTemplateGenerator;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class TestLinkDocumentGenerator {
    private static CLogger logger = Utils.getLogger(TestLinkDocumentGenerator.class);
    private static String CONFIG_FOLDER = "testLinkTemplates";


    private static String newLine(String str) {
        return String.format("\n%s", str);
    }
    
    private static String multiHtmlP(String str) {
        if (str == null) {
            return htmlP(str);
        }
        
        /*
        String[] lines = str.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(htmlP(l));
        }
        return sb.toString();
        */
        return StringEscapeUtils.escapeHtml(str);
    }

    private static String htmlSTRONG(String str) {
        return String.format("<strong>%s</strong>", str);
    }

    private static String htmlP(String str) {
        return String.format("<p>%s</p>", str);
    }

    private static String makeRequirementDescription(RequirementCategory.Req req) throws ClassNotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append(newLine(htmlP(htmlSTRONG("Prerequisite:"))));
        sb.append(newLine(htmlP(req.getPrerequisite())));
        sb.append(newLine(htmlP(htmlSTRONG("Goal:"))));
        sb.append(newLine(htmlP(req.getGoal())));
        sb.append(newLine(htmlP(htmlSTRONG("Details:"))));
        sb.append(newLine(multiHtmlP(req.getDetails())));
        sb.append(newLine(htmlP(htmlSTRONG("SubResource:"))));
        if (req.getSubResource().isEmpty()) {
            sb.append(newLine(htmlP(null)));
        } else {
            for (String sr : req.getSubResource()) {
                sb.append(newLine(htmlP(sr)));
            }
        }

        sb.append(newLine(htmlP(htmlSTRONG("RelationalResource:"))));
        if (req.getRelationalResource().isEmpty()) {
            sb.append(newLine(htmlP(null)));
        } else {
            for (String sr : req.getRelationalResource()) {
                sb.append(newLine(htmlP(sr)));
            }
        }

        sb.append(newLine(htmlP(htmlSTRONG("API:"))));
        if (req.getApi().isEmpty()) {
            sb.append(newLine(htmlP(null)));
        } else {
            for (RequirementCategory.Req.Api api : req.getApi()) {
                Class<?> rc = Class.forName(api.getRequestClass());
                String rs = RESTApiJsonTemplateGenerator.dump(rc);
                sb.append(newLine(multiHtmlP(rs)));
                sb.append(newLine(htmlP("")));
                rc = Class.forName(api.getResponseClass());
                rs = RESTApiJsonTemplateGenerator.dump(rc);
                sb.append(newLine(multiHtmlP(rs)));
                sb.append(newLine(htmlP("")));
                sb.append(newLine(htmlP("")));
            }
        }
        
        sb.append(newLine(htmlP(htmlSTRONG("Other:"))));
        sb.append(newLine(htmlP(req.getOther())));
        return sb.toString();
    }

    private static void requirmentSpecFromXml(String outputDir, String filePath) throws JAXBException, ClassNotFoundException, IOException {
        JAXBContext context = JAXBContext.newInstance("org.zstack.configuration.testlink.schema");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        File tfile = new File(filePath);
        RequirementCategory sc = (RequirementCategory) unmarshaller.unmarshal(tfile);

        String docId = sc.getDocumentId();
        String specTitle = sc.getTitle();
        if (specTitle == null) {
            specTitle = docId;
        }
        RequirementSpecification ret = new RequirementSpecification();
        RequirementSpecification.ReqSpec rspec = new RequirementSpecification.ReqSpec();
        rspec.setDocId(docId);
        rspec.setTitle(specTitle);

        for (RequirementCategory.Req req : sc.getReq()) {
            RequirementSpecification.ReqSpec.Requirement r = new RequirementSpecification.ReqSpec.Requirement();
            r.setDocid(String.format("%s-REQ-%s", docId.toUpperCase(), sc.getReq().indexOf(req)));
            r.setTitle(req.getTitle());
            r.setDescription(makeRequirementDescription(req));
            rspec.getRequirement().add(r);
        }
        ret.setReqSpec(rspec);

        File retFile = new File(PathUtil.join(outputDir, String.format("reqSpec-%s", tfile.getName())));
        if (retFile.createNewFile()) {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(ret, retFile);
        }
    }

    public static void generateRequirementSpec(String outputDir) {
        URL folderUrl = TestLinkDocumentGenerator.class.getClassLoader().getResource(CONFIG_FOLDER);
        if (folderUrl == null || !folderUrl.getProtocol().equals("file")) {
            throw new CloudRuntimeException(
                    String.format(
                            "The folder %s is not found in classpath or there is another resource has the same name.",
                            CONFIG_FOLDER));
        }

        try {
            String[] files = new File(folderUrl.toURI()).list();
            for (String f : files) {
                if (!f.endsWith(".xml")) {
                    logger.warn(String.format("skip file which is not ended with .xml", f));
                    continue;
                }
                URL fileUrl = TestLinkDocumentGenerator.class.getClassLoader().getResource(Utils.getPathUtil().join(CONFIG_FOLDER, f));
                requirmentSpecFromXml(outputDir, fileUrl.getPath());
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
