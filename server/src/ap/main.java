package ap;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import ap.thrift.Calculator;

import com.facebook.nifty.core.*;
import com.facebook.nifty.guice.NiftyModule;
import com.google.inject.Guice;
import com.google.inject.Stage;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class main {

    public static CalculatorHandler handler;

    public static Calculator.Processor processor;

    public static void main(String [] args) {
        new main().startGuiceServer();
    }

    public static void simple(Calculator.Processor processor) {
        try {
            TServerTransport serverTransport = new TServerSocket(9090);
            TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));

            // Use this for a multithreaded server
            // TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

            System.out.println("Starting the simple server...");
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void secure(Calculator.Processor processor) {
        try {
      /*
       * Use TSSLTransportParameters to setup the required SSL parameters. In this example
       * we are setting the keystore and the keystore password. Other things like algorithms,
       * cipher suites, client auth etc can be set. 
       */
            TSSLTransportParameters params = new TSSLTransportParameters();
            // The Keystore contains the private key
            params.setKeyStore("../../lib/java/test/.keystore", "thrift", null, null);

      /*
       * Use any of the TSSLTransportFactory to get a server transport with the appropriate
       * SSL configuration. You can use the default settings if properties are set in the command line.
       * Ex: -Djavax.net.ssl.keyStore=.keystore and -Djavax.net.ssl.keyStorePassword=thrift
       * 
       * Note: You need not explicitly call open(). The underlying server socket is bound on return
       * from the factory class. 
       */
            TServerTransport serverTransport = TSSLTransportFactory.getServerSocket(9091, 0, null, params);
            TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));

            // Use this for a multi threaded server
            // TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

            System.out.println("Starting the secure server...");
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startGuiceServer() {
        final NiftyBootstrap bootstrap = Guice.createInjector(
                Stage.PRODUCTION,
                new NiftyModule() {
                    @Override
                    protected void configureNifty() {
                        // Create the handler
                        Calculator.Iface serviceInterface = new CalculatorHandler();

                        // Create the processor
                        TProcessor processor = new Calculator.Processor<>(serviceInterface);

                        // Build the server definition
                        ThriftServerDef serverDef = new ThriftServerDefBuilder().withProcessor(processor)
                                .build();

                        // Bind the definition
                        bind().toInstance(serverDef);
                    }
                }).getInstance(NiftyBootstrap.class);

        // Start the server
        bootstrap.start();

        // Arrange to stop the server at shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                bootstrap.stop();
            }
        });
    }
}