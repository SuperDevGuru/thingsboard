/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.actors.rpc;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.gen.cluster.ClusterRpcServiceGrpc;
import org.thingsboard.server.service.cluster.rpc.GrpcSession;
import org.thingsboard.server.service.cluster.rpc.GrpcSessionListener;

import java.util.UUID;

import static org.thingsboard.server.gen.cluster.ClusterAPIProtos.MessageType.CONNECT_RPC_MESSAGE;

/**
 * @author Andrew Shvayka
 */
public class RpcSessionActor extends ContextAwareActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final UUID sessionId;
    private GrpcSession session;
    private GrpcSessionListener listener;

    public RpcSessionActor(ActorSystemContext systemContext, UUID sessionId) {
        super(systemContext);
        this.sessionId = sessionId;
    }

    @Override
    protected boolean process(TbActorMsg msg) {
        //TODO Move everything here, to work with TbActorMsg
        return false;
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof ClusterAPIProtos.ClusterMessage) {
            tell((ClusterAPIProtos.ClusterMessage) msg);
        } else if (msg instanceof RpcSessionCreateRequestMsg) {
            initSession((RpcSessionCreateRequestMsg) msg);
        }
    }

    private void tell(ClusterAPIProtos.ClusterMessage msg) {
        session.sendMsg(msg);
    }

    @Override
    public void postStop() {
        log.info("Closing session -> {}", session.getRemoteServer());
        session.close();
    }

    private void initSession(RpcSessionCreateRequestMsg msg) {
        log.info("[{}] Initializing session", context().self());
        ServerAddress remoteServer = msg.getRemoteAddress();
        listener = new BasicRpcSessionListener(systemContext.getActorService(), context().parent(), context().self());
        if (msg.getRemoteAddress() == null) {
            // Server session
            session = new GrpcSession(listener);
            session.setOutputStream(msg.getResponseObserver());
            session.initInputStream();
            session.initOutputStream();
            systemContext.getRpcService().onSessionCreated(msg.getMsgUid(), session.getInputStream());
        } else {
            // Client session
            ManagedChannel channel = ManagedChannelBuilder.forAddress(remoteServer.getHost(), remoteServer.getPort()).usePlaintext().build();
            session = new GrpcSession(remoteServer, listener, channel);
            session.initInputStream();

            ClusterRpcServiceGrpc.ClusterRpcServiceStub stub = ClusterRpcServiceGrpc.newStub(channel);
            StreamObserver<ClusterAPIProtos.ClusterMessage> outputStream = stub.handleMsgs(session.getInputStream());

            session.setOutputStream(outputStream);
            session.initOutputStream();
            outputStream.onNext(toConnectMsg());
        }
    }

    public static class ActorCreator extends ContextBasedCreator<RpcSessionActor> {
        private static final long serialVersionUID = 1L;

        private final UUID sessionId;

        public ActorCreator(ActorSystemContext context, UUID sessionId) {
            super(context);
            this.sessionId = sessionId;
        }

        @Override
        public RpcSessionActor create() throws Exception {
            return new RpcSessionActor(context, sessionId);
        }
    }

    private ClusterAPIProtos.ClusterMessage toConnectMsg() {
        ServerAddress instance = systemContext.getDiscoveryService().getCurrentServer().getServerAddress();
        return ClusterAPIProtos.ClusterMessage.newBuilder().setMessageType(CONNECT_RPC_MESSAGE).setServerAddress(
                ClusterAPIProtos.ServerAddress.newBuilder().setHost(instance.getHost())
                        .setPort(instance.getPort()).build()).build();
    }
}
