package org.apache.dolphinscheduler.remote;/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.netty.channel.Channel;
import org.apache.dolphinscheduler.remote.NettyRemotingClient;
import org.apache.dolphinscheduler.remote.NettyRemotingServer;
import org.apache.dolphinscheduler.remote.command.Command;
import org.apache.dolphinscheduler.remote.command.CommandType;
import org.apache.dolphinscheduler.remote.command.Ping;
import org.apache.dolphinscheduler.remote.command.Pong;
import org.apache.dolphinscheduler.remote.config.NettyClientConfig;
import org.apache.dolphinscheduler.remote.config.NettyServerConfig;
import org.apache.dolphinscheduler.remote.processor.NettyRequestProcessor;
import org.apache.dolphinscheduler.remote.utils.Address;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  netty remote client test
 */
public class NettyRemotingClientTest {


    /**
     *  test ping
     */
    @Test
    public void testSend(){
        NettyServerConfig serverConfig = new NettyServerConfig();

        NettyRemotingServer server = new NettyRemotingServer(serverConfig);
        server.registerProcessor(CommandType.PING, new NettyRequestProcessor() {
            @Override
            public void process(Channel channel, Command command) {
                channel.writeAndFlush(Pong.create(command.getOpaque()));
            }
        });
        server.start();
        //
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong opaque = new AtomicLong(1);
        final NettyClientConfig clientConfig = new NettyClientConfig();
        NettyRemotingClient client = new NettyRemotingClient(clientConfig);
        client.registerProcessor(CommandType.PONG, new NettyRequestProcessor() {
            @Override
            public void process(Channel channel, Command command) {
                opaque.set(command.getOpaque());
                latch.countDown();
            }
        });
        Command commandPing = Ping.create();
        try {
            client.send(new Address("127.0.0.1", serverConfig.getListenPort()), commandPing);
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(opaque.get(), commandPing.getOpaque());
    }
}
