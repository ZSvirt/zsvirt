package org.zstack.core.errorcode;

import org.zstack.core.errorcode.schema.Error;
import org.zstack.header.core.I18nMessage;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.opaque.OpaqueScripts;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.opaque.OpaqueConstants.*;

/**
 */
public class ErrorFacadeImpl implements ErrorFacade {
    private static final CLogger logger = Utils.getLogger(ErrorFacadeImpl.class);
    private Map<String, ErrorCodeInfo> codes = new HashMap<>();
    private boolean dumpOnError = Boolean.parseBoolean(System.getProperty("ErrorFacade.dumpOnError"));

    @Override
    public List<ErrorCode> getAllErrorCodes() {
        List<ErrorCode> ret = new ArrayList<>(codes.size());
        for (ErrorCodeInfo info : codes.values()) {
            ret.add(info.code.copy());
        }
        ret.sort(Comparator.comparing(ErrorCode::getCode));
        return ret;
    }

    @Override
    public ErrorCode instantiateErrorCode(Enum code, ErrorCode cause) {
        return instantiateErrorCode(code.toString(), cause);
    }

    @Override
    public ErrorCode instantiateErrorCode(String code, ErrorCode cause) {
        return instantiateErrorCode(code, cause.getDetails(), cause);
    }

    @Override
    public ErrorCode instantiateErrorCode(Enum code, String details, ErrorCode cause) {
        return instantiateErrorCode(code.toString(), details, cause);
    }

    @Override
    public ErrorCode instantiateErrorCode(String code, String details, ErrorCode cause) {
        ErrorCode err = instantiateErrorCode(code, details);
        if (cause != null) {
            err.setCause(cause);
        }
        return err;
    }

    private void replaceSystemError(ErrorCode err, String details) {
        try {
            ErrorCode subErr = JSONObjectUtil.toObject(details.substring(details.indexOf("{\"code\":")), ErrorCode.class);
            err.setCode(subErr.getCode());
            err.setElaboration(subErr.getElaboration());
            err.setMessages(subErr.getMessages());
            err.setDescription(subErr.getDescription());
            err.setDetails(subErr.getDetails());
            err.setCause(subErr.getCause());
        } catch (Exception e) {
            logger.warn(String.format("%s cannot be cast to ErrorCode type", details));
        }
    }

    private ErrorCode doInstantiateErrorCode(String code, String details, List<ErrorCode> causes) {
        ErrorCodeInfo info = codes.get(code);
        if (info == null) {
            throw new CloudRuntimeException(String.format("cannot find error code[%s]", code));
        }

        if (details != null && details.length() > 4096) {
            details = details.substring(0, 4093) + "...";
        }
        ErrorCode err = info.code.copy();
        if (SysErrors.INTERNAL.toString().equals(code)) {
            if (details != null && details.trim().contains(", {\"code\":") && details.trim().endsWith("}")) {
                replaceSystemError(err, details);
            } else {
                err.setDetails(details);
            }
        } else {
            err.setDetails(details);
        }

        err.setCauses(causes);

        if (dumpOnError) {
            DebugUtils.dumpStackTrace(String.format("An error code%s is instantiated," +
                    " for tracing the place error happened, dump stack as below", err));
        }

        return err;
    }

    @Override
    public ErrorCode instantiateErrorCode(Enum<?> code, String fmt, Object... args) {
        return instantiateErrorCode(code.toString(), fmt, args);
    }

    @Override
    public ErrorCode instantiateErrorCode(String code, String fmt, Object... args) {
        String details;
        if (fmt == null) {
            details = "";
        } else if (args == null || args.length == 0) {
            details = fmt;
        } else {
            Object[] formatArgs = args.clone();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof I18nMessage) {
                    formatArgs[i] = ((I18nMessage) args[i]).getDetails();
                }
            }

            try {
                details = String.format(fmt, formatArgs);
            } catch (Exception e) {
                logger.warn("exception happened when format error message");
                logger.warn(e.getMessage());
                details = fmt;
            }
        }
        ErrorCode errorCode = doInstantiateErrorCode(code, details, null)
                .withOpaque(OPAQUE_KEY_TEMPLATE, fmt);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                errorCode.withOpaque(opaqueKeyForArg(i), args[i]);
            }
        }
        return errorCode;
    }

    @Override
    public ErrorCode stringToInternalError(String details) {
        return instantiateErrorCode(SysErrors.INTERNAL.toString(), details);
    }

    @Override
    public ErrorCode throwableToInternalError(Throwable t) {
        return throwableToError(t, SysErrors.INTERNAL);
    }

    @Override
    public ErrorCode stringToTimeoutError(String details) {
        return instantiateErrorCode(SysErrors.TIMEOUT.toString(), details);
    }

    @Override
    public ErrorCode throwableToTimeoutError(Throwable t) {
        return throwableToError(t, SysErrors.TIMEOUT);
    }

    @Override
    public ErrorCode stringToOperationError(String details) {
        return instantiateErrorCode(SysErrors.OPERATION_ERROR, details);
    }

    @Override
    public ErrorCode stringToOperationError(String details, ErrorCode cause) {
        return instantiateErrorCode(SysErrors.OPERATION_ERROR, details, cause);
    }

    @Override
    public ErrorCode instantiateErrorCode(Enum code, List<ErrorCode> causes) {
        return instantiateErrorCode(code.toString(), causes);
    }

    @Override
    public ErrorCode instantiateErrorCode(String code, List<ErrorCode> causes) {
        return instantiateErrorCode(code, null, causes);
    }

    @Override
    public ErrorCode instantiateErrorCode(Enum code, String details, List<ErrorCode> causes) {
        return instantiateErrorCode(code.toString(), details, causes);
    }

    @Override
    public ErrorCode instantiateErrorCode(String code, String details, List<ErrorCode> causes) {
        return doInstantiateErrorCode(code, details, causes);
    }

    @Override
    public ErrorCode stringToOperationError(String details, List<ErrorCode> causes) {
        return instantiateErrorCode(SysErrors.OPERATION_ERROR, details, causes);
    }

    @Override
    public ErrorCode throwableToOperationError(Throwable t) {
        return throwableToError(t, SysErrors.OPERATION_ERROR);
    }

    @Override
    public ErrorCode stringToInvalidArgumentError(String details) {
        return instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR, details);
    }

    @Override
    public ErrorCode throwableToInvalidArgumentError(Throwable t) {
        return throwableToError(t, SysErrors.INVALID_ARGUMENT_ERROR);
    }

    private ErrorCode throwableToError(Throwable t, Enum<?> errCode) {
        ErrorCode error = instantiateErrorCode(errCode, t.getMessage());
        if (t instanceof OpaqueScripts) {
            error.withOpaque((OpaqueScripts) t);
        }
        return error;
    }

    private class ErrorCodeInfo {
        ErrorCode code;
        String path;
    }

    private void createErrorCode(org.zstack.core.errorcode.schema.Error error, String path) {
        for (Error.Code code : error.getCode()) {
            String codeId = String.format("%s.%s", error.getPrefix(), code.getId());
            ErrorCodeInfo info = codes.get(codeId);
            if (info != null) {
                throw new CloudRuntimeException(String.format("duplicate definition of ErrorCode[%s]," +
                        " file[%s] and file[%s] both define it", codeId, info.path, path));
            }

            ErrorCode errorCode = new ErrorCode();
            errorCode.setCode(codeId);
            errorCode.setDescription(code.getDescription());
            errorCode.setElaboration(code.getElaboration());
            info = new ErrorCodeInfo();
            info.code = errorCode;
            info.path = path;
            codes.put(codeId, info);
        }
    }

    public void init() {
        try {
            JAXBContext context = JAXBContext.newInstance("org.zstack.core.errorcode.schema");
            List<String> paths = PathUtil.scanFolderOnClassPath("errorCodes");
            for (String p : paths) {
                if (!p.endsWith(".xml")) {
                    logger.warn(String.format("ignore %s which is not ending with .xml", p));
                    continue;
                }

                File cfg = new File(p);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                org.zstack.core.errorcode.schema.Error error =
                        (org.zstack.core.errorcode.schema.Error) unmarshaller.unmarshal(cfg);
                createErrorCode(error, p);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
