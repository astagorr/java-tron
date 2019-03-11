package org.tron.core.net;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.server.ChannelManager;
import org.tron.core.exception.P2pException;
import org.tron.core.exception.P2pException.TypeEnum;
import org.tron.core.net.message.TronMessage;
import org.tron.core.net.messagehandler.BlockMsgHandler;
import org.tron.core.net.messagehandler.ChainInventoryMsgHandler;
import org.tron.core.net.messagehandler.FetchInvDataMsgHandler;
import org.tron.core.net.messagehandler.InventoryMsgHandler;
import org.tron.core.net.messagehandler.SyncBlockChainMsgHadler;
import org.tron.core.net.messagehandler.TransactionsMsgHandler;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.peer.PeerStatusCheck;
import org.tron.core.net.service.AdvService;
import org.tron.core.net.service.SyncService;
import org.tron.protos.Protocol.ReasonCode;

@Slf4j
@Component
public class TronNetService {

  @Autowired
  private ChannelManager channelManager;

  @Autowired
  private AdvService advService;

  @Autowired
  private SyncService syncService;

  @Autowired
  private PeerStatusCheck peerStatusCheck;

  @Autowired
  private SyncBlockChainMsgHadler syncBlockChainMsgHadler;

  @Autowired
  private ChainInventoryMsgHandler chainInventoryMsgHandler;

  @Autowired
  private InventoryMsgHandler inventoryMsgHandler;


  @Autowired
  private FetchInvDataMsgHandler fetchInvDataMsgHandler;

  @Autowired
  private BlockMsgHandler blockMsgHandler;

  @Autowired
  private TransactionsMsgHandler transactionsMsgHandler;

  public void start () {
    channelManager.init();
    advService.init();
    syncService.init();
    peerStatusCheck.init();
    transactionsMsgHandler.init();
    logger.info("TronNetService start successfully.");
  }

  public void close () {
    channelManager.close();
    advService.close();
    syncService.close();
    peerStatusCheck.close();
    transactionsMsgHandler.close();
    logger.info("TronNetService closed successfully.");
  }

  public void onMessage(PeerConnection peer, TronMessage msg) {
    try {
      switch (msg.getType()) {
        case SYNC_BLOCK_CHAIN:
          syncBlockChainMsgHadler.processMessage(peer, msg);
          break;
        case BLOCK_CHAIN_INVENTORY:
          chainInventoryMsgHandler.processMessage(peer, msg);
          break;
        case INVENTORY:
          inventoryMsgHandler.processMessage(peer, msg);
          break;
        case FETCH_INV_DATA:
          fetchInvDataMsgHandler.processMessage(peer, msg);
          break;
        case BLOCK:
          blockMsgHandler.processMessage(peer, msg);
          break;
        case TRXS:
          transactionsMsgHandler.processMessage(peer, msg);
          break;
        default:
          throw new P2pException(TypeEnum.NO_SUCH_MESSAGE, "No such message: " + msg.getType());
      }
    }catch (Exception e) {
      processException(peer, msg, e);
    }
  }

  private void processException (PeerConnection peer, TronMessage msg, Exception ex) {

    ReasonCode code = null;
    boolean exceptionPrintFlag = false;
    if (ex instanceof P2pException) {
      TypeEnum type = ((P2pException) ex).getType();
      switch (type) {
        case BAD_TRX:
          code = ReasonCode.BAD_TX;
          break;
        case BAD_BLOCK:
          code = ReasonCode.BAD_BLOCK;
          exceptionPrintFlag = true;
          break;
        case NO_SUCH_MESSAGE:
        case MESSAGE_WITH_WRONG_LENGTH:
        case BAD_MESSAGE:
          code = ReasonCode.BAD_PROTOCOL;
          break;
        case SYNC_FAILED:
          code = ReasonCode.SYNC_FAIL;
          break;
        case UNLINK_BLOCK:
          code = ReasonCode.UNLINKABLE;
          break;
        case DEFAULT:
          code = ReasonCode.UNKNOWN;
          break;
      }
    } else {
      exceptionPrintFlag = true;
      code = ReasonCode.UNKNOWN;
    }

    if (exceptionPrintFlag) {
      logger.error("Message {} /n process failed from peer {}.", msg, peer.getInetAddress(), ex);
    } else {
      logger.error("Message {} /n process failed from peer {}, reason: {}.", msg, peer.getInetAddress(), ex.getMessage());
    }

    peer.disconnect(code);
  }
}