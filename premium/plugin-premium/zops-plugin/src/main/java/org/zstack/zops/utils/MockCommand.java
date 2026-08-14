package org.zstack.zops.utils;

import java.util.regex.Pattern;

public class MockCommand {
    private String command;

    private Boolean exactMatch = true;

    private CommandResult result;

    public MockCommand(String command, String result ) {
        this.command = command;
        this.result = new CommandResult(result);
    }

    public MockCommand(String command, String result, Boolean exactMatch) {
        this.command = command;
        this.result = new CommandResult(result);
        this.exactMatch = exactMatch;
    }

    public MockCommand(String command, String result, Integer retCode) {
        this.command = command;
        this.result = new CommandResult(result);
        this.result.setRetCode(retCode);
    }

    public Boolean match(String command) {
        if (exactMatch) {
            return command.equals(this.command);
        } else {
            Pattern pattern = Pattern.compile(this.command);
            return  pattern.matcher(command).matches();
        }
    }

    public CommandResult getResult() {
        return result;
    }

    public void setResult(CommandResult result) {
        this.result = result;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
