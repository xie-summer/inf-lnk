package com.ly.fn.inf.lnk.remoting;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ly.fn.inf.lnk.remoting.RemotingCallback;
import com.ly.fn.inf.lnk.remoting.RemotingClient;
import com.ly.fn.inf.lnk.remoting.RemotingServer;
import com.ly.fn.inf.lnk.remoting.ReplyFuture;
import com.ly.fn.inf.lnk.remoting.RemotingCommandTest.SimpleBean;
import com.ly.fn.inf.lnk.remoting.exception.RemotingConnectException;
import com.ly.fn.inf.lnk.remoting.exception.RemotingSendRequestException;
import com.ly.fn.inf.lnk.remoting.exception.RemotingTimeoutException;
import com.ly.fn.inf.lnk.remoting.netty.NettyClientConfigurator;
import com.ly.fn.inf.lnk.remoting.netty.NettyCommandProcessor;
import com.ly.fn.inf.lnk.remoting.netty.NettyRemotingClient;
import com.ly.fn.inf.lnk.remoting.netty.NettyRemotingServer;
import com.ly.fn.inf.lnk.remoting.netty.NettyServerConfigurator;
import com.ly.fn.inf.lnk.remoting.protocol.CommandCode;
import com.ly.fn.inf.lnk.remoting.protocol.CommandVersion;
import com.ly.fn.inf.lnk.remoting.protocol.JacksonProtocolFactory;
import com.ly.fn.inf.lnk.remoting.protocol.JacksonSerializer;
import com.ly.fn.inf.lnk.remoting.protocol.ProtocolFactory;
import com.ly.fn.inf.lnk.remoting.protocol.ProtocolFactorySelector;
import com.ly.fn.inf.lnk.remoting.protocol.RemotingCommand;

import io.netty.channel.ChannelHandlerContext;

public class RemotingServerTest {
    private static RemotingServer remotingServer;
    private static RemotingClient remotingClient;

    public static RemotingServer createRemotingServer() throws InterruptedException {
        NettyServerConfigurator config = new NettyServerConfigurator();
        final ProtocolFactory protocolFactory = new JacksonProtocolFactory();
        ProtocolFactorySelector protocolFactorySelector = new ProtocolFactorySelector() {
            public ProtocolFactory select(int serializeCode) {
                return protocolFactory;
            }
        };
        RemotingServer remotingServer = new NettyRemotingServer(protocolFactorySelector, config);
        remotingServer.registerDefaultProcessor(new NettyCommandProcessor() {
            public RemotingCommand processCommand(ChannelHandlerContext ctx, RemotingCommand request) {
                JacksonSerializer serializer = new JacksonSerializer();
                SimpleBean simpleBean = serializer.deserialize(SimpleBean.class, request.getBody());
                System.err.println("收到客户端请求 : " + serializer.deserialize(String.class, simpleBean.getAvt()));
                simpleBean.setAvt(serializer.serializeAsBytes("来之服务器的问候" + simpleBean.getName() + ", " + simpleBean.getAge() + " 很好啊"));
                protocolFactory.encode(simpleBean, request);
                return request;
            }

            @Override
            public boolean tryAcquireFailure(long timeoutMillis) {
                return false;
            }

            @Override
            public void release() {}
        }, Executors.newCachedThreadPool());
        remotingServer.start();
        System.err.println(remotingServer.getServerAddress().getAddress());
        System.err.println(remotingServer.getServerAddress().getHostName());
        System.err.println(remotingServer.getServerAddress().getHostString());
        System.err.println(remotingServer.getServerAddress().getPort());
        return remotingServer;
    }

    public static RemotingClient createRemotingClient() {
        NettyClientConfigurator config = new NettyClientConfigurator();
        final ProtocolFactory protocolFactory = new JacksonProtocolFactory();
        ProtocolFactorySelector protocolFactorySelector = new ProtocolFactorySelector() {
            public ProtocolFactory select(int serializeCode) {
                return protocolFactory;
            }
        };
        RemotingClient client = new NettyRemotingClient(protocolFactorySelector, config);
        client.start();
        return client;
    }

    @BeforeClass
    public static void setup() throws InterruptedException {
        remotingServer = createRemotingServer();
        remotingClient = createRemotingClient();
    }

    @AfterClass
    public static void destroy() {
        remotingClient.shutdown();
        remotingServer.shutdown();
    }

    @Test
    public void testInvokeSync() throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        try {
            ProtocolFactory protocolFactory = new JacksonProtocolFactory();
            JacksonSerializer serializer = new JacksonSerializer();
            RemotingCommand command = new RemotingCommand();
            command.setCode(CommandCode.SUCCESS);
            command.setVersion(CommandVersion.V1);
            SimpleBean simpleBean = new SimpleBean();
            simpleBean.setName("刘飞");
            simpleBean.setAge(30);
            simpleBean.setAvt(serializer.serializeAsBytes("你好吗-sync"));
            protocolFactory.encode(simpleBean, command);
            RemotingCommand response = remotingClient.invokeSync("localhost:8888", command, 1000 * 3);
            System.err.println("response : " + response);
            SimpleBean reply = serializer.deserialize(SimpleBean.class, response.getBody());
            System.err.println("reply command response : " + serializer.deserialize(String.class, reply.getAvt()));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    @Test
    public void testInvokeAsync() throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        try {
            final ProtocolFactory protocolFactory = new JacksonProtocolFactory();
            final JacksonSerializer serializer = new JacksonSerializer();
            RemotingCommand command = new RemotingCommand();
            command.setCode(CommandCode.SUCCESS);
            command.setVersion(CommandVersion.V1);
            SimpleBean simpleBean = new SimpleBean();
            simpleBean.setName("刘飞");
            simpleBean.setAge(30);
            simpleBean.setAvt(serializer.serializeAsBytes("你好吗-async"));
            protocolFactory.encode(simpleBean, command);
            final CountDownLatch wait = new CountDownLatch(1);
            remotingClient.invokeAsync("localhost:8888", command, 1000 * 3, new RemotingCallback() {
                @Override
                public void onComplete(ReplyFuture replyFuture) {
                    RemotingCommand response = replyFuture.getResponse();
                    System.err.println("response : " + response);
                    SimpleBean reply = serializer.deserialize(SimpleBean.class, response.getBody());
                    System.err.println("reply command response : " + serializer.deserialize(String.class, reply.getAvt()));
                    wait.countDown();
                }
            });
            wait.await(3, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @Test
    public void testInvokeOneway() throws InterruptedException, RemotingConnectException, RemotingTimeoutException, RemotingSendRequestException {
        try {
            ProtocolFactory protocolFactory = new JacksonProtocolFactory();
            JacksonSerializer serializer = new JacksonSerializer();
            RemotingCommand command = new RemotingCommand();
            command.setCode(CommandCode.SUCCESS);
            command.setVersion(CommandVersion.V1);
            SimpleBean simpleBean = new SimpleBean();
            simpleBean.setName("刘飞");
            simpleBean.setAge(30);
            simpleBean.setAvt(serializer.serializeAsBytes("你好吗-oneway"));
            protocolFactory.encode(simpleBean, command);
            remotingClient.invokeOneway("localhost:8888", command);
            Thread.sleep(2000L);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
