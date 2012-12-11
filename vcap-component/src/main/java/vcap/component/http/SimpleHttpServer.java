/*
 *   Copyright (c) 2012 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package vcap.component.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpChunkAggregator;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a simple embeddable HTTP server for handling simple web service end-points and things like publishing
 * /varz and /healthz information.
 *
 * @author "Mike Heath <elcapo@gmail.com>"
 */
public class SimpleHttpServer implements Closeable {

	private final ServerBootstrap bootstrap;
	private final Map<String, RequestHandler> requestHandlers = Collections.synchronizedMap(new HashMap<String, RequestHandler>());

	private final NioEventLoopGroup parentGroup;
	private final NioEventLoopGroup childGroup;

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHttpServer.class);

	public SimpleHttpServer(SocketAddress localAddress) {
		parentGroup = new NioEventLoopGroup();
		childGroup = new NioEventLoopGroup();
		bootstrap = initBootstrap(localAddress, parentGroup, childGroup);
	}

	public SimpleHttpServer(SocketAddress localAddress, NioEventLoopGroup parentGroup, NioEventLoopGroup childGroup) {
		this.parentGroup = null;
		this.childGroup = null;
		bootstrap = initBootstrap(localAddress, parentGroup, childGroup);
	}

	private ServerBootstrap initBootstrap(SocketAddress localAddress, NioEventLoopGroup parentGroup, NioEventLoopGroup childGroup) {
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(parentGroup, childGroup)
				.channel(NioServerSocketChannel.class)
				.localAddress(localAddress)
				.childHandler(new SimpleHttpServerInitializer())
				.bind().awaitUninterruptibly(); // Make sure the server is bound before the constructor returns.

		return bootstrap;
	}

	@Override
	public void close() throws IOException {
		if (parentGroup == null || childGroup == null) {
			bootstrap.shutdown();
		} else {
			parentGroup.shutdown();
			childGroup.shutdown();
		}
	}

	public void addHandler(String uri, RequestHandler requestHandler) {
		requestHandlers.put(uri, requestHandler);
	}

	private class SimpleHttpServerInitializer extends ChannelInitializer<SocketChannel> {
		@Override
		public void initChannel(SocketChannel ch) throws Exception {
			final ChannelPipeline pipeline = ch.pipeline();

			pipeline.addLast("decoder", new HttpRequestDecoder());
			pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
			pipeline.addLast("encoder", new HttpResponseEncoder());
			pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

			pipeline.addLast("handler", new SimpleHttpServerHandler());
		}
	}

	private class SimpleHttpServerHandler extends ChannelInboundMessageHandlerAdapter<HttpRequest> {
		@Override
		public void messageReceived(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
			if (!request.getDecoderResult().isSuccess()) {
				sendError(ctx, HttpResponseStatus.BAD_REQUEST);
				return;
			}

			final String uri = request.getUri();
			final RequestHandler requestHandler = requestHandlers.get(uri);
			if (requestHandler != null) {
				final HttpResponse httpResponse;
				httpResponse = requestHandler.handleRequest(request);
				// Close the connection as soon as the message is sent.
				ctx.write(httpResponse).addListener(ChannelFutureListener.CLOSE);
			} else {
				sendError(ctx, HttpResponseStatus.NOT_FOUND);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			if (ctx.channel().isOpen()) {
				if (cause instanceof RequestException) {
					sendError(ctx, ((RequestException) cause).getStatus());
					return;
				} else {
					sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
				}
			}
			LOGGER.error(cause.getMessage(), cause);
		}

		private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
			HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
			response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
			response.setContent(Unpooled.copiedBuffer(
					"Failure: " + status.toString() + "\r\n",
					CharsetUtil.UTF_8));

			// Close the connection as soon as the error message is sent.
			ctx.write(response).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
