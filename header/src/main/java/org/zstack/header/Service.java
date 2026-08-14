package org.zstack.header;

import org.zstack.header.message.Message;

import java.util.List;

public interface Service extends Component {
    void handleMessage(Message msg);

    String getId();

    int getSyncLevel();

    List<String> getAliasIds();
}
