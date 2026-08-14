package scripts

import org.apache.commons.lang.StringUtils
import org.zstack.header.identity.SuppressCredentialCheck
import org.zstack.header.rest.RestRequest
import org.zstack.rest.sdk.SdkFile
import org.zstack.rest.sdk.SdkTemplate


class GoApiTemplate implements SdkTemplate {
    private Class<?> apiMsgClazz
    private RestRequest at
    private String path
    private Class responseClass
    private String replyName
    private SdkTemplate inventoryGenerator

    GoApiTemplate(Class apiMsgClass, SdkTemplate inventoryGenerator) {
        apiMsgClazz = apiMsgClass
        this.inventoryGenerator = inventoryGenerator
        at = apiMsgClazz.getAnnotation(RestRequest.class)
        if (at.path() == "null") {
            path = at.optionalPaths()[0]
        }else {
            path = at.path()
        }
        responseClass = at.responseClass()
        replyName = StringUtils.removeStart(responseClass.simpleName, "API")
    }

    @Override
    List<SdkFile> generate() {
        return generateAction(StringUtils.removeEnd(StringUtils.removeStart(apiMsgClazz.simpleName, "API"), "Msg"))
    }

    def generateAction(String clzName) {
        def f = new SdkFile()
        def file = []
        f.subPath = "/api/"
        f.fileName = "${clzName}.go"
        f.content = """package api

import (
	"encoding/json"
	mapstruct "github.com/mitchellh/mapstructure"
	log "github.com/sirupsen/logrus"
)

const (
	${clzName}Path = "${path}"
    ${clzName}Method = "${at.method()}"
    ${clzName}SuppressCredentialCheck = ${apiMsgClazz.isAnnotationPresent(SuppressCredentialCheck.class)}
)

${inventoryGenerator.generateStruct(apiMsgClazz, clzName + "Struct")}

${StringUtils.removeEnd(inventoryGenerator.generateStruct(responseClass, clzName + "Rsp"),"}\n\n")}
	Error ErrorCode `json:"error"`
}

type ${clzName}Req struct {
	${clzName} ${clzName}Struct `json:"${at.isAction() ? StringUtils.uncapitalize(clzName) : at.parameterName()}"`
}

func (rsp *${clzName}Rsp) getErrorCode() ErrorCode {
	return rsp.Error
}

func (req *${clzName}Req) getMethod() string {
	return ${clzName}Method
}

func (req *${clzName}Req) getUri() string {
	return ${clzName}Path
}

func (req *${clzName}Req) isSuppressCredentialCheck() bool {
	return ${clzName}SuppressCredentialCheck
}

func (sdk *ZStackClient) ${clzName}(param *${clzName}Struct) ${clzName}Rsp {
	req := ${clzName}Req{${clzName}: *param}
	rsp := ${clzName}Rsp{}
	body, err := sdk.Call(&req)
	if err != nil {
		rsp.Error.Code = err
		return rsp
	}

	result := make(map[string]interface{})
	err = json.Unmarshal(body, &result)
	if err != nil {
		log.Debugf("Unmarshal response failed, reason: %s", err)
		rsp.Error.Code = err
		return rsp
	}

	err = mapstruct.Decode(result, &rsp)
	if err != nil {
		log.Debugf("decode %s failed, err: %v", result, err)
		rsp.Error.Code = err
		return rsp
	}

	return rsp
}

""";
        file.add(f)
        return file
    }
}

