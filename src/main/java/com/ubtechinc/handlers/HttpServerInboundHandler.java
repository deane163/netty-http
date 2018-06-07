package com.ubtechinc.handlers;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpServerInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static Logger LOG = LoggerFactory.getLogger(HttpServerInboundHandler.class);

    private static HttpHeaders headers;

    @Override
    public void messageReceived(ChannelHandlerContext ctx,
                                FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }
        //去除浏览器"/favicon.ico"的干扰
        if(request.uri().equals("/favicon.ico")){
            return;
        }
        if (request.method().equals(GET) && request.method().equals(POST)) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        headers = request.headers();
        if(request.method().equals(GET)){
            final String uri = request.uri();

            //这里可以根据具体的url来拆分，获取请求带过来的参数，实现自己具体的业务操作
            LOG.info("come here messageReceived uri=" + uri);
            String text ="deane";
            sendListing(ctx, text);
        }
        if(request.method().equals(POST)){
            //根据不同的 Content_Type 处理 body 数据,获取请求带过来的参数，实现自己具体的业务操作
            dealWithContentType(ctx, request);
        }

        //sendError(ctx, UNAUTHORIZED);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }
    private static void sendListing(ChannelHandlerContext ctx, String text) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
        StringBuilder buf = new StringBuilder();
        buf.append("http请求成功！"+text);
        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    private static void sendError(ChannelHandlerContext ctx,
                                  HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                status, Unpooled.copiedBuffer("Failure: " + status.toString()
                + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 简单处理常用几种 Content-Type 的 POST 内容（可自行扩展）
     * @param //headers
     * @param //content
     * @throws Exception
     */
    private static void dealWithContentType(ChannelHandlerContext ctx,FullHttpRequest request) throws Exception{
        String contentType = getContentType();
        if(contentType.equals("application/json")){  //可以使用HttpJsonDecoder
            String jsonStr = request.content().toString(Charsets.toCharset(CharEncoding.UTF_8));
            JSONObject obj = JSON.parseObject(jsonStr);
            for(Map.Entry<String, Object> item : obj.entrySet()){
                System.out.println(item.getKey()+"="+item.getValue().toString());
            }
            sendListing(ctx, obj.toJSONString());
        }
    }

    /**
     * 获得Content-Type类型
     * @return
     */
    private static String getContentType(){
        String typeStr = headers.get("Content-Type").toString();
        String[] list = typeStr.split(";");
        return list[0];
    }

}