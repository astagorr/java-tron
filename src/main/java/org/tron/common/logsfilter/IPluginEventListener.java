package org.tron.common.logsfilter;

import org.pf4j.ExtensionPoint;

public interface IPluginEventListener extends ExtensionPoint {

    public void handleBlockEvent(Object trigger);

    public void handleTransactionTrigger(Object trigger);

    public void handleContractLogTrigger(Object trigger);

    public void handleContractEventTrigger(Object trigger);

}